package com.psddev.cms.tool.widget;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.ToolRole;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.WorkStream;
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DefaultDashboardWidget;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.PaginatedResult;

public class WorkStreamsWidget extends DefaultDashboardWidget {
    private static final int[] LIMITS = { 10, 20, 50 };
    private static final String TOOL_ENTITY_TYPE_PARAMETER = "toolEntityType";
    private static final String TOOL_ENTITY_VALUE_PARAMETER = "toolEntity";
    private static final String OFFSET_PARAMETER = "offset";
    private static final String LIMIT_PARAMETER = "limit";
    private static final String STOP_PARAMETER = "stop";

    @Override
    public int getColumnIndex() {
        return 0;
    }

    @Override
    public int getWidgetIndex() {
        return 1;
    }

    @Override
    public void writeHtml(ToolPageContext page, Dashboard dashboard) throws IOException, ServletException {
        int limit = page.pageParam(Integer.class, LIMIT_PARAMETER, LIMITS[0]);
        UUID stop = page.param(UUID.class, STOP_PARAMETER);

        ToolUser user = page.getUser();

        // Actions
        if (stop != null) {
            WorkStream toStop = Query.from(WorkStream.class).where("id = ?", stop).first();
            if (toStop != null) {
                toStop.stop(user);
            }
            page.redirect(null);
            return;
        }

        PaginatedResult<WorkStream> results = getResults(page);

        page.writeHeader();
        page.writeStart("div", "class", "widget p-workStreams");
            page.writeStart("h1", "class", "icon icon-object-workStream");
                page.writeHtml(page.localize(WorkStreamsWidget.class, "title"));
            page.writeEnd();

            writeFilters(page);

        if (!results.hasPages()) { //workStreams.isEmpty()) {
            page.writeStart("div", "class", "message message-info");
                page.writeHtml(page.localize(WorkStreamsWidget.class, "message.noWorkStreams"));
            page.writeEnd();

        } else {
            writePaginationHtml(page, results, limit);

            // Results
            List<WorkStream> workStreams = results.getItems();

            for (WorkStream workStream : workStreams) {
                List<ToolUser> users = workStream.getUsers();
                long skipped = workStream.countSkipped(user);
                long complete = workStream.countComplete();
                long incomplete = workStream.countIncomplete() - skipped;
                long total = complete + incomplete + skipped;
                boolean working = workStream.isWorking(user);

                page.writeStart("div",
                        "class", "block " + (working ? "p-workStreams-working" : "p-workStreams-notWorking"),
                        "style", page.cssString(
                                "padding-right", working ? "165px" : "75px",
                                "position", "relative"));

                //TODO: LOCALIZE
                if (users.isEmpty()) {
                    page.writeHtml("No users");

                } else {
                    page.writeStart("a",
                            "href", page.url("/workStreamUsers", "id", workStream.getId()),
                            "target", "workStream");
                        page.writeHtml(users.size());
                        page.writeHtml(" users");
                    page.writeEnd();
                }

                page.writeHtml(" working on ");

                page.writeStart("a",
                        "href", page.objectUrl("/content/editWorkStream", workStream, "reload", true),
                        "target", "workStream");
                    page.writeObjectLabel(workStream);
                page.writeEnd();

                if (working) {
                    page.writeStart("a",
                            "class", "button p-workStreams-continue",
                            "href", page.url("/content/edit.jsp", "workStreamId", workStream.getId(), "_", System.currentTimeMillis()),
                            "target", "_top",
                            "style", page.cssString(
                                    "bottom", 0,
                                    "position", "absolute",
                                    "right", "70px",
                                    "text-align", "center",
                                    "width", "90px"));
                        page.writeHtml(page.localize(WorkStreamsWidget.class, "action.continue"));
                    page.writeEnd();

                    page.writeStart("a",
                            "class", "button p-workStreams-stop",
                            "href", page.url("", "stop", workStream.getId()),
                            "style", page.cssString(
                                    "bottom", 0,
                                    "position", "absolute",
                                    "right", 0,
                                    "text-align", "center",
                                    "width", "65px"));
                        page.writeHtml(page.localize(WorkStreamsWidget.class, "action.stop"));
                    page.writeEnd();

                } else {
                    page.writeStart("a",
                            "class", "button p-workStreams-start",
                            "href", page.url("/content/edit.jsp", "workStreamId", workStream.getId(), "_", System.currentTimeMillis()),
                            "target", "_top",
                            "style", page.cssString(
                                    "bottom", 0,
                                    "position", "absolute",
                                    "right", 0,
                                    "text-align", "center",
                                    "width", "70px"));
                        page.writeHtml(page.localize(WorkStreamsWidget.class, "action.start"));
                    page.writeEnd();
                }

                page.writeStart("div", "class", "progress");
                    page.writeStart("div", "class", "progressBar", "style", "width:" + ((total - incomplete) * 100.0 / total) + "%");
                    page.writeEnd();

                    //TODO: LOCALIZE
                    page.writeStart("strong");
                        page.writeHtml(incomplete);
                    page.writeEnd();

                    page.writeHtml(" of ");

                    page.writeStart("strong");
                        page.writeHtml(total);
                    page.writeEnd();

                    page.writeHtml(" left ");

                    if (complete > 0L || skipped > 0L) {
                        page.writeHtml("(");
                    }

                    if (complete > 0L) {
                        page.writeStart("strong");
                            page.writeHtml(complete);
                        page.writeEnd();

                        page.writeHtml(" complete");

                        if (skipped > 0L) {
                            page.writeHtml(", ");
                        }
                    }

                    if (skipped > 0L) {
                        page.writeStart("strong");
                            page.writeHtml(skipped);
                        page.writeEnd();

                        page.writeHtml(" skipped");
                    }

                    if (complete > 0L || skipped > 0L) {
                        page.writeHtml(")");
                    }

                    page.writeEnd();
                page.writeEnd();
            }
        }
        page.writeEnd();
        page.writeFooter();
    }

    private PaginatedResult<WorkStream> getResults(ToolPageContext page) {

        Query<WorkStream> query = Query.from(WorkStream.class).where(page.siteItemsPredicate());

        ToolEntityType entityType = page.pageParam(ToolEntityType.class, TOOL_ENTITY_TYPE_PARAMETER, ToolEntityType.ANYONE);

        UUID entityId = null;

        if (entityType == ToolEntityType.USER || entityType == ToolEntityType.ROLE) {
            entityId = page.pageParam(UUID.class, TOOL_ENTITY_VALUE_PARAMETER, null);
        } else if (entityType == ToolEntityType.ME) {
            entityId = page.getUser().getId();
        }

        if (entityId != null) {
            query.and("assignedEntities = ?", entityId);
        }

        return query.select(page.param(long.class, OFFSET_PARAMETER), page.paramOrDefault(int.class, LIMIT_PARAMETER, LIMITS[0]));
    }

    private void writeFilters(ToolPageContext page) throws IOException {
        page.writeStart("div", "class", "widget-filters");
            page.writeStart("form",
                    "method", "get",
                    "action", page.url(null));
                page.writeStart("select",
                        "data-bsp-autosubmit", "",
                        "name", TOOL_ENTITY_TYPE_PARAMETER,
                        "data-searchable", "true");

                    ToolEntityType userType = page.pageParam(ToolEntityType.class, TOOL_ENTITY_TYPE_PARAMETER, ToolEntityType.ANYONE);
                    for (ToolEntityType t : ToolEntityType.values()) {
                        if (t != ToolEntityType.ROLE || Query.from(ToolRole.class).first() != null) {
                            page.writeStart("option",
                                    "selected", t.equals(userType) ? "selected" : null,
                                    "value", t.name());
                            page.writeHtml(page.localize(null, t.getResourceKey()));
                            page.writeEnd();
                        }
                    }

                page.writeEnd();

                // TODO: move somewhere reusable (duplicated in other widgets)
                ObjectType toolEntityType;
                Query<?> toolEntityQuery;

                if (userType == ToolEntityType.ROLE) {
                    toolEntityType = ObjectType.getInstance(ToolRole.class);
                    toolEntityQuery = Query.from(ToolRole.class).sortAscending("name");

                } else if (userType == ToolEntityType.USER) {
                    toolEntityType = ObjectType.getInstance(ToolUser.class);
                    toolEntityQuery = Query.from(ToolUser.class).sortAscending("name");

                } else {
                    toolEntityType = null;
                    toolEntityQuery = null;
                }

                if (toolEntityQuery != null) {
                    Object toolEntity = Query.from(Object.class).where("_id = ?", page.pageParam(UUID.class, TOOL_ENTITY_VALUE_PARAMETER, null)).first();
                    if (toolEntityQuery.hasMoreThan(250)) {
                        State toolEntityState = State.getInstance(toolEntity);

                        page.writeElement("input",
                                "type", "text",
                                "class", "objectId",
                                "data-bsp-autosubmit", "",
                                "data-editable", false,
                                "data-label", toolEntityState != null ? toolEntityState.getLabel() : null,
                                "data-typeIds", toolEntityType != null ? toolEntityType.getId() : null,
                                "name", TOOL_ENTITY_VALUE_PARAMETER,
                                "value", toolEntityState != null ? toolEntityState.getId() : null);

                    } else {
                        page.writeStart("select",
                                "name", TOOL_ENTITY_VALUE_PARAMETER,
                                "data-bsp-autosubmit", "",
                                "data-searchable", "true");
                            page.writeStart("option", "value", "").writeEnd();
                            for (Object v : toolEntityQuery.selectAll()) {
                                State userState = State.getInstance(v);

                                page.writeStart("option",
                                        "value", userState.getId(),
                                        "selected", v.equals(toolEntity) ? "selected" : null);
                                page.writeHtml(userState.getLabel());
                                page.writeEnd();
                            }
                        page.writeEnd();
                    }
                }
            page.writeEnd();
        page.writeEnd();
    }

    private void writePaginationHtml(ToolPageContext page, PaginatedResult<WorkStream> results, int limit) throws IOException {

        // Pagination
        page.writeStart("ul", "class", "pagination");

            if (results.hasPrevious()) {
                page.writeStart("li", "class", "first");
                    page.writeStart("a", "href", page.url("", OFFSET_PARAMETER, results.getFirstOffset()));
                        page.writeHtml(page.localize(WorkStreamsWidget.class, "pagination.newest"));
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("li", "class", "previous");
                    page.writeStart("a", "href", page.url("", OFFSET_PARAMETER, results.getPreviousOffset()));
                        page.writeHtml(page.localize(ImmutableMap.of("count", limit), "pagination.newerCount"));
                    page.writeEnd();
                page.writeEnd();
            }

            if (results.getOffset() > 0 || results.hasNext() || results.getItems().size() > LIMITS[0]) {
                page.writeStart("li");
                    page.writeStart("form",
                            "data-bsp-autosubmit", "",
                            "method", "get",
                            "action", page.url(null));
                        page.writeStart("select", "name", LIMIT_PARAMETER);
                        for (int l : LIMITS) {
                            page.writeStart("option",
                                    "value", l,
                                    "selected", limit == l ? "selected" : null);
                                page.writeHtml(page.localize(WorkStreamsWidget.class, ImmutableMap.of("count", l), "option.showCount"));
                            page.writeEnd();
                        }
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();
            }

            if (results.hasNext()) {
                page.writeStart("li", "class", "next");
                    page.writeStart("a", "href", page.url("", "offset", results.getNextOffset()));
                        page.writeHtml(page.localize(WorkStreamsWidget.class, ImmutableMap.of("count", limit), "pagination.olderCount"));
                    page.writeEnd();
                page.writeEnd();
            }

        page.writeEnd();

    }

    private enum ToolEntityType {

        ANYONE("label.anyone"),
        ME("label.me"),
        ROLE("label.role"),
        USER("label.user");

        private String resourceKey;

        ToolEntityType(String resourceKey) {
            this.resourceKey = resourceKey;
        }

        public String getResourceKey() {
            return resourceKey;
        }
    }
}
