package com.psddev.cms.tool;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.dari.db.Record;
import com.psddev.dari.util.StringUtils;

public abstract class DashboardWidget extends Record {

    public abstract void writeHtml(ToolPageContext page, Dashboard dashboard) throws IOException, ServletException;

    /**
     * Returns the ID that represents this DashboardWidget class for use in
     * permissions.
     */
    public String getPermissionId() {
        return "widget/" + getClass().getCanonicalName();
    }

    @Override
    public String getLabel() {
        return StringUtils.toLabel(getClass().getSimpleName());
    }
}
