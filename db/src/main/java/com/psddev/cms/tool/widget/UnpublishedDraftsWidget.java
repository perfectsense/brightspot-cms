package com.psddev.cms.tool.widget;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.ToolRole;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.Workflow;
import com.psddev.cms.db.WorkflowState;
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DefaultDashboardWidget;
import com.psddev.cms.tool.QueryRestriction;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.QueryFilter;
import com.psddev.dari.db.State;
import com.psddev.dari.util.PaginatedResult;

public class UnpublishedDraftsWidget extends DefaultDashboardWidget {

    private static final int[] LIMITS = { 10, 20, 50 };

    @Override
    public int getColumnIndex() {
        return 1;
    }

    @Override
    public int getWidgetIndex() {
        return 3;
    }

    @Override
    public void writeHtml(ToolPageContext page, Dashboard dashboard) throws IOException, ServletException {
        Query<Workflow> workflowQuery = Query.from(Workflow.class);
        Map<String, String> workflowStateLabels = new TreeMap<>();

        workflowStateLabels.put("draft", "Draft");

        for (Workflow w : workflowQuery.iterable(0)) {
            for (WorkflowState s : w.getStates()) {
                workflowStateLabels.put("ws." + s.getName(), s.getDisplayName());
            }
        }

        String state = page.pageParam(String.class, "state", null);
        final ObjectType type = ObjectType.getInstance(page.pageParam(UUID.class, "typeId", null));
        Query<?> draftsQuery;

        if ("draft".equals(state)) {
            draftsQuery = Query
                    .from(Object.class)
                    .where("_type = ? or cms.content.draft = true", Draft.class);

        } else if (state != null && state.startsWith("ws.")) {
            draftsQuery = Query
                    .from(Object.class)
                    .where("cms.workflow.currentState = ?", state.substring(3));

        } else {
            draftsQuery = Query
                    .from(Object.class)
                    .where("_type = ? or cms.content.draft = true or cms.workflow.currentState != missing", Draft.class);
        }

        final UserType userType = page.pageParam(UserType.class, "userType", UserType.ANYONE);
        String userParameter = userType + ".value";
        final Object user = Query.from(Object.class).where("_id = ?", page.pageParam(UUID.class, userParameter, null)).first();
        QueryFilter<Object> queryFilter = null;

        if (type != null || userType != UserType.ANYONE) {
            queryFilter = item -> {
                State itemState = State.getInstance(item);
                boolean typeOk = true;
                boolean userOk = true;

                if (type != null) {
                    ObjectType itemType = item instanceof Draft ? ((Draft) item).getObjectType() : itemState.getType();
                    typeOk = itemType != null && itemType.getGroups().contains(type.getInternalName());
                }

                if (userType != UserType.ANYONE) {
                    ToolUser updateUser = itemState.as(Content.ObjectModification.class).getUpdateUser();

                    if (userType == UserType.ME) {
                        userOk = page.getUser().equals(updateUser);

                    } else if (user instanceof ToolUser) {
                        userOk = user.equals(updateUser);

                    } else if (user instanceof ToolRole && updateUser != null) {
                        userOk = user.equals(updateUser.getRole());
                    }
                }

                return typeOk && userOk;
            };
        }

        QueryRestriction.updateQueryUsingAll(draftsQuery, page);

        int limit = page.pageParam(int.class, "limit", 20);
        PaginatedResult<?> drafts = draftsQuery
                .and("* matches *")
                .and(Content.UPDATE_DATE_FIELD + " != missing")
                .and(page.siteItemsPredicate())
                .sortDescending(Content.UPDATE_DATE_FIELD)
                .selectFiltered(page.param(long.class, "offset"), limit, queryFilter);

        page.writeStart("div", "class", "widget widget-unpublishedDrafts");
            page.writeStart("h1", "class", "icon icon-object-draft");
                page.writeHtml("Unpublished Drafts");
            page.writeEnd();

            page.writeStart("div", "class", "widget-filters");
                for (Class<? extends QueryRestriction> c : QueryRestriction.classIterable()) {
                    page.writeQueryRestrictionForm(c);
                }

                page.writeStart("form",
                        "method", "get",
                        "action", page.url(null));

                    if (workflowStateLabels.size() > 1) {
                        page.writeStart("select",
                                "data-bsp-autosubmit", "",
                                "name", "state");
                            page.writeStart("option", "value", "");
                                page.writeHtml("Any Statuses");
                            page.writeEnd();

                            for (Map.Entry<String, String> entry : workflowStateLabels.entrySet()) {
                                String key = entry.getKey();

                                page.writeStart("option",
                                        "selected", key.equals(state) ? "selected" : null,
                                        "value", key);
                                    page.writeHtml(entry.getValue());
                                page.writeEnd();
                            }
                        page.writeEnd();
                    }

                    page.writeTypeSelect(
                            ObjectType.getInstance(Content.class).as(ToolUi.class).findDisplayTypes(),
                            type,
                            "Any Types",
                            "name", "typeId",
                            "data-bsp-autosubmit", "",
                            "data-searchable", true);

                    page.writeStart("select",
                            "data-bsp-autosubmit", "",
                            "name", "userType");
                        for (UserType t : UserType.values()) {
                            if (t != UserType.ROLE || Query.from(ToolRole.class).first() != null) {
                                page.writeStart("option",
                                        "selected", t.equals(userType) ? "selected" : null,
                                        "value", t.name());
                                    page.writeHtml(t.getDisplayName());
                                page.writeEnd();
                            }
                        }
                    page.writeEnd();

                    Query<?> userQuery;

                    if (userType == UserType.ROLE) {
                        userQuery = Query.from(ToolRole.class).sortAscending("name");

                    } else if (userType == UserType.USER) {
                        userQuery = Query.from(ToolUser.class).sortAscending("name");

                    } else {
                        userQuery = null;
                    }

                    if (userQuery != null) {
                        if (userQuery.hasMoreThan(250)) {
                            State userState = State.getInstance(user);

                            page.writeElement("input",
                                    "type", "text",
                                    "class", "objectId",
                                    "data-bsp-autosubmit", "",
                                    "data-editable", false,
                                    "data-label", userState != null ? userState.getLabel() : null,
                                    "data-typeIds", ObjectType.getInstance(ToolRole.class).getId(),
                                    "name", userParameter,
                                    "value", userState != null ? userState.getId() : null);

                        } else {
                            page.writeStart("select",
                                    "name", userParameter,
                                    "data-bsp-autosubmit", "",
                                    "data-searchable", "true");
                                page.writeStart("option", "value", "").writeEnd();

                                for (Object v : userQuery.selectAll()) {
                                    State userState = State.getInstance(v);

                                    page.writeStart("option",
                                            "value", userState.getId(),
                                            "selected", v.equals(user) ? "selected" : null);
                                        page.writeHtml(userState.getLabel());
                                    page.writeEnd();
                                }
                            page.writeEnd();
                        }
                    }
                page.writeEnd();
            page.writeEnd();

            if (drafts.getItems().isEmpty()) {
                String label = state != null ? workflowStateLabels.get(state) : null;

                page.writeStart("div", "class", "message message-info");
                    page.writeHtml("No ");
                    page.writeHtml(label != null ? label.toLowerCase(Locale.ENGLISH) : "matching");
                    page.writeHtml(" items.");
                page.writeEnd();

            } else {
                page.writeStart("ul", "class", "pagination");
                    if (drafts.hasPrevious()) {
                        page.writeStart("li", "class", "first");
                            page.writeStart("a", "href", page.url("", "offset", drafts.getFirstOffset()));
                                page.writeHtml("Newest");
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("li", "class", "previous");
                            page.writeStart("a", "href", page.url("", "offset", drafts.getPreviousOffset()));
                                page.writeHtml("Newer ");
                                page.writeHtml(drafts.getLimit());
                            page.writeEnd();
                        page.writeEnd();
                    }

                    page.writeStart("li");
                        page.writeStart("form",
                                "data-bsp-autosubmit", "",
                                "method", "get",
                                "action", page.url(null));
                            page.writeStart("select", "name", "limit");
                                for (int l : LIMITS) {
                                    page.writeStart("option",
                                            "value", l,
                                            "selected", limit == l ? "selected" : null);
                                        page.writeHtml("Show ");
                                        page.writeHtml(l);
                                    page.writeEnd();
                                }
                            page.writeEnd();
                        page.writeEnd();
                    page.writeEnd();

                    if (drafts.hasNext()) {
                        page.writeStart("li", "class", "next");
                            page.writeStart("a", "href", page.url("", "offset", drafts.getNextOffset()));
                                page.writeHtml("Older ");
                                page.writeHtml(drafts.getLimit());
                            page.writeEnd();
                        page.writeEnd();
                    }
                page.writeEnd();

                page.writeStart("table", "class", "links table-striped pageThumbnails");
                    page.writeStart("tbody");
                        for (Object item : drafts.getItems()) {
                            if (item instanceof Draft) {
                                Draft draft = (Draft) item;
                                item = draft.getObject();

                                if (item == null) {
                                    continue;
                                }

                                State itemState = State.getInstance(item);

                                if (!itemState.isVisible()
                                        && draft.getObjectChanges().isEmpty()) {
                                    continue;
                                }

                                UUID draftId = draft.getId();

                                page.writeStart("tr", "data-preview-url", "/_preview?_cms.db.previewId=" + draftId);
                                    page.writeStart("td");
                                        page.writeStart("span", "class", "visibilityLabel");
                                            page.writeHtml("Update");
                                        page.writeEnd();
                                    page.writeEnd();

                                    page.writeStart("td");
                                        page.writeHtml(page.getTypeLabel(item));
                                    page.writeEnd();

                                    page.writeStart("td", "data-preview-anchor", "");
                                        page.writeStart("a",
                                                "target", "_top",
                                                "href", page.url("/content/edit.jsp",
                                                        "id", itemState.getId(),
                                                        "draftId", draftId));
                                            page.writeObjectLabel(itemState);
                                        page.writeEnd();
                                    page.writeEnd();

                                    page.writeStart("td");
                                        page.writeObjectLabel(draft.as(Content.ObjectModification.class).getUpdateUser());
                                    page.writeEnd();
                                page.writeEnd();

                            } else {
                                State itemState = State.getInstance(item);
                                UUID itemId = itemState.getId();

                                page.writeStart("tr", "data-preview-url", "/_preview?_cms.db.previewId=" + itemId);
                                    page.writeStart("td");
                                        page.writeStart("span", "class", "visibilityLabel");
                                            page.writeHtml(itemState.getVisibilityLabel());
                                        page.writeEnd();
                                    page.writeEnd();

                                    page.writeStart("td");
                                        page.writeHtml(page.getTypeLabel(item));
                                    page.writeEnd();

                                    page.writeStart("td", "data-preview-anchor", "");
                                        page.writeStart("a", "href", page.url("/content/edit.jsp", "id", itemId), "target", "_top");
                                            page.writeObjectLabel(itemState);
                                        page.writeEnd();
                                    page.writeEnd();

                                    page.writeStart("td");
                                        page.writeObjectLabel(itemState.as(Content.ObjectModification.class).getUpdateUser());
                                    page.writeEnd();
                                page.writeEnd();
                            }
                        }
                    page.writeEnd();
                page.writeEnd();
            }
        page.writeEnd();
    }

    private enum UserType {

        ANYONE("Anyone"),
        ME("Me"),
        ROLE("Role"),
        USER("User");

        private String displayName;

        private UserType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

}
