package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.ToolUserAction;
import com.psddev.cms.db.ToolUserDevice;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "toolUserHistory")
@SuppressWarnings("serial")
public class ToolUserHistory extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        ToolUser user = page.getUser();
        Map<String, List<ToolUserDevice>> devicesByUserAgent = new CompactMap<String, List<ToolUserDevice>>();

        for (ToolUserDevice device : Query
                .from(ToolUserDevice.class)
                .where("user = ?", user)
                .selectAll()) {
            String userAgent = device.getUserAgentDisplay();
            List<ToolUserDevice> devices = devicesByUserAgent.get(userAgent);

            if (devices == null) {
                devices = new ArrayList<ToolUserDevice>();
                devicesByUserAgent.put(userAgent, devices);
            }

            devices.add(device);
        }

        final Map<ToolUserDevice, List<ToolUserAction>> actionsByDevice = new CompactMap<ToolUserDevice, List<ToolUserAction>>();

        for (Map.Entry<String, List<ToolUserDevice>> entry : devicesByUserAgent.entrySet()) {
            ToolUserDevice device = null;
            List<ToolUserAction> actions = null;
            long lastTime = 0;

            for (ToolUserDevice d : entry.getValue()) {
                List<ToolUserAction> a = Query
                        .from(ToolUserAction.class)
                        .where("device = ?", d)
                        .sortDescending("time")
                        .selectAll();

                if (!a.isEmpty()) {
                    long time = a.get(0).getTime();

                    if (lastTime < time) {
                        lastTime = time;
                        device = d;
                        actions = a;
                    }
                }
            }

            if (device != null) {
                actionsByDevice.put(device, actions);
            }
        }

        List<ToolUserDevice> recentDevices = new ArrayList<ToolUserDevice>(actionsByDevice.keySet());

        Collections.sort(recentDevices, new Comparator<ToolUserDevice>() {

            @Override
            public int compare(ToolUserDevice x, ToolUserDevice y) {
                long xTime = actionsByDevice.get(x).get(0).getTime();
                long yTime = actionsByDevice.get(y).get(0).getTime();

                return xTime < yTime ? 1 : (xTime > yTime ? -1 : 0);
            }
        });

        page.writeHeader();
            page.writeStart("div", "class", "widget", "style", "overflow: hidden;");
                page.writeStart("h1", "class", "icon icon-object-history");
                    page.writeHtml("History");
                page.writeEnd();

                page.writeStart("div", "class", "tabbed");
                    for (ToolUserDevice device : recentDevices) {
                        List<ToolUserAction> actions = actionsByDevice.get(device);
                        String lookingGlassUrl = page.cmsUrl("/lookingGlass", "id", device.getOrCreateLookingGlassId());

                        page.writeStart("div", "data-tab", device.getUserAgentDisplay());
                            page.writeStart("div", "style", page.cssString(
                                    "float", "right",
                                    "text-align", "center"));
                                page.writeStart("a",
                                        "class", "icon icon-facetime-video",
                                        "target", "_blank",
                                        "href", lookingGlassUrl);
                                    page.writeHtml("Looking Glass");
                                page.writeEnd();

                                page.writeElement("br");

                                page.writeElement("img",
                                        "width", 150,
                                        "height", 150,
                                        "src", page.cmsUrl("qrCode",
                                                "data", JspUtils.getAbsoluteUrl(page.getRequest(), lookingGlassUrl),
                                                "size", 150));
                            page.writeEnd();

                            page.writeStart("ul",
                                    "class", "links",
                                    "style", page.cssString("margin-right", "150px"));
                                for (ToolUserAction action : actions) {
                                    Object actionContent = action.getContent();

                                    if (actionContent == null) {
                                        continue;
                                    }

                                    page.writeStart("li");
                                        page.writeStart("a",
                                                "target", "_top",
                                                "href", page.objectUrl("/content/edit.jsp", actionContent));
                                            page.writeTypeObjectLabel(actionContent);
                                        page.writeEnd();
                                    page.writeEnd();
                                }
                            page.writeEnd();
                        page.writeEnd();
                    }
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }
}
