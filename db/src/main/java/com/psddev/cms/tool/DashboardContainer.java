package com.psddev.cms.tool;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

@Recordable.Embedded
public abstract class DashboardContainer extends Record {

    public abstract Dashboard getDashboard();

    public static class Shared extends DashboardContainer {

        @Required
        private Dashboard dashboard;

        @Override
        public Dashboard getDashboard() {
            return dashboard;
        }

        public void setDashboard(Dashboard dashboard) {
            this.dashboard = dashboard;
        }
    }

    public static class OneOff extends DashboardContainer {

        @Embedded
        @Required
        private Dashboard dashboard;

        @Override
        public Dashboard getDashboard() {
            return dashboard;
        }

        public void setDashboard(Dashboard dashboard) {
            this.dashboard = dashboard;
        }
    }
}
