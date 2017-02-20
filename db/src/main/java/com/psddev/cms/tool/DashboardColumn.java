package com.psddev.cms.tool;

import java.util.ArrayList;
import java.util.List;

import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.Record;

@DashboardColumn.Embedded
public class DashboardColumn extends Record {

    @ToolUi.CollectionItemWeight
    private double width;

    @Embedded
    private List<DashboardWidget> widgets;

    public double getWidth() {
        return width > 0.0 ? width : 50.0;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public List<DashboardWidget> getWidgets() {
        if (widgets == null) {
            widgets = new ArrayList<>();
        }
        return widgets;
    }

    public void setWidgets(List<DashboardWidget> widgets) {
        this.widgets = widgets;
    }

    @Override
    public String getLabel() {
        return getWidgets().size() + " Widgets";
    }
}
