package com.psddev.cms.tool;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

import java.util.ArrayList;
import java.util.List;

@Recordable.Embedded
public class DashboardTab extends Record {

    @Required
    private String name;

    @Required
    private List<DashboardColumn> columns;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DashboardColumn> getColumns() {
        if (columns == null) {
            columns = new ArrayList<>();
        }

        return columns;
    }

    public void setColumns(List<DashboardColumn> columns) {
        this.columns = columns;
    }
}
