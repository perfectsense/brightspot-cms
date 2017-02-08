package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DashboardColumn;
import com.psddev.cms.tool.DashboardTab;
import com.psddev.cms.tool.DashboardWidget;
import com.psddev.cms.tool.DashboardContainer;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;
import com.psddev.dari.util.UuidUtils;

@RoutingFilter.Path(application = "cms", value = "/dashboardWidget")
public class DashboardWidgetPage extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        String pathInfo = page.getRequest().getPathInfo();
        pathInfo = StringUtils.removeStart(pathInfo, "/");
        pathInfo = StringUtils.removeEnd(pathInfo, "/");
        String[] pathInfoParts = pathInfo.split("/");
        Dashboard dashboard = null;
        DashboardContainer dashboardContainer = null;

        switch (pathInfoParts[0]) {
            case "user" :
                dashboardContainer = page.getUser().getDashboardContainer();
                break;

            case "role" :
                dashboardContainer = page.getUser().getRole().getDashboardContainer();
                break;

            case "tool" :
                dashboardContainer = page.getCmsTool().getDashboardContainer();
                break;

            case "default" :
                dashboard = Dashboard.createDefaultDashboard();
                break;

            default :
                throw new IllegalArgumentException();
        }

        if (dashboardContainer != null) {
            dashboard = dashboardContainer.getDashboard();
        }

        DashboardWidget widget = null;
        String widgetClassName = pathInfoParts[1];
        UUID widgetId = UuidUtils.fromString(pathInfoParts[2]);

        widget = Query
                .from(DashboardWidget.class)
                .where("_id = ?", widgetId)
                .first();

        if (widget == null) {
            COLUMNS: for (DashboardColumn column : dashboard.getColumns()) {
                if (column != null) {
                    for (DashboardWidget w : column.getWidgets()) {
                        if (w != null && widgetId.equals(w.getId())) {
                            widget = w;
                            break COLUMNS;
                        }
                    }
                }
            }

            if (widget == null) {
                TABS: for (DashboardTab tab : dashboard.getTabs()) {
                    for (DashboardColumn column : tab.getColumns()) {
                        if (column != null) {
                            for (DashboardWidget w : column.getWidgets()) {
                                if (w != null && widgetId.equals(w.getId())) {
                                    widget = w;
                                    break TABS;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (widget == null) {
            widget = (DashboardWidget) TypeDefinition.getInstance(ObjectUtils.getClassByName(widgetClassName)).newInstance();
        }

        widget.writeHtml(page, dashboard);
    }
}
