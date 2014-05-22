package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;

import org.joda.time.DateTime;

import com.psddev.cms.db.Draft;
import com.psddev.cms.db.Schedule;
import com.psddev.cms.db.Site;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "/misc/scheduledEvents.jsp")
@SuppressWarnings("serial")
public class ScheduledEvents extends PageServlet {

    @Override
    protected String getPermissionId() {
        return "area/dashboard";
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Mode mode = page.pageParam(Mode.class, "mode", Mode.WEEK);
        DateTime date = new DateTime(page.param(Date.class, "date"), page.getUserDateTimeZone());
        DateTime begin = mode.getBegin(date);
        DateTime end = mode.getEnd(date);
        Map<DateTime, List<Schedule>> schedulesByDate = new TreeMap<DateTime, List<Schedule>>();
        boolean hasSchedules = false;

        for (DateTime i = begin; i.isBefore(end); i = i.plusDays(1)) {
            schedulesByDate.put(i, new ArrayList<Schedule>());
        }

        Site currentSite = page.getSite();

        for (Schedule schedule : Query.
                from(Schedule.class).
                where("triggerDate >= ? and triggerDate < ?", begin, end).
                sortAscending("triggerDate").
                iterable(0)) {

            if (currentSite != null && !currentSite.equals(schedule.getTriggerSite())) {
                continue;
            }

            DateTime scheduleDate = page.toUserDateTime(schedule.getTriggerDate()).toDateMidnight().toDateTime();
            List<Schedule> schedules = schedulesByDate.get(scheduleDate);

            if (schedules != null) {
                schedules.add(schedule);
                hasSchedules = true;
            }
        }

        page.writeStart("div", "class", "widget widget-scheduledEvents" + (hasSchedules ? "" : " widget-scheduledEvents-empty"));
            page.writeStart("h1", "class", "icon icon-action-schedule");

                page.writeHtml("Scheduled Events: ");

                String beginMonth = begin.monthOfYear().getAsText();
                int beginYear = begin.year().get();
                String endMonth = end.monthOfYear().getAsText();
                int endYear = end.year().get();

                page.writeHtml(beginMonth);
                page.writeHtml(" ");
                page.writeHtml(begin.dayOfMonth().get());

                if (beginYear != endYear) {
                    page.writeHtml(", ");
                    page.writeHtml(beginYear);
                }

                page.writeHtml(" - ");

                if (!endMonth.equals(beginMonth)) {
                    page.writeHtml(endMonth);
                    page.writeHtml(" ");
                }

                page.writeHtml(end.dayOfMonth().get());
                page.writeHtml(", ");
                page.writeHtml(endYear);

            page.writeEnd();

            page.writeStart("ul", "class", "piped");
                page.writeStart("li");
                    page.writeStart("a",
                            "class", "icon icon-action-create",
                            "href", page.cmsUrl("/scheduleEdit"),
                            "target", "scheduleEdit");
                        page.writeHtml("New Schedule");
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("li");
                    page.writeStart("a",
                            "class", "icon icon-action-search",
                            "href", page.cmsUrl("/scheduleList"),
                            "target", "scheduleList");
                        page.writeHtml("Available Schedules");
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();

            /*
            page.writeStart("form", "method", "get", "data-bsp-autosubmit", "", "action", page.url(null));
                page.writeStart("select", "name", "mode");
                    for (Mode m : Mode.values()) {
                        page.writeStart("option",
                                "value", m.name(),
                                "selected", m.equals(mode) ? "selected" : null);
                            page.writeHtml(m);
                        page.writeEnd();
                    }
                page.writeEnd();
            page.writeEnd();
            */

            page.writeStart("ul", "class", "pagination");

                DateTime previous = mode.getPrevious(date);
                DateTime today = new DateTime(null, page.getUserDateTimeZone()).toDateMidnight().toDateTime();

                if (!previous.isBefore(today)) {
                    page.writeStart("li", "class", "previous");
                        page.writeStart("a",
                                "href", page.url("", "date", previous.getMillis()));
                            page.writeHtml("Previous ").writeHtml(mode);
                        page.writeEnd();
                    page.writeEnd();
                }

                if (begin.isAfter(today) || end.isBefore(today)) {
                    page.writeStart("li");
                        page.writeStart("a",
                                "href", page.url("", "date", System.currentTimeMillis()));
                            page.writeHtml("Today");
                        page.writeEnd();
                    page.writeEnd();
                }

                page.writeStart("li", "class", "next");
                    page.writeStart("a",
                            "href", page.url("", "date", mode.getNext(date).getMillis()));
                        page.writeHtml("Next ").writeHtml(mode);
                    page.writeEnd();
                page.writeEnd();

            page.writeEnd();

            mode.display(page, schedulesByDate);
        page.writeEnd();
    }

    private enum Mode {
        WEEK("Week") {

            @Override
            public DateTime getBegin(DateTime date) {
                return date.toDateMidnight().toDateTime();
            }

            @Override
            public DateTime getEnd(DateTime date) {
                return getBegin(date).plusWeeks(1);
            }

            @Override
            public DateTime getPrevious(DateTime date) {
                return date.plusWeeks(-1);
            }

            @Override
            public DateTime getNext(DateTime date) {
                return date.plusWeeks(1);
            }

            @Override
            public void display(ToolPageContext page, Map<DateTime, List<Schedule>> schedulesByDate) throws IOException {
                page.writeStart("div", "class", "calendar calendar-week");
                    for (Map.Entry<DateTime, List<Schedule>> entry : schedulesByDate.entrySet()) {
                        DateTime date = entry.getKey();
                        List<Schedule> schedules = entry.getValue();

                        page.writeStart("div", "class", "calendarRow");
                            page.writeStart("div", "class", "calendarDay" + (date.equals(new DateTime(null, page.getUserDateTimeZone()).toDateMidnight()) ? " calendarDay-today" : ""));
                                page.writeStart("span", "class", "calendarDayOfWeek").writeHtml(date.dayOfWeek().getAsShortText()).writeEnd();
                                page.writeStart("span", "class", "calendarDayOfMonth").writeHtml(date.dayOfMonth().get()).writeEnd();
                            page.writeEnd();

                            page.writeStart("div", "class", "calendarCell").writeStart("table", "class", "links table-striped pageThumbnails").writeStart("tbody");
                                for (Schedule schedule : schedules) {
                                    DateTime triggerDate = page.toUserDateTime(schedule.getTriggerDate());
                                    List<Object> drafts = Query.fromAll().where("com.psddev.cms.db.Draft/schedule = ?", schedule).selectAll();

                                    if (drafts.isEmpty()) {
                                        continue;
                                    }

                                    boolean first = true;

                                    for (Object d : drafts) {
                                        if (!(d instanceof Draft)) {
                                            continue;
                                        }

                                        Draft draft = (Draft) d;
                                        Object draftObject = draft.getObject();

                                        page.writeStart("tr", "data-preview-url", "/_preview?_cms.db.previewId=" + draft.getId());
                                            page.writeStart("td", "class", "time");
                                                if (first) {
                                                    page.writeHtml(triggerDate.toString("hh:mm a"));
                                                    first = false;
                                                }
                                            page.writeEnd();

                                            page.writeStart("td");
                                                page.writeTypeLabel(draftObject);
                                            page.writeEnd();

                                            page.writeStart("td", "data-preview-anchor", "");
                                                page.writeStart("a",
                                                        "href", page.objectUrl("/content/edit.jsp", draft),
                                                        "target", "_top");
                                                    page.writeObjectLabel(draftObject);
                                                page.writeEnd();
                                            page.writeEnd();
                                        page.writeEnd();
                                    }
                                }
                            page.writeEnd().writeEnd().writeEnd();
                        page.writeEnd();
                    }
                page.writeEnd();
            }
        },

        MONTH("Month") {

            @Override
            public DateTime getBegin(DateTime date) {
                return date.toDateMidnight().withDayOfMonth(1).toDateTime();
            }

            @Override
            public DateTime getEnd(DateTime date) {
                return getBegin(date).plusMonths(1);
            }

            @Override
            public DateTime getPrevious(DateTime date) {
                return date.plusMonths(-1);
            }

            @Override
            public DateTime getNext(DateTime date) {
                return date.plusMonths(1);
            }

            @Override
            public void display(ToolPageContext page, Map<DateTime, List<Schedule>> schedulesByDate) throws IOException {
            }
        };

        private final String displayName;

        private Mode(String displayName) {
            this.displayName = displayName;
        }

        public abstract DateTime getBegin(DateTime date);

        public abstract DateTime getEnd(DateTime date);

        public abstract DateTime getPrevious(DateTime date);

        public abstract DateTime getNext(DateTime date);

        public abstract void display(ToolPageContext page, Map<DateTime, List<Schedule>> schedulesByDate) throws IOException;

        @Override
        public String toString() {
            return displayName;
        }
    }
}
