package com.psddev.cms.tool.page;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.Localization;
import com.psddev.cms.db.LocalizationContext;
import com.psddev.cms.db.Schedule;
import com.psddev.cms.db.Site;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.RoutingFilter;
import org.joda.time.DateTime;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RoutingFilter.Path(application = "cms", value = "/scheduleEventsList")
public class ScheduleEventsList extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(final ToolPageContext page) throws IOException, ServletException {
        reallyDoService(page);
    }

    public static void reallyDoService(ToolPageContext page) throws IOException, ServletException {
        long ms = page.param(long.class, "date");
        DateTime date = new DateTime(ms);
        Site currentSite = page.getSite();
        DateTime begin = date.toDateMidnight().toDateTime();
        DateTime end = begin.plusDays(1);

        List<Schedule> schedules = new ArrayList<>();

        for (Schedule schedule : Query
                .from(Schedule.class)
                .where("triggerDate >= ? and triggerDate < ?", begin, end)
                .sortAscending("triggerDate")
                .iterable(0)) {

            if (currentSite != null && !currentSite.equals(schedule.getTriggerSite())) {
                continue;
            }
            schedules.add(schedule);
        }

        page.writeStart("div", "class", "widget");
        {
            page.writeStart("h1", "class", "icon icon-object-schedule");
            page.writeHtml(
                    Localization.currentUserText(
                            new LocalizationContext(
                                    ScheduleEventsList.class,
                                    ImmutableMap.of(
                                            "date",
                                            Localization.currentUserDate(begin.getMillis(), Localization.DATE_ONLY_SKELETON))),
                            "title"));
            page.writeEnd();

            page.writeStart("table", "class", "links table-striped");
            {
                page.writeStart("thead");
                {
                    page.writeStart("tr");
                    {
                        page.writeStart("th");
                        page.writeHtml("Content");
                        page.writeEnd();

                        page.writeStart("th");
                        page.writeHtml("Time");
                        page.writeEnd();
                    }
                    page.writeEnd();
                }
                page.writeEnd();

                page.writeStart("tbody");
                {
                    for (Schedule schedule : schedules) {
                        List<Object> drafts = Query.fromAll().where("com.psddev.cms.db.Draft/schedule = ?", schedule).selectAll();

                        if (drafts.isEmpty()) {
                            continue;
                        }

                        Date triggerDate = schedule.getTriggerDate();

                        for (Object draft : drafts) {
                            page.writeStart("tr");
                            {
                                page.writeStart("td");
                                page.writeStart("a",
                                        "href", page.objectUrl("/content/edit.jsp", draft),
                                        "target", "_top");
                                page.writeObjectLabel(draft);
                                page.writeEnd();

                                page.writeStart("td");
                                page.writeHtml(triggerDate != null
                                        ? Localization.currentUserDate(triggerDate.getTime(), Localization.TIME_ONLY_SKELETON)
                                        : Localization.currentUserText(ScheduleEventsList.class, Localization.NOT_AVAILABLE_KEY));
                                page.writeEnd();
                            }
                            page.writeEnd();
                        }
                    }
                }
                page.writeEnd();
            }
            page.writeEnd();
        }
        page.writeEnd();
    }
}
