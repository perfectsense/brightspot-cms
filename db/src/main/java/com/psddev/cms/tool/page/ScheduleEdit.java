package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import com.psddev.cms.db.Draft;
import com.psddev.cms.db.Schedule;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "/scheduleEdit")
@SuppressWarnings("serial")
public class ScheduleEdit extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(final ToolPageContext page) throws IOException, ServletException {
        Schedule schedule = (Schedule) page.findOrReserve(Schedule.class);
        boolean newSchedule = schedule.getState().isNew();

        if (page.isFormPost()) {
            try {
                if (page.param(String.class, "action-save") != null) {
                    ToolUser toolUser = page.getUser();

                    page.include("/WEB-INF/objectPost.jsp", "object", schedule);

                    if (newSchedule) {
                        schedule.setTriggerUser(toolUser);
                        schedule.setTriggerSite(page.getSite());
                    }

                    schedule.save();

                    if (newSchedule) {
                        toolUser.setCurrentSchedule(schedule);
                        toolUser.save();
                    }

                } else if (page.param(String.class, "action-delete") != null) {
                    try {
                        schedule.beginWrites();
                        Query.from(Draft.class).where("schedule = ?", schedule).deleteAll();
                        schedule.delete();
                        schedule.commitWrites();

                    } finally {
                        schedule.endWrites();
                    }
                }

                page.writeStart("script", "type", "text/javascript");
                    page.writeRaw("window.location = window.location;");
                page.writeEnd();

                return;

            } catch (Exception error) {
                page.getErrors().add(error);
            }
        }

        page.writeStart("div", "class", "widget");
            page.writeFormHeading(schedule, "class", "icon icon-object-schedule");
            page.include("/WEB-INF/errors.jsp");

            page.writeStart("form",
                    "method", "post",
                    "action", page.url("", "id", schedule.getId()));
                page.include("/WEB-INF/objectForm.jsp", "object", schedule);

                page.writeStart("div", "class", "actions");
                    page.writeStart("button",
                            "class", "icon icon-action-save",
                            "name", "action-save",
                            "value", "true");
                        page.writeHtml("Save");
                    page.writeEnd();

                    if (!newSchedule) {
                        page.writeStart("button",
                                "class", "icon icon-action-delete action-pullRight link",
                                "name", "action-delete",
                                "value", "true");
                            page.writeHtml("Delete");
                        page.writeEnd();
                    }
                page.writeEnd();
            page.writeEnd();

            List<Draft> drafts = Query
                    .from(Draft.class)
                    .where("schedule = ?", schedule)
                    .selectAll();

            if (!drafts.isEmpty()) {
                page.writeStart("h2").writeHtml("Items").writeEnd();

                page.writeStart("div", "class", "fixedScrollable");
                    page.writeStart("ul", "class", "links");
                        for (Draft draft : drafts) {
                            page.writeStart("li");
                                page.writeStart("a",
                                        "href", page.objectUrl("/content/edit.jsp", draft),
                                        "target", "_top");
                                    page.writeObjectLabel(draft);
                                page.writeEnd();
                            page.writeEnd();
                        }
                    page.writeEnd();
                page.writeEnd();
            }
        page.writeEnd();
    }
}
