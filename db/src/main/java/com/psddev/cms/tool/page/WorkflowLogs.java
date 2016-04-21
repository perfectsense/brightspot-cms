package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.Site;
import com.psddev.cms.db.Workflow;
import com.psddev.cms.db.WorkflowLog;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
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
        Object obj = Query.findById(Object.class, page.param(UUID.class, "objectId"));
        State state = State.getInstance(obj);
        Workflow workflow = Workflow.findWorkflow(state.as(Site.ObjectModification.class).getOwner(), state);
        List<WorkflowLog> logs = Query
                .from(WorkflowLog.class)
                .where("objectId = ?", page.param(UUID.class, "objectId"))
                .sortDescending("date")
                .selectAll();

        page.writeHeader();
            page.writeStart("div", "class", "widget");
                page.writeStart("h1", "class", "icon icon-object-workflow");
                    page.writeHtml(page.localize(WorkflowLogs.class, "title"));
                page.writeEnd();

                if (!logs.isEmpty()) {
                    page.writeStart("table", "class", "table-striped");
                        page.writeStart("thead");
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.writeHtml(page.localize(WorkflowLogs.class, "label.state"));
                                page.writeEnd();

                                page.writeStart("th");
                                    page.writeHtml(page.localize(WorkflowLogs.class, "label.comment"));
                                page.writeEnd();

                                page.writeStart("th");
                                    page.writeHtml(page.localize(WorkflowLogs.class, "label.user"));
                                page.writeEnd();

                                page.writeStart("th");
                                    page.writeHtml(page.localize(WorkflowLogs.class, "label.time"));
                                page.writeEnd();
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("tbody");
                            for (WorkflowLog log : logs) {
                                page.writeStart("tr");
                                    page.writeStart("td");
                                        page.writeHtml(workflow == null ? log.getNewWorkflowState() : workflow.getStateDisplayName(log.getNewWorkflowState()));
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
