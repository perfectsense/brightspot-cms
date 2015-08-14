package com.psddev.cms.tool.page;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.cms.db.Schedule;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "/scheduleList")
@SuppressWarnings("serial")
public class ScheduleList extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(final ToolPageContext page) throws IOException, ServletException {
        reallyDoService(page);
    }

    public static void reallyDoService(ToolPageContext page) throws IOException, ServletException {
        page.writeStart("div", "class", "widget");
            page.writeStart("h1", "class", "icon icon-object-schedule");
                page.writeHtml("Available Schedules");
            page.writeEnd();

            page.writeStart("ul");
                for (Schedule schedule : Query
                        .from(Schedule.class)
                        .where("name != missing")
                        .sortAscending("name")
                        .selectAll()) {
                    page.writeStart("li");
                        page.writeStart("form",
                                "method", "post",
                                "style", "display: inline;",
                                "target", "_top",
                                "action", page.cmsUrl("/misc/updateUserSettings",
                                        "action", "scheduleSet",
                                        "scheduleId", schedule.getId(),
                                        "returnUrl", page.cmsUrl("/")));
                            page.writeStart("button", "class", "link");
                                page.writeObjectLabel(schedule);
                            page.writeEnd();
                        page.writeEnd();
                    page.writeEnd();
                }
            page.writeEnd();
        page.writeEnd();
    }
}
