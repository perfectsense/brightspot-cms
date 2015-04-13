package com.psddev.cms.tool.page;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.ImageTag;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.PageServlet;
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
import com.psddev.dari.util.PaginatedResult;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StorageItem;

@RoutingFilter.Path(application = "cms", value = "/content/searchAdvanced")
@SuppressWarnings("serial")
public class ContentSearchAdvanced extends PageServlet {

    public static final String TYPE_PARAMETER = "t";
    public static final String PREDICATE_PARAMETER = "p";
    public static final String FIELDS_PARAMETER = "f";
    public static final String ITEMS_PARAMETER = "i";

    private static final int[] LIMITS = { 10, 20, 50, Integer.MAX_VALUE };

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(final ToolPageContext page) throws IOException, ServletException {
        DatabaseEnvironment environment = Database.Static.getDefault().getEnvironment();
        ObjectType type = environment.getTypeById(page.param(UUID.class, TYPE_PARAMETER));
        String predicate = page.param(String.class, PREDICATE_PARAMETER);
        List<ObjectField> allFields = new ArrayList<ObjectField>();

        for (String fieldName : new String[] {
                "cms.content.publishDate",
                "cms.content.publishUser",
                "cms.content.updateDate",
                "cms.content.updateUser",
                "cms.directory.paths" }) {
            allFields.add(environment.getField(fieldName));
        }

        if (type != null) {
            allFields.addAll(type.getFields());
        }

        List<String> fieldNames = page.params(String.class, FIELDS_PARAMETER);
        List<ObjectField> fields = new ArrayList<ObjectField>();

        for (ObjectField field : allFields) {
            if (fieldNames.contains(field.getInternalName())) {
                fields.add(field);
            }
        }

        Collections.sort(allFields);
        Collections.sort(fields);

        List<UUID> ids = page.params(UUID.class, ITEMS_PARAMETER);
        Query<Object> query = (type != null ?
                Query.fromType(type) :
                Query.fromGroup(Content.SEARCHABLE_GROUP)).
                where(predicate);

        if (page.param(String.class, "action-download") != null) {
            HttpServletResponse response = page.getResponse();

            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=search-result-" + new DateTime(null, page.getUserDateTimeZone()).toString("yyyy-MM-dd-hh-mm-ss") + ".csv");

            page.putOverride(Recordable.class, new HtmlFormatter<Recordable>() {
                @Override
                public void format(HtmlWriter writer, Recordable object) throws IOException {
                    ToolPageContext page = (ToolPageContext) writer;
                    writer.write(page.getObjectLabel(object));
                }
            });

            page.putOverride(Metric.class, new HtmlFormatter<Metric>() {
                @Override
                public void format(HtmlWriter writer, Metric object) throws IOException {
                    writer.write(new Double(object.getSum()).toString());
                }
            });

            page.putOverride(StorageItem.class, new HtmlFormatter<StorageItem>() {
                @Override
                public void format(HtmlWriter writer, StorageItem item) throws IOException {
                    writer.write(item.getPublicUrl());
                }
            });

            page.write('\ufeff');

            page.write("\"");
            writeCsvItem(page, "Type");
            page.write("\",\"");
            writeCsvItem(page, "Label");
            page.write("\"");

            for (ObjectField field : fields) {
                page.write(",\"");
                writeCsvItem(page, field.getDisplayName());
                page.write("\"");
            }

            page.write("\r\n");

            for (Object item : query.noCache().iterable(0)) {
                State itemState = State.getInstance(item);

                if (!ids.isEmpty() && !ids.contains(itemState.getId())) {
                    continue;
                }

                page.write("\"");
                writeCsvItem(page, page.getTypeLabel(item));
                page.write("\",\"");
                writeCsvItem(page, page.getObjectLabel(item));
                page.write("\"");

                for (ObjectField field : fields) {
                    page.write(",\"");

                    if ("cms.directory.paths".equals(field.getInternalName())) {
                        for (Iterator<Directory.Path> i = itemState.as(Directory.ObjectModification.class).getPaths().iterator(); i.hasNext();) {
                            Directory.Path p = i.next();
                            String path = p.getPath();

                            page.writeHtml(path);
                            page.writeHtml(" (");
                            page.writeHtml(p.getType());
                            page.writeHtml(")");

                            if (i.hasNext()) {
                                page.write(", ");
                            }
                        }

                    } else {
                        for (Iterator<Object> i = CollectionUtils.recursiveIterable(itemState.getByPath(field.getInternalName())).iterator(); i.hasNext();) {
                            Object value = i.next();
                            page.writeObject(value);
                            if (i.hasNext()) {
                                page.write(", ");
                            }
                        }
                    }

                    page.write("\"");
                }

                page.write("\r\n");
            }

            return;

        } else if (page.param(String.class, "action-trash") != null) {
            Iterator<Object> queryIterator = query.noCache().iterable(0).iterator();

            try {
                while (queryIterator.hasNext()) {
                    Object item = queryIterator.next();
                    State itemState = State.getInstance(item);

                    if (!ids.isEmpty() && !ids.contains(itemState.getId())) {
                        continue;
                    }

                    page.trash(item);
                }

            } finally {
                if (queryIterator instanceof Closeable) {
                    ((Closeable) queryIterator).close();
                }
            }

            page.getResponse().sendRedirect(page.param(String.class, "returnUrl"));
            return;
        }

        long offset = page.param(long.class, "offset");
        int limit = page.pageParam(int.class, "limit", 20);
        PaginatedResult<Object> result;

        try {
            result = query.select(offset, limit);

        } catch (RuntimeException error) {
            result = null;
        }

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
                        "href", page.objectUrl("/content/edit.jsp", content));
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

        page.writeHeader();
            page.writeStart("div", "class", "widget");
                page.writeStart("h1", "class", "icon icon-search");
                    page.writeHtml("Advanced Search");
                page.writeEnd();

                page.writeStart("form",
                        "method", "get",
                        "action", page.url(null));
                    page.writeStart("div", "class", "inputContainer");
                        page.writeStart("div", "class", "inputLabel");
                            page.writeStart("label", "for", page.createId());
                                page.writeHtml("Type");
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("div", "class", "inputSmall");
                            page.writeTypeSelect(
                                    environment.getTypes(),
                                    type,
                                    "Any Types",
                                    "data-bsp-autosubmit", "",
                                    "name", TYPE_PARAMETER,
                                    "data-searchable", true);
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("div", "class", "inputContainer");
                        page.writeStart("div", "class", "inputLabel");
                            page.writeStart("label", "for", page.createId());
                                page.writeHtml("Query");
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("div", "class", "inputSmall");
                            page.writeStart("textarea",
                                    "id", page.getId(),
                                    "name", PREDICATE_PARAMETER);
                                page.writeHtml(predicate);
                            page.writeEnd();
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("div", "class", "inputContainer");
                        page.writeStart("div", "class", "inputLabel");
                            page.writeStart("label", "for", page.createId());
                                page.writeHtml("Fields");
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("div", "class", "inputSmall");
                            page.writeStart("select",
                                    "name", FIELDS_PARAMETER,
                                    "multiple", "multiple");
                                for (ObjectField field : allFields) {
                                    if (field.as(ToolUi.class).isHidden()) {
                                        continue;
                                    }

                                    String fieldName = field.getInternalName();

                                    page.writeStart("option",
                                            "selected", fieldNames.contains(fieldName) ? "selected" : null,
                                            "value", fieldName);
                                        page.writeHtml(field.getDisplayName());
                                    page.writeEnd();
                                }
                            page.writeEnd();
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("div", "class", "buttons");
                        page.writeStart("button", "class", "action action-search");
                            page.writeHtml("Search");
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("h2");
                    page.writeHtml("Result");
                page.writeEnd();

                if (result == null) {
                    page.writeStart("div", "class", "message message-error");
                        page.writeStart("p");
                            page.writeHtml("Not a valid query! ");
                            page.writeHtml(predicate);
                        page.writeEnd();
                    page.writeEnd();

                } else if (!result.hasPages()) {
                    page.writeStart("div", "class", "message message-warning");
                        page.writeStart("p");
                            page.writeHtml("No matching items!");
                        page.writeEnd();
                    page.writeEnd();

                } else {
                    page.writeStart("form",
                            "class", "searchAdvancedResult",
                            "method", "post",
                            "action", page.url(null));
                        page.writeElement("input", "type", "hidden", "name", TYPE_PARAMETER, "value", type != null ? type.getId() : null);
                        page.writeElement("input", "type", "hidden", "name", PREDICATE_PARAMETER, "value", predicate);
                        page.writeElement("input", "type", "hidden", "name", "returnUrl", "value", page.url(""));

                        for (ObjectField field : fields) {
                            page.writeElement("input", "type", "hidden", "name", FIELDS_PARAMETER, "value", field.getInternalName());
                        }

                        page.writeStart("ul", "class", "pagination");
                            if (result.hasPrevious()) {
                                page.writeStart("li", "class", "next");
                                    page.writeStart("a",
                                            "href", page.url("", "offset", result.getPreviousOffset()));
                                        page.writeHtml("Previous ");
                                        page.writeHtml(result.getLimit());
                                    page.writeEnd();
                                page.writeEnd();
                            }

                            if (result.getOffset() > 0 ||
                                    result.hasNext() ||
                                    result.getItems().size() > LIMITS[0]) {
                                page.writeStart("li");
                                    for (String fieldName : fieldNames) {
                                        page.writeElement("input", "type", "hidden", "name", FIELDS_PARAMETER, "value", fieldName);
                                    }

                                    page.writeStart("select",
                                            "data-bsp-autosubmit", "",
                                            "name", "limit");
                                        for (int l : LIMITS) {
                                            page.writeStart("option",
                                                    "value", l,
                                                    "selected", limit == l ? "selected" : null);
                                                page.writeHtml("Show ");
                                                page.writeHtml(l == Integer.MAX_VALUE ? "All" : l);
                                            page.writeEnd();
                                        }
                                    page.writeEnd();
                                page.writeEnd();
                            }

                            page.writeStart("li", "class", "label");
                                page.writeHtml(result.getFirstItemIndex());
                                page.writeHtml(" to ");
                                page.writeHtml(result.getLastItemIndex());
                                page.writeHtml(" of ");
                                page.writeStart("strong");
                                    page.writeHtml(result.getCount());
                                page.writeEnd();
                            page.writeEnd();

                            if (result.hasNext()) {
                                page.writeStart("li", "class", "next");
                                    page.writeStart("a",
                                            "href", page.url("", "offset", result.getNextOffset()));
                                        page.writeHtml("Next ");
                                        page.writeHtml(result.getLimit());
                                    page.writeEnd();
                                page.writeEnd();
                            }
                        page.writeEnd();

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

                                    for (ObjectField field : fields) {
                                        page.writeStart("th");
                                            page.writeHtml(field.getDisplayName());
                                        page.writeEnd();
                                    }
                                page.writeEnd();
                            page.writeEnd();

                            page.writeStart("tbody");
                                for (Object item : result.getItems()) {
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
                                                    "href", page.objectUrl("/content/edit.jsp", item));
                                                page.writeObjectLabel(item);
                                            page.writeEnd();
                                        page.writeEnd();

                                        for (ObjectField field : fields) {
                                            page.writeStart("td");
                                                if ("cms.directory.paths".equals(field.getInternalName())) {
                                                    for (Directory.Path p : itemState.as(Directory.ObjectModification.class).getPaths()) {
                                                        String path = p.getPath();

                                                        page.writeStart("div");
                                                            page.writeStart("a", "href", path, "target", "_blank");
                                                                page.writeHtml(path);
                                                            page.writeEnd();

                                                            page.writeHtml(" (");
                                                            page.writeHtml(p.getType());
                                                            page.writeHtml(")");
                                                        page.writeEnd();
                                                    }

                                                } else {
                                                    for (Iterator<Object> i = CollectionUtils.recursiveIterable(itemState.getByPath(field.getInternalName())).iterator(); i.hasNext();) {
                                                        Object value = i.next();
                                                        page.writeObject(value);
                                                        if (i.hasNext()) {
                                                            page.writeHtml(", ");
                                                        }
                                                    }
                                                }
                                            page.writeEnd();
                                        }
                                    page.writeEnd();
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
                                        "formaction", page.cmsUrl("/contentEditBulk"));
                                    page.writeHtml("Bulk Edit All");
                                page.writeEnd();
                            }

                            page.writeStart("a",
                                    "class", "action button icon icon-object-workStream",
                                    "target", "workStreamCreate",
                                    "href", page.cmsUrl("/content/newWorkStream.jsp",
                                            "query", ObjectUtils.toJson(query.getState().getSimpleValues())));
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
            page.writeEnd();
        page.writeFooter();
    }

    private void writeCsvItem(ToolPageContext page, Object item) throws IOException {
        page.write(item.toString().replaceAll("\"", "\"\""));
    }
}
