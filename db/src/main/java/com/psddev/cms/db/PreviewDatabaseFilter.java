package com.psddev.cms.db;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.dari.db.ApplicationFilter;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.ObjectUtils;

public class PreviewDatabaseFilter extends AbstractFilter implements AbstractFilter.Auto {

    @Override
    public void updateDependencies(
            Class<? extends AbstractFilter> filterClass,
            List<Class<? extends Filter>> dependencies) {
        if (ApplicationFilter.class.isAssignableFrom(filterClass)) {
            dependencies.add(getClass());
        }
    }

    @Override
    public void doRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        ToolUser user = AuthenticationFilter.Static.getUser(request);

        if (user != null) {
            Schedule currentSchedule = Query.from(Schedule.class).where("_id = ?", request.getParameter("_scheduleId")).first();

            if (currentSchedule == null) {
                currentSchedule = user.getCurrentSchedule();
            }

            Date date = ObjectUtils.to(Date.class, request.getParameter("_date"));

            if (currentSchedule != null || date != null || PageFilter.Static.isPreview(request)) {
                try {
                    PreviewDatabase pd = new PreviewDatabase();

                    pd.setDelegate(Database.Static.getDefault());

                    if (currentSchedule != null) {
                        pd.addChanges(currentSchedule);

                    } else if (date != null) {
                        pd.setDate(date);
                    }

                    Database.Static.overrideDefault(pd);

                    Object mainObject = PageFilter.Static.getMainObject(request);
                    State mainState = State.getInstance(PageFilter.Static.getMainObject(request));

                    if (mainState != null) {
                        Object originalObject = Query
                                .fromAll()
                                .where("_id = ?", mainState.getId())
                                .resolveInvisible()
                                .noCache()
                                .first();

                        mainState.setDatabase(null);
                        mainState.setResolveInvisible(true);

                        if (originalObject != null) {
                            mainState.setValues(State.getInstance(pd.applyChanges(originalObject)).getSimpleValues());

                        } else {
                            mainState.setValues(State.getInstance(pd.applyChanges(mainObject)).getSimpleValues());
                        }
                    }

                    chain.doFilter(request, response);

                } finally {
                    Database.Static.restoreDefault();
                }

                return;
            }
        }

        chain.doFilter(request, response);
    }
}
