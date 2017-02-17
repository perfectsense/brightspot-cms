package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import javax.servlet.ServletException;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.util.ObjectUtils;

public class ProfileTab extends ProfilePanelTab {

    @Override
    public void writeHtml(ToolPageContext page) throws IOException, ServletException {

        String tab = page.param(String.class, "tab");
        ToolUser user = page.getUser();

        if (user != null
                && user.getRole() != null
                && !user.getRole().hasPermission("type" + ObjectType.getInstance(ToolUser.class).getId() + "/read")
                && (ObjectUtils.isBlank(page.param(UUID.class, "id")) || user.getId().equals(page.param(UUID.class, "id")))) {

            user.getRole().setPermissions(user.getRole().getPermissions().replace("-type/" + ObjectType.getInstance(ToolUser.class).getId(), ""));
        }

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
                "data-tab", page.localize(ProfileTab.class, "title"));

            page.writeStart("ul", "class", "piped");
                page.writeStart("li");
                    page.writeStart("a",
                            "class", "icon icon-key",
                            "href", page.cmsUrl("/toolUserTfa"),
                            "target", "toolUserTfa");
                        page.writeHtml(page.localize(ProfileTab.class, user.isTfaEnabled() ? "action.disableTfa" : "action.enableTfa"));
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
                        page.writeHtml(page.localize(ProfileTab.class, "action.save"));
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeEnd();
    }
}
