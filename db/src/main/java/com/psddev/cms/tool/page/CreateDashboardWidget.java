package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.tool.DashboardWidget;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.UuidUtils;

//TODO merge into UpdateUserDashboard?
@RoutingFilter.Path(application = "cms", value = "/createWidget")
public class CreateDashboardWidget extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        if (page.isFormPost()) {
            String objectId = page.param(String.class, ToolPageContext.OBJECT_ID_PARAMETER);
            UUID newWidgetTypeId = page.param(UUID.class, objectId + "/widget.typeId");
            ObjectType widgetType = ObjectType.getInstance(newWidgetTypeId);
            Record widgetInstance = (Record) widgetType.createObject(UuidUtils.createSequentialUuid());

            //TODO properly create widget with all state values etc.

            page.getRequest().setAttribute("widget", widgetInstance);
            UpdateUserDashboard.processAction(page);
            page.writeTag("meta",
                    "name", "widget",
                    "content", page.cmsUrl("/dashboardWidget/user/" + widgetInstance.getClass().getName() + "/" + widgetInstance.getId() + "/"),
                    "data-y", page.param(String.class, "y"),
                    "data-x", page.param(String.class, "x"),
                    "data-add-column", page.param(boolean.class, "addColumn"));
        } else {
            page.writeStart("div", "class", "widget");
                page.writeStandardForm(new NewWidget());
            page.writeEnd();
        }
    }

    public static class NewWidget extends Record {

        @Embedded
        private DashboardWidget widget;

        public DashboardWidget getWidget() {
            return widget;
        }

        public void setWidget(DashboardWidget widget) {
            this.widget = widget;
        }
    }
}
