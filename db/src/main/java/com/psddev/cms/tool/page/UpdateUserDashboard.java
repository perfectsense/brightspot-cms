package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DashboardColumn;
import com.psddev.cms.tool.DashboardWidget;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "/misc/updateUserDashboard")
public class UpdateUserDashboard extends PageServlet {

    private static final String COLUMN_INDEX_KEY = "y";
    private static final String ROW_INDEX_KEY = "x";

    @Override
    protected String getPermissionId() {
        return "area/dashboard";
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        processAction(page);
    }

    protected static void processAction(ToolPageContext page) throws IOException, ServletException {

        if (!page.isFormPost()) {
            throw new IllegalStateException("Form must be posted!");
        }

        for (String action : page.params(String.class, "action")) {
            switch (action) {
                case "dashboardWidgets-add" :
                    addWidget(page);
                    break;
                case "dashboardWidgets-remove" :
                    removeWidget(page);
                    break;
                case "dashboardWidgets-move" :
                    moveWidget(page);
                    break;
                case "dashboardColumns-add" :
                    addColumn(page);
                    break;
                case "dashboardColumns-resize" :
                    resizeColumns(page);
                    break;
                default :
                    return;
            }
        }
    }

    /**
     * Adds a widget to a user's dashboard,
     * also conditionally adds a column
     * @param page
     */
    private static void addWidget(ToolPageContext page) {

        ToolUser user = page.getUser();
        Dashboard dashboard = getDashboard(page);

        int columnIndex = page.param(int.class, COLUMN_INDEX_KEY);
        int rowIndex = page.param(int.class, ROW_INDEX_KEY);
        boolean shouldAddColumn = page.param(boolean.class, "addColumn");
        List<DashboardColumn> columns = dashboard.getColumns();
        DashboardColumn column;

        if (shouldAddColumn) {
            column = new DashboardColumn();
            column.setWidth(DashboardColumn.MINIMUM_WIDTH);
            columns.add(columnIndex, column);
        } else {
            column = columns.get(columnIndex);
        }

        Object widget = page.getRequest().getAttribute("widget");

        if (widget == null) {
            throw new IllegalArgumentException("No widget found on request");
        }

        List<DashboardWidget> widgets = column.getWidgets();

        if (rowIndex >= widgets.size()) {
            widgets.add((DashboardWidget) widget);
        } else {
            widgets.add(rowIndex, (DashboardWidget) widget);
        }

        user.setDashboard(dashboard);
        user.save();
    }

    /**
     * Moves a widget on a user's dashboard
     * @param page
     */
    private static void moveWidget(ToolPageContext page) {

        ToolUser user = page.getUser();
        Dashboard dashboard = getDashboard(page);

        int targetX = page.param(int.class, ROW_INDEX_KEY);
        int targetY = page.param(int.class, COLUMN_INDEX_KEY);
        int originalX = page.param(int.class, "originalX");
        int originalY = page.param(int.class, "originalY");

        List<DashboardColumn> columns = dashboard.getColumns();
        DashboardWidget movedWidget = columns.get(originalY).getWidgets().get(originalX);
        DashboardColumn oldColumn = columns.get(originalY);

        //remove column if it will have no more widgets after move
        if (oldColumn.getWidgets().size() <= 1) {
            columns.remove(oldColumn);
        }

        oldColumn.getWidgets().remove(originalX);
        columns.get(targetY).getWidgets().add(targetX, movedWidget);

        user.setDashboard(dashboard);
        user.save();
    }

    /**
     * Removes a widget from the user's dashboard
     * @param page
     */
    private static void removeWidget(ToolPageContext page) {

        ToolUser user = page.getUser();
        Dashboard dashboard = getDashboard(page);

        int rowIndex = page.param(int.class, ROW_INDEX_KEY);
        int colIndex = page.param(int.class, COLUMN_INDEX_KEY);
        List<DashboardColumn> columns = dashboard.getColumns();
        DashboardColumn column = columns.get(colIndex);
        column.getWidgets().remove(rowIndex);

        if (ObjectUtils.isBlank(column.getWidgets())) {
            columns.remove(colIndex);
        }

        user.setDashboard(dashboard);
        user.save();
    }

    /**
     * Adds a column to the user's dashboard
     * @param page
     */
    private static void addColumn(ToolPageContext page) {

        ToolUser user = page.getUser();
        Dashboard dashboard = getDashboard(page);

        int col = page.param(int.class, COLUMN_INDEX_KEY);
        dashboard.getColumns().add(col, new DashboardColumn());

        user.setDashboard(dashboard);
        user.save();
    }

    private static void resizeColumns(ToolPageContext page) {
        ToolUser user = page.getUser();
        Dashboard dashboard = getDashboard(page);

        List<Integer> columnIndexes = page.params(int.class, COLUMN_INDEX_KEY);
        List<Integer> widths = page.params(int.class, "width");

        if (ObjectUtils.isBlank(columnIndexes) || ObjectUtils.isBlank(widths)) {
            return;
        }

        List<DashboardColumn> columns = dashboard.getColumns();

        for (int i : columnIndexes) {
            columns.get(i).setWidth(widths.get(i));
        }

        user.setDashboard(dashboard);
        user.save();
    }

    /**
     * Gets a dashboard for the user to edit
     * @param page
     * @return Dashboard
     */
    private static Dashboard getDashboard(ToolPageContext page) {

        Dashboard dashboard = page.getUser().getDashboard();

        if (dashboard == null) {
            dashboard = page.getCmsTool().getDefaultDashboard();

            if (dashboard == null) {
                dashboard = Dashboard.getDefaultDashboard();
            }
        }

        List<DashboardColumn> columns = dashboard.getColumns();

        if (ObjectUtils.isBlank(columns)) {
            columns.add(new DashboardColumn());
            dashboard.setColumns(columns);
        }

        return dashboard;

    }
}
