package com.psddev.cms.tool;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

/**
 * Dashboard wrapper interface to provide capability to share dashboard.
 */
public interface DashboardWrapper extends Recordable {

    Dashboard getDashboard();

    @Embedded
    class SharedDashboard extends Record implements DashboardWrapper {

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

    @Embedded
    class EmbeddedDashboard extends Record implements DashboardWrapper {

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
