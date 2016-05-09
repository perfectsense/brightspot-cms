package com.psddev.cms.tool.page.content.edit;

import com.google.common.base.Preconditions;
import com.psddev.cms.db.Draft;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;

@RoutingFilter.Path(application = "cms", value = "/content/edit/new-draft")
public class NewDraft extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        State state = State.getInstance(Preconditions.checkNotNull(Query
                .fromAll()
                .where("_id = ?", page.param(UUID.class, "id"))
                .first()));

        String name = page.param(String.class, "name");
        boolean error = false;

        if (page.isFormPost()) {
            if (!ObjectUtils.isBlank(name)) {
                Draft draft = new Draft();

                draft.setName(name);
                draft.setOwner(page.getUser());
                draft.setObjectType(state.getType());
                draft.setObjectId(state.getId());
                page.publish(draft);

                page.writeStart("script", "type", "text/javascript");
                    page.writeRaw("window.location = '");
                    page.writeRaw(StringUtils.escapeJavaScript(page.cmsUrl(
                            "/content/edit.jsp",
                            "id", state.getId(),
                            "draftId", draft.getId())));
                    page.writeRaw("';");
                page.writeEnd();
                return;

            } else {
                error = true;
            }
        }

        page.writeHeader();
            page.writeStart("div", "class", "widget");
                page.writeStart("h1");
                    page.writeHtml(page.localize(Draft.class, "action.newType"));
                page.writeEnd();

                if (error) {
                    page.writeStart("div", "class", "message message-error");
                        page.writeHtml("Name is required!");
                    page.writeEnd();
                }

                page.writeStart("form", "method", "post", "action", page.url(""));
                    page.writeStart("div", "class", "inputContainer");
                        page.writeStart("div", "class", "inputLabel");
                            page.writeStart("label", "for", page.createId());
                                page.writeHtml("Name");
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("div", "class", "inputSmall");
                            page.writeElement("input",
                                    "type", "text",
                                    "id", page.getId(),
                                    "name", "name",
                                    "placeholder", "(Required)",
                                    "value", name);
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("div", "class", "actions");
                        page.writeStart("button");
                            page.writeHtml("Create");
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }
}
