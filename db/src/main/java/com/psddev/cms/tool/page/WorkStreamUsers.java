package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.WorkStream;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.widget.AbstractPaginatedResultWidget;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;

@RoutingFilter.Path(application = "cms", value = "/workStreamUsers")
@SuppressWarnings("serial")
public class WorkStreamUsers extends PageServlet {

    @Override
    protected String getPermissionId() {
        return "area/dashboard";
    }

    @Override
    protected void doService(final ToolPageContext page) throws IOException, ServletException {
        WorkStream workStream = Query.from(WorkStream.class).where("_id = ?", page.param(UUID.class, "id")).first();
        List<ToolUser> users = workStream.getUsers();

        Collections.sort(users);

        page.writeStart("div", "class", "widget");
            page.writeStart("h1", "class", "icon icon-object-workStream");
                // TODO: LOCALIZE
                page.writeHtml("Users Working On: ");
                page.writeObjectLabel(workStream);
            page.writeEnd();

            if (users.isEmpty()) {
                page.writeStart("div", "class", "message message-info");
                    page.writeStart("p");
                        page.writeHtml(page.localize(WorkStreamUsers.class, "message.noUsers"));
                    page.writeEnd();
                page.writeEnd();

            } else {

                page.writeStart("div",
                        "class", "tabbed",
                        "data-id", "workStreamUsers");

                    writeStatusTabHtml(page, workStream, users);
                    writeCompleteTabHtml(page);
                    writeSkippedTabHtml(page);

                page.writeEnd();
            }
        page.writeEnd();
    }

    private void writeStatusTabHtml(ToolPageContext page, WorkStream workStream, List<ToolUser> users) throws IOException {
        page.writeStart("div",
                "data-tab", "Status");

            page.writeStart("table", "class", "table-striped");
                page.writeStart("thead");
                    page.writeStart("tr");
                        page.writeStart("th");
                            page.writeHtml(page.localize(WorkStreamUsers.class, "label.user"));
                        page.writeEnd();
                        page.writeStart("th");
                            page.writeHtml(page.localize(WorkStreamUsers.class, "label.currentlyOn"));
                        page.writeEnd();
                        page.writeStart("th");
                            page.writeHtml(page.localize(WorkStreamUsers.class, "label.completed"));
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("tbody");
                    for (ToolUser user : users) {
                        page.writeStart("tr");
                            page.writeStart("td");
                                page.writeObjectLabel(user);
                            page.writeEnd();

                            page.writeStart("td");
                            Object currentItem = workStream.getCurrentItem(user);

                            if (currentItem == null) {
                                page.writeHtml("N/A");

                            } else {
                                page.writeStart("a",
                                        "href", page.objectUrl("/content/edit.jsp", currentItem),
                                        "target", "_top");
                                page.writeObjectLabel(currentItem);
                                page.writeEnd();
                            }
                            page.writeEnd();

                            page.writeStart("td");
                                page.writeHtml(workStream.countComplete(user));
                            page.writeEnd();
                        page.writeEnd();
                    }
                page.writeEnd();
            page.writeEnd();

        page.writeEnd();
    }

    private void writeCompleteTabHtml(ToolPageContext page) throws IOException, ServletException {
        page.writeStart("div",
                "data-tab", "Completed");

            new WorkStreamStatusView() {

                @Override
                public Query<Record> getQuery(ToolPageContext page) {
                    ToolUser user = getSelectedUser(page);

                    if (user == null) {
                        return null;
                    }

                    return Query.from(Record.class)
                            .resolveInvisible()
                            .where("cms.workstream.completeIds = ?", page.param(UUID.class, "id") + "," + user.getId().toString());
                }

            }.writeHtml(page, null);

        page.writeEnd();
    }

    private void writeSkippedTabHtml(ToolPageContext page) throws IOException, ServletException {
        page.writeStart("div",
                "data-tab", "Skipped");

            new WorkStreamStatusView() {

                @Override
                public Query<Record> getQuery(ToolPageContext page) {
                    WorkStream workStream = Query.from(WorkStream.class).where("_id = ?", page.param(UUID.class, "id")).first();
                    ToolUser user = getSelectedUser(page);

                    if (user == null) {
                        return null;
                    }

                    Map<String, List<UUID>> skippedMap = workStream.getSkippedItems();

                    if (ObjectUtils.isBlank(skippedMap)) {
                        return null;
                    }

                    List<UUID> objectIds = skippedMap.get(user.getId().toString());
                    if (objectIds == null) {
                        return null;
                    }

                    return Query.from(Record.class).where("_id = ?", objectIds).resolveInvisible();
                }

            }.writeHtml(page, null);

        page.writeEnd();
    }

    private abstract static class WorkStreamStatusView extends AbstractPaginatedResultWidget<Record> {

        private static final String USER_ID_PARAMETER = "userId";

        private transient ToolUser user;
        private transient WorkStream workStream;

        ToolUser getSelectedUser(ToolPageContext page) {
            if (user == null) {
                String userId = page.param(String.class, USER_ID_PARAMETER);

                if (StringUtils.isBlank(userId)) {
                    return page.getUser();
                }

                user = ObjectUtils.firstNonNull(Query.from(ToolUser.class).where("_id = ?", userId).first(), page.getUser());
            }

            return user;
        }

        WorkStream getWorkStream(ToolPageContext page) {
            if (workStream == null) {
                workStream = Query.from(WorkStream.class).where("_id = ?", page.param(String.class, "id")).first();
            }

            return workStream;
        }

        @Override
        public String getTitle(ToolPageContext page) throws IOException {
            return null;
        }

        @Override
        public String getUrl(ToolPageContext page, String path, Object... parameters) {
            List<Object> parameterList = new ArrayList<>(Arrays.asList(parameters));
            parameterList.add("id");
            parameterList.add(page.param(UUID.class, "id"));

            return page.url(path, parameterList.toArray());
        }

        @Override
        public void writeFiltersHtml(ToolPageContext page) throws IOException {

            WorkStream workStream = getWorkStream(page);

            if (workStream == null) {
                return;
            }

            List<ToolUser> users = workStream.getUsers();
            if (ObjectUtils.isBlank(users)) {
                return;
            }

            page.writeStart("select",
                    "name", USER_ID_PARAMETER,
                    "data-bsp-autosubmit", "",
                    "data-searchable", true);
                for (ToolUser user : users) {

                    page.writeStart("option",
                            "value", user.getId(),
                            "selected", user.equals(getSelectedUser(page)) ? "selected" : null);
                        page.writeHtml(user.getLabel());
                    page.writeEnd();

                }
            page.writeEnd();
        }
    }
}
