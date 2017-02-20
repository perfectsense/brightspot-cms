package com.psddev.cms.tool.page;

import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.RoutingFilter;

import javax.servlet.ServletException;
import java.io.IOException;

@RoutingFilter.Path(application = "cms", value = "toolUserSaveSelection")
public class ToolUserSaveSelection extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {

        String selectionId = page.param(String.class, "selectionId");

        if (selectionId == null) {
            throw new IllegalArgumentException("[selectionId] is required!");
        }

        SearchResultSelection selection = (SearchResultSelection) Query.fromAll().where("_id = ?", selectionId).first();

        if (selection == null) {
            throw new IllegalArgumentException("Could not find a Collection/SearchResultSelection for selectionId " + selectionId);
        }

        if (page.isFormPost()) {

            page.updateUsingParameters(selection);
            selection.save();

            page.writeStart("div", "id", page.createId());
            page.writeEnd();
            page.writeStart("script", "type", "text/javascript");
                page.writeRaw("$('#").writeRaw(page.getId()).writeRaw("').popup('close');");
                page.writeRaw("$('#").writeRaw(page.getId()).writeRaw("').popup('source').closest('.frame').find('.reload').click();");
            page.writeEnd();
            return;
        }

        page.writeHeader();
            page.writeStart("div", "class", "widget");
                page.writeStart("h1", "class", "icon icon-action-search");
                    page.writeHtml(page.localize(ToolUserSaveSelection.class, "title"));
                page.writeEnd();

            page.writeStart("form",
                "method", "post",
                "action", page.url(""));

                page.writeFormFields(selection);

                page.writeStart("div", "class", "actions");
                    page.writeStart("button",
                        "class", "action icon icon-action-save");
                        page.writeHtml(page.localize(ToolUserSaveSelection.class, "action.save"));
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }
}
