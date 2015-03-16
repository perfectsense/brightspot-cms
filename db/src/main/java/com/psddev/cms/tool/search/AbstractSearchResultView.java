package com.psddev.cms.tool.search;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultItem;
import com.psddev.cms.tool.SearchResultView;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PaginatedResult;
import com.psddev.dari.util.StringUtils;

public abstract class AbstractSearchResultView implements SearchResultView {

    private static final String SORT_SETTING_PREFIX = "sort/";
    private static final String SORT_SHOW_MISSING_SETTING_PREFIX = "sortShowMissing/";

    protected ToolPageContext page;
    protected Search search;
    protected SearchResultItem itemWriter;

    @Override
    public boolean isSupported(Search search) {
        return true;
    }

    @Override
    public boolean isPreferred(Search search) {
        return false;
    }

    @Override
    public boolean isHtmlWrapped(
            Search search,
            ToolPageContext page,
            SearchResultItem itemWriter) {

        return true;
    }

    @Override
    public final void writeHtml(
            Search search,
            ToolPageContext page,
            SearchResultItem itemWriter)
            throws IOException {

        if (this.page != null) {
            throw new IllegalStateException("writeHtml can only be called once!");
        }

        this.page = page;
        this.search = search;
        this.itemWriter = itemWriter;

        doWriteHtml();
    }

    protected abstract void doWriteHtml() throws IOException;

    protected void writeFieldsHtml() throws IOException {
        ObjectType type = search.getSelectedType();
        ToolUser user = page.getUser();

        page.writeStart("div", "class", "searchResult-fields");
            page.writeStart("a",
                    "target", "searchResultFields",
                    "href", page.toolUrl(CmsTool.class, "/searchResultFields",
                            "typeId", type != null ? type.getId() : null));

                page.writeHtml("Fields: ");
                page.writeHtml(user != null &&
                        user.getSearchResultFieldsByTypeId().get(type != null ? type.getId().toString() : "") != null ?
                        "Custom" :
                        "Default");
            page.writeEnd();
        page.writeEnd();
    }

    protected ObjectField updateSort() {
        ObjectType selectedType = search.getSelectedType();

        if (selectedType != null) {
            if (search.getSort() != null) {
                AuthenticationFilter.Static.putUserSetting(page.getRequest(), SORT_SETTING_PREFIX + selectedType.getId(), search.getSort());
                AuthenticationFilter.Static.putUserSetting(page.getRequest(), SORT_SHOW_MISSING_SETTING_PREFIX + selectedType.getId(), ObjectUtils.to(String.class, search.isShowMissing()));

            } else {
                Object sortSetting = AuthenticationFilter.Static.getUserSetting(page.getRequest(), SORT_SETTING_PREFIX + selectedType.getId());

                if (!ObjectUtils.isBlank(sortSetting)) {
                    search.setSort(sortSetting.toString());

                    Object showMissingSetting = AuthenticationFilter.Static.getUserSetting(page.getRequest(), SORT_SHOW_MISSING_SETTING_PREFIX + selectedType.getId());

                    if (!ObjectUtils.isBlank(showMissingSetting)) {
                        search.setShowMissing(ObjectUtils.to(Boolean.class, showMissingSetting));
                    }
                }
            }
        }

        if (search.getSort() == null) {
            ToolUi ui = selectedType == null ? null : selectedType.as(ToolUi.class);

            search.setShowMissing(true);

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

        if (selectedType != null) {
            return selectedType.getFieldGlobally(search.getSort());

        } else {
            return Database.Static.getDefault().getEnvironment().getField(search.getSort());
        }
    }

    protected void writeSortsHtml() throws IOException {
        if (search.findSorts().size() < 2) {
            return;
        }

        page.writeStart("div", "class", "searchResult-sorts");
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

                page.writeHtml(" ");

                page.writeElement("input",
                        "id", page.createId(),
                        "type", "checkbox",
                        "name", Search.SHOW_MISSING_PARAMETER,
                        "value", "true",
                        "checked", search.isShowMissing() ? "checked" : null);

                page.writeHtml(" ");

                page.writeStart("label", "for", page.getId());
                    page.writeHtml("Show Missing");
                page.writeEnd();
            page.writeEnd();
        page.writeEnd();
    }

    protected void writePaginationHtml(PaginatedResult<?> result) throws IOException {
        page.writeStart("div", "class", "searchResult-pagination");
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
        page.writeEnd();
    }

    protected void writeEmptyHtml() throws IOException {
        page.writeStart("div", "class", "message message-warning");
            page.writeStart("p");
                page.writeHtml("No matching items!");
            page.writeEnd();
        page.writeEnd();
    }
}
