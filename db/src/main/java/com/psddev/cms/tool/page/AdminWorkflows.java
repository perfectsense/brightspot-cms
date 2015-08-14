package com.psddev.cms.tool.page;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.cms.db.Workflow;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "/admin/workflows.jsp")
@SuppressWarnings("serial")
public class AdminWorkflows extends PageServlet {

    @Override
    protected String getPermissionId() {
        return "area/admin/adminWorkflows";
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Object selected = page.findOrReserve(Workflow.class);
        State selectedState = State.getInstance(selected);

        if (page.tryStandardUpdate(selected)) {
            return;
        }

        page.writeHeader();
            page.writeStart("div", "class", "withLeftNav");
                page.writeStart("div", "class", "leftNav");
                    page.writeStart("div", "class", "widget");
                        page.writeStart("h1", "class", "icon icon-object-workflow");
                            page.writeHtml("Workflows");
                        page.writeEnd();

                        page.writeStart("ul", "class", "links");
                            page.writeStart("li", "class", "new " + (selectedState.isNew() ? "selected" : ""));
                                page.writeStart("a", "href", page.url(null));
                                    page.writeHtml("New Workflow");
                                page.writeEnd();
                            page.writeEnd();

                            for (Workflow workflow : Query
                                    .from(Workflow.class)
                                    .sortAscending("name")
                                    .selectAll()) {
                                page.writeStart("li", "class", workflow.equals(selected) ? "selected" : null);
                                    page.writeStart("a", "href", page.objectUrl(null, workflow));
                                        page.writeObjectLabel(workflow);
                                    page.writeEnd();
                                page.writeEnd();
                            }
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("div", "class", "main");
                    page.writeStart("div", "class", "widget");
                        page.writeStandardForm(selected);
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }
}
