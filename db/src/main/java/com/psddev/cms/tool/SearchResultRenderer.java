package com.psddev.cms.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.psddev.dari.db.PredicateParser;
import org.joda.time.DateTime;

import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Renderer;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.Taxon;
import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Metric;
import com.psddev.dari.db.MetricInterval;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PaginatedResult;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;

public class SearchResultRenderer {

    public static final String TAXON_LEVEL_PARAMETER = "taxonLevel";

    private static final String ATTRIBUTE_PREFIX = SearchResultRenderer.class.getName() + ".";
    private static final String PREVIOUS_DATE_ATTRIBUTE = ATTRIBUTE_PREFIX + "previousDate";
    private static final String MAX_SUM_ATTRIBUTE = ATTRIBUTE_PREFIX + ".maximumSum";
    private static final String TAXON_PARENT_ID_PARAMETER = "taxonParentId";
    private static final String SORT_SETTING_PREFIX = "sort/";

    protected final ToolPageContext page;

    @Deprecated
    protected final PageWriter writer;

    protected final Search search;
    protected final ObjectField sortField;
    protected final boolean showSiteLabel;
    protected final boolean showTypeLabel;
    protected final PaginatedResult<?> result;
    protected final Exception queryError;

    @SuppressWarnings("deprecation")
    public SearchResultRenderer(ToolPageContext page, Search search) throws IOException {
        this.page = page;
        this.writer = page.getWriter();
        this.search = search;

        ObjectType selectedType = search.getSelectedType();
        ToolUi ui = selectedType == null ? null : selectedType.as(ToolUi.class);
        PaginatedResult<?> result = null;

        if (selectedType != null) {
            if (search.getSort() != null) {
                AuthenticationFilter.Static.putUserSetting(page.getRequest(), SORT_SETTING_PREFIX + selectedType.getId(), search.getSort());

            } else {
                Object sortSetting = AuthenticationFilter.Static.getUserSetting(page.getRequest(), SORT_SETTING_PREFIX + selectedType.getId());

                if (!ObjectUtils.isBlank(sortSetting)) {
                    search.setSort(sortSetting.toString());
                }
            }
        }

        if (search.getSort() == null) {
            if (ui != null && ui.getDefaultSortField() != null) {
                search.setSort(ui.getDefaultSortField());

            } else if (!ObjectUtils.isBlank(search.getQueryString())) {
                search.setSort(Search.RELEVANT_SORT_VALUE);

            } else {
                Map<String, String> f = search.getFieldFilters().get("cms.content.publishDate");

                if (f != null &&
                        (f.get("") != null ||
                        f.get("x") != null)) {
                    search.setSort("cms.content.publishDate");

                } else {
                    search.setSort("cms.content.updateDate");
                }
            }
        }

        showSiteLabel = Query.from(CmsTool.class).first().isDisplaySiteInSearchResult() &&
                page.getSite() == null &&
                Query.from(Site.class).hasMoreThan(0);

        if (selectedType != null) {
            this.sortField = selectedType.getFieldGlobally(search.getSort());
            this.showTypeLabel = selectedType.as(ToolUi.class).findDisplayTypes().size() != 1;

            if (ObjectType.getInstance(ObjectType.class).equals(selectedType)) {
                List<ObjectType> types = new ArrayList<ObjectType>();
                Predicate predicate = search.toQuery(page.getSite()).getPredicate();

                for (ObjectType t : Database.Static.getDefault().getEnvironment().getTypes()) {
                    if (t.is(predicate)) {
                        types.add(t);
                    }
                }

                result = new PaginatedResult<ObjectType>(search.getOffset(), search.getLimit(), types);
            }

        } else {
            this.sortField = Database.Static.getDefault().getEnvironment().getField(search.getSort());
            this.showTypeLabel = search.findValidTypes().size() != 1;
        }

        Exception queryError = null;

        if (result == null) {
            try {
                result = search.toQuery(page.getSite()).select(search.getOffset(), search.getLimit());

            } catch (IllegalArgumentException | Query.NoFieldException error) {
                queryError = error;
            }
        }

        this.result = result;
        this.queryError = queryError;
    }

    @SuppressWarnings("unchecked")
    public void render() throws IOException {
        if (queryError != null) {
            page.writeStart("div", "class", "message message-error");
            page.writeHtml("Invalid advanced query: ");
            page.writeHtml(queryError.getMessage());
            page.writeEnd();
            return;
        }

        boolean resultsDisplayed = false;
        int level = page.paramOrDefault(int.class, TAXON_LEVEL_PARAMETER, 1);

        if (level == 1) {
            page.writeStart("h2").writeHtml("Result").writeEnd();
        }

        // check if the Taxon UI should be displayed
        ObjectType taxonType = null;
        if (ObjectUtils.isBlank(search.getQueryString()) &&
                search.getVisibilities().isEmpty()) {

            if (search.getSelectedType() != null) {

                if (search.getSelectedType().getGroups().contains(Taxon.class.getName())) {
                    taxonType = search.getSelectedType();
                }

            } else if (search.getTypes() != null && search.getTypes().size() == 1) {

                ObjectType searchType = search.getTypes().iterator().next();

                if (searchType.getGroups().contains(Taxon.class.getName())) {
                    taxonType = searchType;
                }
            }
        }

        // display the taxon UI if the type is not null
        if (taxonType != null) {

            search.setSuggestions(false);

            int nextLevel = level + 1;
            Collection<Taxon> taxonResults = null;
            UUID taxonParentUuid = page.paramOrDefault(UUID.class, TAXON_PARENT_ID_PARAMETER, null);
            Site site = page.getSite();
            Predicate predicate = search.toQuery(page.getSite()).getPredicate();

            if (!ObjectUtils.isBlank(taxonParentUuid)) {

                Taxon parent = Query.findById(Taxon.class, taxonParentUuid);
                taxonResults = (Collection<Taxon>) Taxon.Static.getChildren(parent, predicate);

                if (site != null && !ObjectUtils.isBlank(taxonResults)) {

                    Collection<Taxon> siteTaxons = new ArrayList<Taxon>();

                    for (Taxon taxon : taxonResults) {
                        if (PredicateParser.Static.evaluate(taxon, site.itemsPredicate())) {
                            siteTaxons.add(taxon);
                        }
                    }
                    taxonResults = siteTaxons;
                }

            } else {
                taxonResults = Taxon.Static.getRoots((Class<Taxon>) taxonType.getObjectClass(), site, predicate);
            }

            if (!ObjectUtils.isBlank(taxonResults)) {
                resultsDisplayed = true;

                page.writeStart("div", "class", "searchResultList");

                if (level == 1) {
                    page.writeStart("div", "class", "taxonomyContainer");
                    page.writeStart("div", "class", "searchTaxonomy");
                }

                renderTaxonList(taxonResults, nextLevel);

                if (level == 1) {
                    page.writeEnd();
                    page.writeEnd();
                }

                page.writeEnd();
            }
        }

        if (!resultsDisplayed) {
            if (search.findSorts().size() > 1) {
                page.writeStart("div", "class", "searchSorter");
                    renderSorter();
                page.writeEnd();
            }

            page.writeStart("div", "class", "searchPagination");
                renderPagination();
            page.writeEnd();

            page.writeStart("div", "class", "searchResultList");
                if (result.hasPages()) {
                    renderList(result.getItems());
                } else {
                    renderEmpty();
                }
            page.writeEnd();
        }

        if (search.isSuggestions() && ObjectUtils.isBlank(search.getQueryString())) {
            String frameName = page.createId();

            page.writeStart("div", "class", "frame", "name", frameName);
            page.writeEnd();

            page.writeStart("form",
                    "class", "searchSuggestionsForm",
                    "method", "post",
                    "action", page.url("/content/suggestions.jsp"),
                    "target", frameName);
                page.writeElement("input",
                        "type", "hidden",
                        "name", "search",
                        "value", ObjectUtils.toJson(search.getState().getSimpleValues()));
            page.writeEnd();
        }
    }

    private void writeTaxon(Taxon taxon, int nextLevel) throws IOException {
        page.writeStart("li");
            if (taxon.as(Taxon.Data.class).isSelectable()) {
                renderBeforeItem(taxon);
                writeTaxonLabel(taxon);
                renderAfterItem(taxon);
            } else {
                writeTaxonLabel(taxon);
            }

            Predicate predicate = search.toQuery(page.getSite()).getPredicate();
            Collection<? extends Taxon> children = Taxon.Static.getChildren(taxon, predicate);

            if (children != null && !children.isEmpty()) {
                page.writeStart("a",
                        "href", page.url("", TAXON_PARENT_ID_PARAMETER, taxon.as(Taxon.Data.class).getId(), TAXON_LEVEL_PARAMETER, nextLevel),
                        "class", "taxonomyExpand",
                        "target", "taxonChildren-d" + nextLevel);
                page.writeEnd();
            }
        page.writeEnd();
    }

    private void writeTaxonLabel(Taxon taxon) throws IOException {
        if (taxon == null) {
            page.writeHtml("N/A");
        }
        String altLabel = taxon.as(Taxon.Data.class).getAltLabel();
        if (ObjectUtils.isBlank(altLabel)) {
            page.writeObjectLabel(taxon);
        } else {
            String visibilityLabel = taxon.getState().getVisibilityLabel();
            if (!ObjectUtils.isBlank(visibilityLabel)) {
                page.writeStart("span", "class", "visibilityLabel");
                    page.writeHtml(visibilityLabel);
                page.writeEnd();

                page.writeHtml(" ");
            }
            page.writeHtml(altLabel);
        }
    }

    public void renderSorter() throws IOException {
        page.writeStart("form",
                "data-bsp-autosubmit", "",
                "method", "get",
                "action", page.url(null));

            for (Map.Entry<String, List<String>> entry : StringUtils.getQueryParameterMap(page.url("",
                    Search.SORT_PARAMETER, null,
                    Search.SHOW_MISSING_PARAMETER, null,
                    Search.OFFSET_PARAMETER, null)).entrySet()) {
                String name = entry.getKey();

                for (String value : entry.getValue()) {
                    page.writeElement("input", "type", "hidden", "name", name, "value", value);
                }
            }

            page.writeStart("select", "name", Search.SORT_PARAMETER);
                for (Map.Entry<String, String> entry : search.findSorts().entrySet()) {
                    String label = entry.getValue();
                    String value = entry.getKey();

                    page.writeStart("option",
                            "value", value,
                            "selected", value.equals(search.getSort()) ? "selected" : null);
                        page.writeHtml("Sort: ").writeHtml(label);
                    page.writeEnd();
                }
            page.writeEnd();

        page.writeEnd();
    }

    public void renderPagination() throws IOException {
        page.writeStart("ul", "class", "pagination");

            if (result.hasPrevious()) {
                page.writeStart("li", "class", "previous");
                    page.writeStart("a", "href", page.url("", Search.OFFSET_PARAMETER, result.getPreviousOffset()));
                        page.writeHtml("Previous ");
                        page.writeHtml(result.getLimit());
                    page.writeEnd();
                page.writeEnd();
            }

            page.writeStart("li");
                page.writeHtml(result.getFirstItemIndex());
                page.writeHtml(" to ");
                page.writeHtml(result.getLastItemIndex());
                page.writeHtml(" of ");
                page.writeStart("strong").writeHtml(result.getCount()).writeEnd();
            page.writeEnd();

            if (result.hasNext()) {
                page.writeStart("li", "class", "next");
                    page.writeStart("a", "href", page.url("", Search.OFFSET_PARAMETER, result.getNextOffset()));
                        page.writeHtml("Next ");
                        page.writeHtml(result.getLimit());
                    page.writeEnd();
                page.writeEnd();
            }

        page.writeEnd();
    }

    public void renderList(Collection<?> listItems) throws IOException {
        List<Object> items = new ArrayList<Object>(listItems);
        Map<Object, StorageItem> previews = new LinkedHashMap<Object, StorageItem>();

        ITEM: for (ListIterator<Object> i = items.listIterator(); i.hasNext();) {
            Object item = i.next();

            for (Tool tool : Query.from(Tool.class).selectAll()) {
                if (!tool.isDisplaySearchResultItem(search, item)) {
                    continue ITEM;
                }
            }

            State itemState = State.getInstance(item);
            StorageItem preview = itemState.getPreview();

            if (preview != null) {
                String contentType = preview.getContentType();

                if (contentType != null && contentType.startsWith("image/")) {
                    i.remove();
                    previews.put(item, preview);
                }
            }
        }

        if (!previews.isEmpty()) {
            page.writeStart("div", "class", "searchResultImages");
                for (Map.Entry<Object, StorageItem> entry : previews.entrySet()) {
                    renderImage(entry.getKey(), entry.getValue());
                }
            page.writeEnd();
        }

        if (!items.isEmpty()) {
            page.writeStart("table", "class", "searchResultTable links table-striped pageThumbnails");
                page.writeStart("tbody");
                    for (Object item : items) {
                        renderRow(item);
                    }
                page.writeEnd();
            page.writeEnd();
        }
    }

    public void renderTaxonList(Collection<?> listItems, int nextLevel) throws IOException {
        page.writeStart("ul", "class", "taxonomy");

        for (Taxon taxon : (Collection<Taxon>) listItems) {
            writeTaxon(taxon, nextLevel);
        }

        page.writeEnd();
        page.writeStart("div",
                "class", "frame taxonChildren",
                "name", "taxonChildren-d" + nextLevel);
        page.writeEnd();
    }

    public void renderImage(Object item, StorageItem image) throws IOException {
        renderBeforeItem(item);

        page.writeStart("figure");
            page.writeElement("img",
                    "src", page.getPreviewThumbnailUrl(item),
                    "alt",
                            (showSiteLabel ? page.getObjectLabel(State.getInstance(item).as(Site.ObjectModification.class).getOwner()) + ": " : "") +
                            (showTypeLabel ? page.getTypeLabel(item) + ": " : "") +
                            page.getObjectLabel(item));

            page.writeStart("figcaption");
                if (showSiteLabel) {
                    page.writeObjectLabel(State.getInstance(item).as(Site.ObjectModification.class).getOwner());
                    page.writeHtml(": ");
                }

                if (showTypeLabel) {
                    page.writeTypeLabel(item);
                    page.writeHtml(": ");
                }

                page.writeObjectLabel(item);
            page.writeEnd();
        page.writeEnd();

        renderAfterItem(item);
    }

    public void renderRow(Object item) throws IOException {
        HttpServletRequest request = page.getRequest();
        State itemState = State.getInstance(item);
        String permalink = itemState.as(Directory.ObjectModification.class).getPermalink();
        Integer embedWidth = null;

        if (ObjectUtils.isBlank(permalink)) {
            ObjectType type = itemState.getType();

            if (type != null) {
                Renderer.TypeModification rendererData = type.as(Renderer.TypeModification.class);
                int previewWidth = rendererData.getEmbedPreviewWidth();

                if (previewWidth > 0 &&
                        !ObjectUtils.isBlank(rendererData.getEmbedPath())) {
                    permalink = "/_preview?_embed=true&_cms.db.previewId=" + itemState.getId();
                    embedWidth = 320;
                }
            }
        }

        page.writeStart("tr",
                "data-preview-url", permalink,
                "data-preview-embed-width", embedWidth,
                "class", State.getInstance(item).getId().equals(page.param(UUID.class, "id")) ? "selected" : null);

            if (sortField != null &&
                    ObjectField.DATE_TYPE.equals(sortField.getInternalType())) {
                DateTime dateTime = page.toUserDateTime(itemState.getByPath(sortField.getInternalName()));

                if (dateTime == null) {
                    page.writeStart("td", "colspan", 2);
                        page.writeHtml("N/A");
                    page.writeEnd();

                } else {
                    String date = page.formatUserDate(dateTime);

                    page.writeStart("td", "class", "date");
                        if (!ObjectUtils.equals(date, request.getAttribute(PREVIOUS_DATE_ATTRIBUTE))) {
                            request.setAttribute(PREVIOUS_DATE_ATTRIBUTE, date);
                            page.writeHtml(date);
                        }
                    page.writeEnd();

                    page.writeStart("td", "class", "time");
                        page.writeHtml(page.formatUserTime(dateTime));
                    page.writeEnd();
                }
            }

            if (showSiteLabel) {
                page.writeStart("td");
                    page.writeObjectLabel(itemState.as(Site.ObjectModification.class).getOwner());
                page.writeEnd();
            }

            if (showTypeLabel) {
                page.writeStart("td");
                    page.writeTypeLabel(item);
                page.writeEnd();
            }

            page.writeStart("td", "data-preview-anchor", "");
                renderBeforeItem(item);
                page.writeObjectLabel(item);
                renderAfterItem(item);
            page.writeEnd();

            if (sortField != null &&
                    !ObjectField.DATE_TYPE.equals(sortField.getInternalType())) {
                String sortFieldName = sortField.getInternalName();
                Object value = itemState.getByPath(sortFieldName);

                page.writeStart("td");
                    if (value instanceof Metric) {
                        page.writeStart("span", "style", page.cssString("white-space", "nowrap"));
                            Double maxSum = (Double) request.getAttribute(MAX_SUM_ATTRIBUTE);

                            if (maxSum == null) {
                                Object maxObject = search.toQuery(page.getSite()).sortDescending(sortFieldName).first();
                                maxSum = maxObject != null ?
                                        ((Metric) State.getInstance(maxObject).get(sortFieldName)).getSum() :
                                        1.0;

                                request.setAttribute(MAX_SUM_ATTRIBUTE, maxSum);
                            }

                            Metric valueMetric = (Metric) value;
                            Map<DateTime, Double> sumEntries = valueMetric.groupSumByDate(
                                    new MetricInterval.Daily(),
                                    new DateTime().dayOfMonth().roundFloorCopy().minusDays(7),
                                    null);

                            double sum = valueMetric.getSum();
                            long sumLong = (long) sum;

                            if (sumLong == sum) {
                                page.writeHtml(String.format("%,2d ", sumLong));

                            } else {
                                page.writeHtml(String.format("%,2.2f ", sum));
                            }

                            if (!sumEntries.isEmpty()) {
                                long minMillis = Long.MAX_VALUE;
                                long maxMillis = Long.MIN_VALUE;

                                for (Map.Entry<DateTime, Double> sumEntry : sumEntries.entrySet()) {
                                    long sumMillis = sumEntry.getKey().getMillis();

                                    if (sumMillis < minMillis) {
                                        minMillis = sumMillis;
                                    }

                                    if (sumMillis > maxMillis) {
                                        maxMillis = sumMillis;
                                    }
                                }

                                double cumulativeSum = 0.0;
                                StringBuilder path = new StringBuilder();
                                double xRange = maxMillis - minMillis;
                                int width = 35;
                                int height = 18;

                                for (Map.Entry<DateTime, Double> sumEntry : sumEntries.entrySet()) {
                                    cumulativeSum += sumEntry.getValue();

                                    path.append('L');
                                    path.append((sumEntry.getKey().getMillis() - minMillis) / xRange * width);
                                    path.append(',');
                                    path.append(height - cumulativeSum / maxSum * height);
                                }

                                path.setCharAt(0, 'M');

                                page.writeStart("svg",
                                        "xmlns", "http://www.w3.org/2000/svg",
                                        "width", width,
                                        "height", height,
                                        "style", page.cssString(
                                                "display", "inline-block",
                                                "vertical-align", "middle"));
                                    page.writeStart("path",
                                            "fill", "none",
                                            "stroke", "#444444",
                                            "d", path.toString());
                                    page.writeEnd();
                                page.writeEnd();
                            }
                        page.writeEnd();

                    } else if (value instanceof Recordable) {
                        page.writeHtml(((Recordable) value).getState().getLabel());

                    } else {
                        page.writeHtml(value);
                    }
                page.writeEnd();
            }

        page.writeEnd();
    }

    public void renderBeforeItem(Object item) throws IOException {
        page.writeStart("a",
                "href", page.toolUrl(CmsTool.class, "/content/edit.jsp",
                        "id", State.getInstance(item).getId(),
                        "search", page.url("", Search.NAME_PARAMETER, null)),
                "data-objectId", State.getInstance(item).getId(),
                "target", "_top");
    }

    public void renderAfterItem(Object item) throws IOException {
        page.writeEnd();
    }

    public void renderEmpty() throws IOException {
        page.writeStart("div", "class", "message message-warning");
            page.writeStart("p");
                page.writeHtml("No matching items!");
            page.writeEnd();
        page.writeEnd();
    }
}
