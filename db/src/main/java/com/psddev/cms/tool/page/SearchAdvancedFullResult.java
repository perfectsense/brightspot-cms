package com.psddev.cms.tool.page;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.ImageTag;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultRenderer;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.Metric;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CollectionUtils;
import com.psddev.dari.util.HtmlFormatter;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ImageEditor;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.UrlBuilder;

@RoutingFilter.Path(application = "cms", value = "searchAdvancedFullResult")
public class SearchAdvancedFullResult extends PageServlet {

    private static final long serialVersionUID = 1L;

    private static final String FIELDS_PARAMETER = "f";
    private static final String ITEMS_PARAMETER = "i";
    private static final int[] LIMITS = { 20, 50, 100 };

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Search search = new Search(page);
        Query<?> query = search.toQuery(page.getSite());
        DatabaseEnvironment environment = Database.Static.getDefault().getEnvironment();
        ObjectType type = search.getSelectedType();
        List<Display> allDisplays = new ArrayList<Display>();

        allDisplays.add(new ReferencesDisplay());
        allDisplays.add(new PathsDisplay());

        for (String name : new String[] {
                "cms.content.publishDate",
                "cms.content.publishUser",
                "cms.content.updateDate",
                "cms.content.updateUser" }) {

            allDisplays.add(new ObjectFieldDisplay(environment.getField(name)));
        }

        if (type != null) {
            for (ObjectField field : type.getFields()) {
                allDisplays.add(new ObjectFieldDisplay(field));
            }
        }

        List<String> displayNames = page.params(String.class, FIELDS_PARAMETER);
        List<Display> displays = new ArrayList<Display>();

        for (Display display : allDisplays) {
            if (displayNames.contains(display.getInternalName())) {
                displays.add(display);
            }
        }

        Collections.sort(allDisplays);
        Collections.sort(displays);

        List<UUID> ids = page.params(UUID.class, ITEMS_PARAMETER);

        if (page.param(String.class, "action-download") != null) {
            HttpServletResponse response = page.getResponse();

            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=search-result-" + new DateTime(null, page.getUserDateTimeZone()).toString("yyyy-MM-dd-hh-mm-ss") + ".csv");
            page.write('\ufeff');

            page.write("\"");
            writeCsvItem(page, "Type");
            page.write("\",\"");
            writeCsvItem(page, "Label");
            page.write("\"");

            for (Display display : displays) {
                page.write(",\"");
                writeCsvItem(page, display.getDisplayName());
                page.write("\"");
            }

            page.write("\r\n");

            Iterator<?> queryIterator = ids.isEmpty() ?
                    query.noCache().iterable(0).iterator() :
                    Query.fromAll().noCache().where("_id = ?", ids).selectAll().iterator();

            try {
                while (queryIterator.hasNext()) {
                    Object item = queryIterator.next();
                    State itemState = State.getInstance(item);

                    page.write("\"");
                    writeCsvItem(page, page.getTypeLabel(item));
                    page.write("\",\"");
                    writeCsvItem(page, page.getObjectLabel(item));
                    page.write("\"");

                    for (Display display : displays) {
                        page.write(",\"");
                        writeCsvItem(page, display.getCsvItem(itemState));
                        page.write("\"");
                    }

                    page.write("\r\n");
                }

            } finally {
                if (queryIterator instanceof Closeable) {
                    ((Closeable) queryIterator).close();
                }
            }

            return;

        } else if (page.param(String.class, "action-trash") != null) {
            Iterator<?> queryIterator = ids.isEmpty() ?
                    query.noCache().iterable(0).iterator() :
                    Query.fromAll().where("_id = ?", ids).selectAll().iterator();

            try {
                while (queryIterator.hasNext()) {
                    page.trash(queryIterator.next());
                }

            } finally {
                if (queryIterator instanceof Closeable) {
                    ((Closeable) queryIterator).close();
                }
            }

            page.getResponse().sendRedirect(page.param(String.class, "returnUrl"));
            return;
        }

        if (search.getLimit() < 20) {
            search.setLimit(20);
        }

        search.setSuggestions(false);

        Renderer renderer = new Renderer(page, search, allDisplays, displays);

        page.putOverride(Recordable.class, new HtmlFormatter<Recordable>() {

            @Override
            public void format(HtmlWriter writer, Recordable object) throws IOException {
                ToolPageContext page = (ToolPageContext) writer;

                page.writeObjectLabel(object);
            }
        });

        page.putOverride(Metric.class, new HtmlFormatter<Metric>() {

            @Override
            public void format(HtmlWriter writer, Metric object) throws IOException {
                writer.write(new Double(object.getSum()).toString());
            }
        });

        page.putOverride(Content.class, new HtmlFormatter<Content>() {

            @Override
            public void format(HtmlWriter writer, Content content) throws IOException {
                ToolPageContext page = (ToolPageContext) writer;

                page.writeStart("a",
                        "href", page.objectUrl("/content/edit.jsp", content),
                        "target", "_top");
                    page.writeObjectLabel(content);
                page.writeEnd();
            }
        });

        page.putOverride(StorageItem.class, new HtmlFormatter<StorageItem>() {

            @Override
            public void format(HtmlWriter writer, StorageItem item) throws IOException {
                ToolPageContext page = (ToolPageContext) writer;

                page.writeElement("img",
                        "height", 100,
                        "src", ImageEditor.Static.getDefault() != null ?
                                new ImageTag.Builder(item).setHeight(100).toUrl() :
                                item.getPublicUrl());
            }
        });

        renderer.render();
    }

    private void writeCsvItem(ToolPageContext page, Object item) throws IOException {
        page.write(item.toString().replaceAll("\"", "\"\""));
    }

    private static class Renderer extends SearchResultRenderer {

        private final ObjectType type;
        private final List<Display> allDisplays;
        private final List<Display> displays;

        public Renderer(
                ToolPageContext page,
                Search search,
                List<Display> allDisplays,
                List<Display> displays)
                throws IOException {

            super(page, search);

            this.type = search.getSelectedType();
            this.allDisplays = allDisplays;
            this.displays = displays;
        }

        @Override
        public void renderSorter() throws IOException {
            super.renderSorter();

            page.writeStart("form",
                    "method", "get",
                    "action", page.url(null),
                    "style", page.cssString(
                            "display", "inline-block",
                            "margin-left", "20px",
                            "width", "200px"));

                for (Map.Entry<String, List<String>> entry : StringUtils.getQueryParameterMap(page.url("",
                        FIELDS_PARAMETER, null)).entrySet()) {
                    String name = entry.getKey();

                    for (String value : entry.getValue()) {
                        page.writeElement("input", "type", "hidden", "name", name, "value", value);
                    }
                }

                page.writeStart("select",
                        "data-bsp-autosubmit", "",
                        "name", FIELDS_PARAMETER,
                        "multiple", "multiple",
                        "placeholder", "Fields");

                    for (Display display: allDisplays) {
                        if (display instanceof ObjectFieldDisplay &&
                                ((ObjectFieldDisplay) display).getField().as(ToolUi.class).isHidden()) {
                            continue;
                        }

                        page.writeStart("option",
                                "selected", displays.contains(display) ? "selected" : null,
                                "value", display.getInternalName());
                            page.writeHtml(display.getDisplayName());
                        page.writeEnd();
                    }
                page.writeEnd();

            page.writeEnd();
        }

        @Override
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

                if (result.getOffset() > 0 ||
                        result.hasNext() ||
                        result.getItems().size() > LIMITS[0]) {
                    page.writeStart("li");
                        page.writeHtml("Show: ");

                        for (int i = 0, length = LIMITS.length; i < length; ++ i) {
                            int l = LIMITS[i];

                            if (i > 0) {
                                page.writeHtml(" | ");
                            }

                            page.writeStart("a", "href", page.url("", Search.LIMIT_PARAMETER, l));
                                if (result.getLimit() == l) {
                                    page.writeStart("strong");
                                        page.writeHtml(l);
                                    page.writeEnd();

                                } else {
                                    page.writeHtml(l);
                                }
                            page.writeEnd();
                        }
                    page.writeEnd();
                }

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

        @Override
        public void renderList(Collection<?> items) throws IOException {
            page.writeStart("form",
                    "class", "searchAdvancedResult",
                    "method", "post",
                    "action", page.url(""),
                    "target", "_top");

                page.writeElement("input",
                        "type", "hidden",
                        "name", "returnUrl",
                        "value", new UrlBuilder(page.getRequest()).
                                absolutePath(page.cmsUrl("/searchAdvancedFull")).
                                currentParameters());

                for (Display display : displays) {
                    page.writeElement("input", "type", "hidden", "name", FIELDS_PARAMETER, "value", display.getInternalName());
                }

                page.writeStart("table", "class", "table-bordered table-striped pageThumbnails");
                    page.writeStart("thead");
                        page.writeStart("tr");
                            page.writeStart("th");
                            page.writeEnd();

                            page.writeStart("th");
                                page.writeHtml("Type");
                            page.writeEnd();

                            page.writeStart("th");
                                page.writeHtml("Label");
                            page.writeEnd();

                            for (Display display : displays) {
                                page.writeStart("th");
                                    page.writeHtml(display.getDisplayName());
                                page.writeEnd();
                            }
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("tbody");
                        for (Object item : items) {
                            renderRow(item);
                        }
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("div", "class", "actions");
                    page.writeStart("button",
                            "class", "action icon icon-action-download",
                            "name", "action-download",
                            "value", true);

                        page.writeHtml("Export All");
                    page.writeEnd();

                    if (type != null) {
                        page.writeStart("button",
                                "class", "action icon icon-action-edit",
                                "formaction", new UrlBuilder(page.getRequest()).
                                        absolutePath(page.cmsUrl("/contentEditBulk")).
                                        currentParameters().
                                        parameter("typeId", type.getId()));

                            page.writeHtml("Bulk Edit All");
                        page.writeEnd();
                    }

                    page.writeStart("a",
                            "class", "action button icon icon-object-workStream",
                            "target", "workStreamCreate",
                            "href", page.cmsUrl("/content/newWorkStream.jsp",
                                    "search", ObjectUtils.toJson(search.getState().getSimpleValues())));

                        page.writeHtml("New Work Stream");
                    page.writeEnd();

                    page.writeStart("button",
                            "class", "action action-pullRight link icon icon-action-trash",
                            "name", "action-trash",
                            "value", "true");

                        page.writeHtml("Bulk Archive All");
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        }

        @Override
        public void renderRow(Object item) throws IOException {
            State itemState = State.getInstance(item);
            String permalink = itemState.as(Directory.ObjectModification.class).getPermalink();

            page.writeStart("tr", "data-preview-url", permalink);
                page.writeStart("td", "style", "width: 20px;");
                    page.writeElement("input",
                            "type", "checkbox",
                            "name", ITEMS_PARAMETER,
                            "value", itemState.getId());

                page.writeEnd();

                page.writeStart("td");
                    page.writeHtml(page.getTypeLabel(item));
                page.writeEnd();

                page.writeStart("td", "data-preview-anchor", "");
                    page.writeStart("a",
                            "href", page.objectUrl("/content/edit.jsp", item),
                            "target", "_top");

                        page.writeObjectLabel(item);
                    page.writeEnd();
                page.writeEnd();

                for (Display display : displays) {
                    page.writeStart("td");
                        display.writeCell(itemState, page);
                    page.writeEnd();
                }
            page.writeEnd();
        }
    }

    private abstract static class Display implements Comparable<Display> {

        public abstract String getInternalName();

        public abstract String getDisplayName();

        public abstract String getCsvItem(State itemState);

        public abstract void writeCell(State itemState, HtmlWriter writer) throws IOException;

        @Override
        public int compareTo(Display other) {
            return getDisplayName().compareTo(other.getDisplayName());
        }

        @Override
        public int hashCode() {
            return getInternalName().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;

            } else if (other instanceof Display) {
                return getInternalName().equals(((Display) other).getInternalName());

            } else {
                return false;
            }
        }
    }

    private static class ObjectFieldDisplay extends Display {

        private final ObjectField field;

        public ObjectFieldDisplay(ObjectField field) {
            this.field = field;
        }

        public ObjectField getField() {
            return field;
        }

        @Override
        public String getInternalName() {
            return field.getInternalName();
        }

        @Override
        public String getDisplayName() {
            return field.getDisplayName();
        }

        @Override
        public String getCsvItem(State itemState) {
            StringWriter string = new StringWriter();
            @SuppressWarnings("resource")
            HtmlWriter html = new HtmlWriter(string);

            html.putOverride(Recordable.class, new HtmlFormatter<Recordable>() {

                @Override
                public void format(HtmlWriter writer, Recordable object) throws IOException {
                    if (object != null) {
                        writer.writeRaw(object.getState().getLabel());
                    }
                }
            });

            html.putOverride(Metric.class, new HtmlFormatter<Metric>() {

                @Override
                public void format(HtmlWriter writer, Metric object) throws IOException {
                    writer.write(new Double(object.getSum()).toString());
                }
            });

            html.putOverride(StorageItem.class, new HtmlFormatter<StorageItem>() {

                @Override
                public void format(HtmlWriter writer, StorageItem item) throws IOException {
                    writer.write(item.getPublicUrl());
                }
            });

            try {
                for (Iterator<Object> i = CollectionUtils.recursiveIterable(itemState.get(getInternalName())).iterator(); i.hasNext();) {
                    Object value = i.next();

                    html.writeObject(value);

                    if (i.hasNext()) {
                        html.write(", ");
                    }
                }

            } catch (IOException error) {
                throw new IllegalStateException(error);
            }

            return string.toString();
        }

        @Override
        public void writeCell(State itemState, HtmlWriter writer) throws IOException {
            for (Iterator<Object> i = CollectionUtils.recursiveIterable(itemState.get(getInternalName())).iterator(); i.hasNext();) {
                Object value = i.next();

                writer.writeObject(value);

                if (i.hasNext()) {
                    writer.writeHtml(", ");
                }
            }
        }
    }

    private static class PathsDisplay extends ObjectFieldDisplay {

        public PathsDisplay() {
            super(Database.Static.getDefault().getEnvironment().getField("cms.directory.paths"));
        }

        @Override
        public String getCsvItem(State itemState) {
            StringBuilder csvItem = new StringBuilder();

            for (Iterator<Directory.Path> i = itemState.as(Directory.ObjectModification.class).getPaths().iterator(); i.hasNext();) {
                Directory.Path p = i.next();
                String path = p.getPath();

                csvItem.append(path);
                csvItem.append(" (");
                csvItem.append(p.getType());
                csvItem.append(")");

                if (i.hasNext()) {
                    csvItem.append(", ");
                }
            }

            return csvItem.toString();
        }

        @Override
        public void writeCell(State itemState, HtmlWriter writer) throws IOException {
            for (Directory.Path p : itemState.as(Directory.ObjectModification.class).getPaths()) {
                String path = p.getPath();

                writer.writeStart("div");
                    writer.writeStart("a", "href", path, "target", "_blank");
                        writer.writeHtml(path);
                    writer.writeEnd();

                    writer.writeHtml(" (");
                    writer.writeHtml(p.getType());
                    writer.writeHtml(")");
                writer.writeEnd();
            }
        }
    }

    private static class ReferencesDisplay extends Display {

        @Override
        public String getInternalName() {
            return "_numRefs";
        }

        @Override
        public String getDisplayName() {
            return "# Of References";
        }

        private long getReferencesCount(State itemState) {
            return Query.
                    fromAll().
                    where("* matches ?", itemState.getId()).
                    count();
        }

        @Override
        public String getCsvItem(State itemState) {
            return String.valueOf(getReferencesCount(itemState));
        }

        @Override
        public void writeCell(State itemState, HtmlWriter writer) throws IOException {
            writer.writeHtml(String.format("%,d", getReferencesCount(itemState)));
        }
    }
}
