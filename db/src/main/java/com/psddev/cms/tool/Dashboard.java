package com.psddev.cms.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable.Embedded;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.TypeDefinition;

@Embedded
public class Dashboard extends Record {

    private List<DashboardColumn> columns;

    public static Dashboard getDefaultDashboard() {
        Dashboard dashboard = new Dashboard();
        List<DashboardColumn> columns = dashboard.getColumns();

        for (Class<? extends DefaultDashboardWidget> c : ClassFinder.Static.findClasses(DefaultDashboardWidget.class)) {
            DefaultDashboardWidget widget = TypeDefinition.getInstance(c).newInstance();
            int columnIndex = widget.getColumnIndex();

            while (columns.size() - 1 < columnIndex) {
                columns.add(new DashboardColumn());
            }

            columns.get(columnIndex).getWidgets().add(widget);
        }

        for (ListIterator<DashboardColumn> i = columns.listIterator(columns.size()); i.hasPrevious();) {
            DashboardColumn column = i.previous();
            Collections.sort(column.getWidgets(), new Comparator<DashboardWidget>() {
                @Override
                public int compare(DashboardWidget x, DashboardWidget y) {
                    return ((DefaultDashboardWidget) x).getWidgetIndex() - ((DefaultDashboardWidget) y).getWidgetIndex();
                }
            });
        }

        return dashboard;
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
