package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.WorkflowLog;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "workflowLogs")
@SuppressWarnings("serial")
public class WorkflowLogs extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(final ToolPageContext page) throws IOException, ServletException {
        List<WorkflowLog> logs = Query
                .from(WorkflowLog.class)
                .where("objectId = ?", page.param(UUID.class, "objectId"))
                .sortDescending("date")
                .selectAll();

        page.writeHeader();
            page.writeStart("div", "class", "widget");
                page.writeStart("h1", "class", "icon icon-object-workflow").writeHtml("Workflow Logs").writeEnd();

                if (!logs.isEmpty()) {
                    page.writeStart("table", "class", "table-striped");
                        page.writeStart("thead");
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.writeHtml("State");
                                page.writeEnd();

                                page.writeStart("th");
                                    page.writeHtml("Comment");
                                page.writeEnd();

                                page.writeStart("th");
                                    page.writeHtml("User");
                                page.writeEnd();

                                page.writeStart("th");
                                    page.writeHtml("Time");
                                page.writeEnd();
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("tbody");
                            for (WorkflowLog log : logs) {
                                page.writeStart("tr");
                                    page.writeStart("td");
                                        page.writeHtml(log.getNewWorkflowState());
                                    page.writeEnd();

                                    page.writeStart("td");
                                        page.writeHtml(log.getComment());
                                    page.writeEnd();

                                    page.writeStart("td");
                                        page.writeHtml(log.getUserName());
                                    page.writeEnd();

                                    page.writeStart("td");
                                        page.writeHtml(page.formatUserDateTime(log.getDate()));
                                    page.writeEnd();
                                page.writeEnd();
                            }
                        page.writeEnd();
                    page.writeEnd();
                }
            page.writeEnd();
        page.writeFooter();
    }
}
