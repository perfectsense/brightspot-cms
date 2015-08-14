package com.psddev.cms.tool.widget;

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
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DefaultDashboardWidget;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ObjectUtils;

public class ScheduledEventsWidget extends DefaultDashboardWidget {

    @Override
    public int getColumnIndex() {
        return 1;
    }

    @Override
    public int getWidgetIndex() {
        return 2;
    }

    @Override
    public void writeHtml(ToolPageContext page, Dashboard dashboard) throws IOException, ServletException {
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

        for (Schedule schedule : Query
                .from(Schedule.class)
                .where("triggerDate >= ? and triggerDate < ?", begin, end)
                .sortAscending("triggerDate")
                .iterable(0)) {

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

                page.writeHtml("Scheduled Events");

            page.writeEnd();

            page.writeStart("div", "class", "scheduledEvents-controls");
                page.writeStart("ul", "class", "piped");
                    page.writeStart("li");
                        page.writeStart("a",
                                "class", "icon icon-action-create",
                                "href", page.cmsUrl("/scheduleEdit"),
                                "target", "scheduleEdit");
                            page.writeHtml("New");
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("li");
                        page.writeStart("a",
                                "class", "icon icon-action-search",
                                "href", page.cmsUrl("/scheduleList"),
                                "target", "scheduleList");
                            page.writeHtml("View All");
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("ul", "class", "scheduledEvents-modes");
                    for (Mode m : Mode.values()) {
                        page.writeStart("li", "class", (m.equals(mode) ? "selected" : ""));
                            page.writeStart("a",
                                    "href", page.url("", "mode", m.name()));
                                page.writeHtml(m.displayName);
                            page.writeEnd();
                        page.writeEnd();
                    }
                page.writeEnd();
            page.writeEnd();

            page.writeTag("hr", "class", "scheduledEvents-separator");

            String beginMonth = begin.monthOfYear().getAsText();
            int beginYear = begin.year().get();
            String endMonth = end.monthOfYear().getAsText();
            int endYear = end.year().get();

            page.writeStart("div", "class", "scheduledEvents-controls");
                page.writeStart("div", "class", "scheduledEvents-dateRange");
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

                    page.writeStart("li");
                        page.writeStart("a",
                                "href", page.url("", "date", System.currentTimeMillis()));
                            page.writeHtml("Today");
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("li", "class", "next");
                        page.writeStart("a",
                                "href", page.url("", "date", mode.getNext(date).getMillis()));
                            page.writeHtml("Next ").writeHtml(mode);
                        page.writeEnd();
                    page.writeEnd();

                page.writeEnd();
            page.writeEnd();

            mode.display(page, schedulesByDate);
        page.writeEnd();
    }

    private enum Mode {
        DAY("Day") {

            @Override
            public DateTime getBegin(DateTime date) {
                return date.toDateMidnight().toDateTime();
            }

            @Override
            public DateTime getEnd(DateTime date) {
                return getBegin(date).plusDays(1);
            }

            @Override
            public DateTime getPrevious(DateTime date) {
                return date.plusDays(-1);
            }

            @Override
            public DateTime getNext(DateTime date) {
                return date.plusDays(1);
            }

            @Override
            public void display(ToolPageContext page, Map<DateTime, List<Schedule>> schedulesByDate) throws IOException {
                displayAgendaView(page, schedulesByDate);
            }
        },
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
                displayAgendaView(page, schedulesByDate);
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
                page.writeStart("div", "class", "calendar calendar-month");

                for (Map.Entry<DateTime, List<Schedule>> entry : schedulesByDate.entrySet()) {
                    DateTime date = entry.getKey();
                    List<Schedule> schedules = entry.getValue();

                    if (date.getDayOfMonth() == 1 || date.getDayOfWeek() == 1) {
                        page.writeStart("div", "class", "calendarRow");

                        if (date.getDayOfMonth() == 1) {
                            int offset = date.getDayOfWeek() - 1;
                            for (int i = offset; i > 0; i--) {
                                writeCalendarDay(page, null, date.minusDays(i), "other-month");
                            }
                        }
                    }

                    writeCalendarDay(page, schedules, date, "");

                    if (date.dayOfMonth().withMaximumValue().equals(date)) {
                        int extraDays = 7 - date.getDayOfWeek();
                        for (int i = 1; i <= extraDays; i++) {
                            writeCalendarDay(page, null, date.plusDays(i), "other-month");
                        }
                    }

                    if (date.getDayOfMonth() == 31 || date.getDayOfWeek() == 7) {
                        page.writeEnd();
                    }

                }
                page.writeEnd();
            }

            private void writeCalendarDay(ToolPageContext page, List<Schedule> schedules, DateTime date, String extraClass) throws IOException {
                page.writeStart("div", "class", "calendarDay" + (date.equals(new DateTime(null, page.getUserDateTimeZone()).toDateMidnight()) ? " calendarDay-today" : "") + (" day-of-week-" + date.getDayOfWeek()) + " " + extraClass);
                    page.writeStart("span", "class", "calendarDayOfWeek").writeHtml(date.dayOfWeek().getAsShortText()).writeEnd();
                    page.writeStart("span", "class", "calendarDayOfMonth").writeHtml(date.dayOfMonth().get()).writeEnd();

                    if (!ObjectUtils.isBlank(schedules)) {

                        for (Schedule schedule : schedules) {
                            List<Object> drafts = Query.fromAll().where("com.psddev.cms.db.Draft/schedule = ?", schedule).selectAll();

                            if (drafts.isEmpty()) {
                                continue;
                            }

                            int draftCount = drafts.size();

                            page.writeStart("div", "class", "calendarEventsContainer");
                                page.writeStart("a",
                                        "href", page.cmsUrl("/scheduleEventsList", "date", date.toDate().getTime()),
                                        "target", "scheduleEventsList");
                                    page.writeStart("div", "class", "calendarEvents");
                                        page.writeStart("div", "class", "count");
                                            page.writeHtml(draftCount);
                                        page.writeEnd();
                                        page.writeStart("div", "class", "label");
                                            page.writeHtml("Event" + (draftCount > 1 ? "s" : ""));
                                        page.writeEnd();
                                    page.writeEnd();
                                page.writeEnd();
                            page.writeEnd();
                        }
                    }

                page.writeEnd();
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

        public static void displayAgendaView(ToolPageContext page, Map<DateTime, List<Schedule>> schedulesByDate) throws IOException {
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
    }
}
