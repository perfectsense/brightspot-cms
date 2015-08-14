package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.History;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "/historyEdit")
@SuppressWarnings("serial")
public class HistoryEdit extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(final ToolPageContext page) throws IOException, ServletException {
        History history = Query
                .from(History.class)
                .where("_id = ?", page.param(UUID.class, "id"))
                .first();

        if (page.isFormPost()) {
            try {
                history.setName(page.param(String.class, "name"));
                history.save();

                page.writeStart("script", "type", "text/javascript");
                    page.write("window.location = window.location;");
                page.writeEnd();

                return;

            } catch (Exception error) {
                page.getErrors().add(error);
            }
        }

        page.writeHeader();
            page.writeStart("div", "class", "widget");
                page.writeStart("h1", "class", "icon icon-object-history");
                    page.writeHtml("Name Revision");
                page.writeEnd();

                page.include("/WEB-INF/errors.jsp");

                page.writeStart("form",
                        "method", "post",
                        "action", page.url(""));
                    page.writeStart("div", "class", "inputContainer");
                        page.writeStart("div", "class", "inputLabel");
                            page.writeStart("label", "for", page.createId()).writeHtml("Name").writeEnd();
                        page.writeEnd();

                        page.writeStart("div", "class", "inputSmall");
                            page.writeElement("input",
                                    "type", "text",
                                    "id", page.getId(),
                                    "name", "name",
                                    "value", history.getName());
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("div", "class", "actions");
                        page.writeStart("button",
                                "class", "icon icon-action-save",
                                "name", "action-save",
                                "value", "true");
                            page.writeHtml("Save");
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }
}
