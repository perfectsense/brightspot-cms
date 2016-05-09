package com.psddev.cms.tool.page;

import java.io.IOException;

import javax.servlet.ServletException;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "toolUserSaveSearch")
public class ToolUserSaveSearch extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        if (page.isFormPost()) {
            ToolUser user = page.getUser();
            String name = page.param(String.class, "name");

            if (ObjectUtils.isBlank(name)) {
                throw new IllegalArgumentException(
                        page.localize(
                                ImmutableMap.of("displayName", page.localize(ToolUserSaveSearch.class, "label.name")),
                                "error.required"));
            }

            String search = page.param(String.class, "search");
            int questionAt = search.indexOf('?');

            if (questionAt > -1) {
                search = search.substring(questionAt + 1);
            }

            user.getSavedSearches().put(name, search);
            user.save();

            page.writeStart("div", "id", page.createId());
            page.writeEnd();
            page.writeStart("script", "type", "text/javascript");
                page.writeRaw("$('#").writeRaw(page.getId()).writeRaw("').popup('close');");
                page.writeRaw("$('.frame[name=\"savedSearches\"]').find('form[name=\"refreshSavedSearches\"]').submit();");
            page.writeEnd();
            return;
        }

        page.writeHeader();
            page.writeStart("div", "class", "widget");
                page.writeStart("h1", "class", "icon icon-action-search");
                    page.writeHtml(page.localize(ToolUserSaveSearch.class, "title"));
                page.writeEnd();

                page.writeStart("form",
                        "method", "post",
                        "action", page.url(""));
                    page.writeStart("div", "class", "inputContainer");
                        page.writeStart("div", "class", "inputLabel");
                            page.writeStart("label");
                                page.writeHtml(page.localize(ToolUserSaveSearch.class, "label.name"));
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("div", "class", "inputSmall");
                            page.writeElement("input",
                                    "type", "text",
                                    "name", "name",
                                    "placeholder", page.localize(ToolUserSaveSearch.class, "placeholder.required"));
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("div", "class", "actions");
                        page.writeStart("button",
                                "class", "action icon icon-action-save");
                            page.writeHtml(page.localize(ToolUserSaveSearch.class, "action.save"));
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }
}
