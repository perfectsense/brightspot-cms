package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.ToolRole;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.CmsTool;
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

@RoutingFilter.Path(application = "cms", value = "/dashboard")
public class DashboardPage extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return "area/dashboard";
    }

    @Override
    public void doService(ToolPageContext page) throws IOException, ServletException {
        ToolUser user = page.getUser();
        Dashboard dashboard = null;
        DashboardContainer dashboardContainer = user.getDashboardContainer();
        String dashboardId = null;

        if (dashboardContainer != null) {
            dashboard = dashboardContainer.getDashboard();
            dashboardId = "user";
        }

        if (dashboard == null) {
            ToolRole role = user.getRole();

            if (role != null) {
                dashboardContainer = role.getDashboardContainer();

                if (dashboardContainer != null) {
                    dashboard = dashboardContainer.getDashboard();
                    dashboardId = "role";
                }
            }
        }

        if (dashboard == null) {
            dashboardContainer = page.getCmsTool().getDashboardContainer();

            if (dashboardContainer != null) {
                dashboard = dashboardContainer.getDashboard();
                dashboardId = "tool";
            }
        }

        if (dashboard == null) {
            dashboard = Dashboard.createDefaultDashboard();
            dashboardId = "default";
        }

        page.writeHeader();
            List<DashboardTab> tabs = dashboard.getTabs();
            UUID tabId = page.param(UUID.class, "tab");
            DashboardTab selectedTab = tabs.stream()
                    .filter(t -> t.getId().equals(tabId))
                    .findFirst()
                    .orElse(null);

            if (!tabs.isEmpty()) {
                page.writeStart("div", "class", "DashboardTabSelect");
                page.writeStart("ul");
                {
                    page.writeStart("li", "class", selectedTab == null ? "selected" : null);
                    page.writeStart("a", "href", page.url("", "tab", null));
                    page.writeHtml(ObjectUtils.firstNonBlank(dashboard.getName(), "Main"));
                    page.writeEnd();
                    page.writeEnd();

                    for (DashboardTab tab : tabs) {
                        page.writeStart("li", "class", tab.equals(selectedTab) ? "selected" : null);
                        page.writeStart("a", "href", page.url("", "tab", tab.getId()));
                        page.writeHtml(tab.getName());
                        page.writeEnd();
                        page.writeEnd();
                    }
                }
                page.writeEnd();
                page.writeEnd();
            }

            page.writeStart("div", "class", "dashboard-columns");
                List<DashboardColumn> columns = selectedTab != null
                        ? selectedTab.getColumns()
                        : dashboard.getColumns();

                double totalWidth = 0;

                for (DashboardColumn column : columns) {
                    double width = column.getWidth();
                    totalWidth += width > 0 ? width : 1;
                }

                CmsTool cms = Query.from(CmsTool.class).first();
                Set<String> disabled = cms != null ? cms.getDisabledPlugins() : Collections.emptySet();

                for (int c = 0, cSize = columns.size(); c < cSize; ++ c) {
                    DashboardColumn column = columns.get(c);
                    double width = column.getWidth();

                    page.writeStart("div",
                            "class", "dashboard-column",
                            "style", page.cssString("width", ((width > 0 ? width : 1) / totalWidth * 100) + "%"));

                        List<DashboardWidget> widgets = column.getWidgets();

                        for (int w = 0, wSize = widgets.size(); w < wSize; ++ w) {
                            DashboardWidget widget = widgets.get(w);

                            if (disabled.contains(widget.getClass().getName())) {
                                continue;
                            }

                            String widgetUrl = page.toolUrl(CmsTool.class,
                                    "/dashboardWidget/"
                                            + dashboardId + "/"
                                            + widget.getClass().getName() + "/"
                                            + widget.getId());

                            page.writeStart("div", "class", "frame dashboard-widget", "data-dashboard-widget-url", widgetUrl);
                                page.writeStart("a", "href", widgetUrl);
                                page.writeEnd();
                            page.writeEnd();
                        }
                    page.writeEnd();
                }
            page.writeEnd();
        page.writeFooter();
    }
}
