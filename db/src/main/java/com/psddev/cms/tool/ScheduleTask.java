package com.psddev.cms.tool;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.joda.time.DateTime;
import com.psddev.cms.db.Schedule;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.RepeatingTask;
import com.psddev.dari.util.Settings;

/**
 * Triggers scheduled events for publishing.
 */
public class ScheduleTask extends RepeatingTask {

    public static final String SCHEDULE_THREAD_NAME = "ScheduleFilter";

    @Override
    protected DateTime calculateRunTime(DateTime currentTime) {
        return everyMinute(currentTime);
    }

    @Override
    protected void doRepeatingTask(DateTime runTime) throws Exception {
        if (ObjectType.getInstance(Schedule.class.getName()) == null) {
            return;
        }

        // Backwards compatibility with the previous method for disabling ScheduleFilter.
        if (Boolean.TRUE.equals(Settings.get(Boolean.class, AbstractFilter.DISABLE_FILTER_SETTING_PREFIX + ScheduleFilter.class.getName()))) {
            return;
        }

        Thread.currentThread().setName(SCHEDULE_THREAD_NAME);

        for (Schedule schedule : Query
                .from(Schedule.class)
                .sortAscending("triggerDate")
                .master()
                .noCache()
                .resolveInvisible()
                .iterable(0)) {

            try {
                schedule.trigger();

            } catch (Exception ex1) {
                try {
                    StringWriter writer = new StringWriter();
                    ex1.printStackTrace(new PrintWriter(writer));
                    schedule.getState().put("cms.lastException", writer.toString());
                    schedule.save();
                } catch (Exception ex2) {
                    // Ignore any error caused by trying to save the error
                    // information to the schedule itself.
                }
            }
        }
    }
}
