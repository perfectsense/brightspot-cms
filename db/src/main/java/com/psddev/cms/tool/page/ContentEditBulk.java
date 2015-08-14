package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.psddev.cms.tool.SearchResultSelectionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "contentEditBulk")
public class ContentEditBulk extends PageServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentEditBulk.class);

    public static final List<Operation> COLLECTION_OPERATIONS = ImmutableList.of(Operation.REPLACE, Operation.ADD, Operation.REMOVE, Operation.CLEAR);
    public static final List<Operation> NON_COLLECTION_OPERATIONS = ImmutableList.of(Operation.REPLACE, Operation.CLEAR);
    public static final String OPERATION_PARAMETER_PREFIX = "contentEditBulk.op/";

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        List<UUID> ids = page.params(UUID.class, ContentSearchAdvanced.ITEMS_PARAMETER);
        UUID selectionId = page.param(UUID.class, "selectionId");
        Query<?> query;

        if (selectionId != null) {
            Set<UUID> itemIds = new HashSet<>();

            for (SearchResultSelectionItem item : Query
                    .from(SearchResultSelectionItem.class)
                    .where("selectionId = ?", selectionId)
                    .selectAll()) {

                itemIds.add(item.getItemId());
            }

            query = Query.fromAll().where("_id = ?", itemIds);

        } else if (ids.isEmpty()) {
            String searchString = page.param(String.class, "search");
            Search search;

            if (searchString != null) {
                search = new Search();

                search.getState().putAll((Map<String, Object>) ObjectUtils.fromJson(searchString));

            } else {
                search = new Search(page);
            }

            query = search.toQuery(page.getSite());

        } else {
            query = Query.fromAll().where("_id = ?", ids);
        }

        long count = query.count();
        ObjectType type = ObjectType.getInstance(page.param(UUID.class, "typeId"));
        State state = State.getInstance(type.createObject(page.param(UUID.class, "id")));

        state.clear();

        Exception error = null;

        if (page.isFormPost()
                && page.param(String.class, "action-save") != null) {
            try {
                JspUtils.include(
                        page.getRequest(),
                        page.getResponse(),
                        page,
                        page.toolPath(CmsTool.class, "/WEB-INF/objectPost.jsp"),
                        "object", state.getOriginalObject());

                Map<String, Object> values = state.getValues();
                Map<String, Object> replaces = new CompactMap<>();
                Map<String, Object> adds = new CompactMap<>();
                Map<String, Object> removes = new CompactMap<>();
                Set<String> clears = new LinkedHashSet<>();

                for (ObjectField field : type.getFields()) {
                    String name = field.getInternalName();
                    Object value = values.get(name);

                    Operation op = page.param(Operation.class, OPERATION_PARAMETER_PREFIX + name);

                    if (op != null) {
                        if (Operation.REPLACE.equals(op)) {
                            replaces.put(name, value);

                        } else if (Operation.ADD.equals(op)) {
                            adds.put(name, value);

                        } else if (Operation.REMOVE.equals(op)) {
                            removes.put(name, value);

                        } else if (Operation.CLEAR.equals(op)) {
                            clears.add(name);
                        }
                    }
                }

                ContentEditBulkSubmission status = new ContentEditBulkSubmission();

                status.setSubmitSite(page.getSite());
                status.setSubmitUser(page.getUser());
                status.setSubmitDate(new Date());
                status.setQuery(query);
                status.setCount(count);
                status.setReplaces(replaces);
                status.setAdds(adds);
                status.setRemoves(removes);
                status.setClears(clears);
                status.submitTask();

                page.getResponse().sendRedirect(page.cmsUrl(
                        "/contentEditBulkSubmissionStatus",
                        "id", status.getId(),
                        "returnUrl", page.param(String.class, "returnUrl")));
                return;

            } catch (Exception e) {
                error = e;
            }
        }

        page.writeHeader();

            page.writeStart("div", "class", "widget");
                page.writeStart("h1");
                    page.writeHtml("Bulk Edit ");
                    page.writeHtml(count);
                    page.writeHtml(" Items");
                page.writeEnd();

                page.writeObject(error);

                String formId = page.createId();

                page.writeStart("form",
                        "id", formId,
                        "method", "post",
                        "action", page.url(null, "id", state.getId()));

                    for (String paramName : page.paramNamesList()) {
                        if ("id".equals(paramName)
                                || paramName.startsWith(OPERATION_PARAMETER_PREFIX)
                                || paramName.startsWith(state.getId() + "/")
                                || paramName.startsWith("action-")) {
                            continue;
                        }

                        for (String value : page.params(String.class, paramName)) {
                            page.writeElement("input",
                                    "type", "hidden",
                                    "name", paramName,
                                    "value", value);
                        }
                    }

                    HttpServletRequest request = page.getRequest();
                    Object oldId = request.getAttribute("bsp.contentEditBulk.id");

                    try {
                        request.setAttribute("bsp.contentEditBulk.id", state.getId());
                        JspUtils.include(
                                request,
                                page.getResponse(),
                                page,
                                page.toolPath(CmsTool.class, "/WEB-INF/objectForm.jsp"),
                                "object", state.getOriginalObject());

                    } finally {
                        request.setAttribute("bsp.contentEditBulk.id", oldId);
                    }

                    page.writeStart("div", "class", "actions");
                        page.writeStart("button",
                                "class", "action icon icon-action-save",
                                "name", "action-save",
                                "value", "true");
                            page.writeHtml("Bulk Save");
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }

    public enum Operation {

        REPLACE("Replace"),
        ADD("Add"),
        REMOVE("Remove"),
        CLEAR("Clear");

        private final String label;

        private Operation(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
