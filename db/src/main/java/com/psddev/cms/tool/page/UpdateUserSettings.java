package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.Schedule;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "/misc/updateUserSettings")
@SuppressWarnings("serial")
public class UpdateUserSettings extends PageServlet {

    @Override
    protected String getPermissionId() {
        return "area/dashboard";
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        if (!page.isFormPost()) {
            throw new IllegalStateException("Form must be posted!");
        }

        ToolUser user = page.getUser();
        State userState = user.getState();

        for (String action : page.params(String.class, "action")) {
            if ("liveContentPreview-enable".equals(action)) {
                userState.put("liveContentPreview", true);

            } else if ("liveContentPreview-disable".equals(action)) {
                userState.put("liveContentPreview", false);

            } else if ("dashboardWidgets-position".equals(action)) {
                String settingsPrefix = page.paramOrDefault(String.class, "settingsPrefix", "");

                userState.put(settingsPrefix + "dashboardWidgets", ObjectUtils.fromJson(page.param(String.class, "widgets")));
                userState.put(settingsPrefix + "dashboardWidgetsCollapse", ObjectUtils.fromJson(page.param(String.class, "widgetsCollapse")));

            } else if ("scheduleSet".equals(action)) {
                user.setCurrentSchedule(Query
                        .from(Schedule.class)
                        .where("_id = ?", page.param(UUID.class, "scheduleId"))
                        .first());
            }
        }

        userState.save();

        String returnUrl = page.param(String.class, "returnUrl");

        if (!ObjectUtils.isBlank(returnUrl)) {
            page.getResponse().sendRedirect(returnUrl);
        }
    }
}
