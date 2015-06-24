package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletException;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.ToolPageContext;

public class ProfileTab extends ProfileWidgetTab {

    @Override
    public void writeHtml(ToolPageContext page) throws IOException, ServletException {

        String tab = page.param(String.class, "tab");
        ToolUser user = page.getUser();

        Collection<String> excludeFields = new ArrayList<String>();

        excludeFields.add("role");
        excludeFields.add("changePasswordOnLogIn");
        excludeFields.add("tfaRequired");

        if (user.isExternal()) {
            excludeFields.add("password");
        }

        if ("profile".equals(tab) && page.isFormPost()) {
            try {
                page.include("/WEB-INF/objectPost.jsp", "object", user, "excludeFields", excludeFields);
                user.save();

                page.writeStart("script", "type", "text/javascript");
                    page.writeRaw("window.top.window.location = window.top.window.location;");
                page.writeEnd();
                return;

            } catch (Exception ex) {
                page.getErrors().add(ex);
            }
        }

        page.writeStart("div",
                "class", "p-tud-profile",
                "data-tab", "Profile");

            page.writeStart("ul", "class", "piped");
                page.writeStart("li");
                    page.writeStart("a",
                            "class", "icon icon-key",
                            "href", page.cmsUrl("/toolUserTfa"),
                            "target", "toolUserTfa");
                        page.writeHtml(user.isTfaEnabled() ? "Disable" : "Enable");
                        page.writeHtml(" Two Factor Authentication");
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();

            page.include("/WEB-INF/errors.jsp");

            page.writeStart("form",
                    "method", "post",
                    "enctype", "multipart/form-data",
                    "action", page.objectUrl("", user));

                page.writeElement("input",
                        "type", "hidden",
                        "name", "tab",
                        "value", "profile");

                page.writeStart("div", "class", "fixedScrollable");
                    page.include("/WEB-INF/objectForm.jsp", "object", user, "excludeFields", excludeFields);
                page.writeEnd();

                page.writeStart("div", "class", "actions");
                    page.writeStart("button", "class", "icon icon-action-save");
                        page.writeHtml("Save");
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeEnd();
    }
}
