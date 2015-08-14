package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import com.psddev.cms.db.ToolUserAction;
import com.psddev.cms.db.ToolUserDevice;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.JspUtils;

public class LookingGlassTab extends ProfilePanelTab {

    @Override
    public void writeHtml(ToolPageContext page) throws IOException, ServletException {

        Map<String, List<ToolUserDevice>> devicesByUserAgent = new CompactMap<String, List<ToolUserDevice>>();

        for (ToolUserDevice device : Query
                .from(ToolUserDevice.class)
                .where("user = ?", page.getUser())
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

        recentDevices.sort((x, y) -> {
            long xTime = actionsByDevice.get(x).get(0).getTime();
            long yTime = actionsByDevice.get(y).get(0).getTime();

            return xTime < yTime ? 1 : (xTime > yTime ? -1 : 0);
        });

        page.writeStart("div",
                "class", "p-tud-lg tabbed",
                "data-tab", "Looking Glass");

            for (ToolUserDevice device : recentDevices) {
                List<ToolUserAction> actions = actionsByDevice.get(device);
                String lookingGlassUrl = JspUtils.getAbsoluteUrl(page.getRequest(), page.cmsUrl("/lookingGlass", "id", device.getOrCreateLookingGlassId()));

                page.writeStart("div", "data-tab", device.getUserAgentDisplay());
                    page.writeStart("div", "class", "p-tud-lg-url");
                        page.writeStart("p");
                            page.writeHtml("Mirror your activities in this browser to another device or share them with another user by using the link or the QR code below:");
                        page.writeEnd();

                        page.writeElement("input",
                                "type", "text",
                                "value", lookingGlassUrl);

                        page.writeElement("img",
                                "width", 200,
                                "height", 200,
                                "src", page.cmsUrl("qrCode",
                                        "data", lookingGlassUrl,
                                        "size", 200));
                    page.writeEnd();

                    page.writeStart("div", "class", "p-tud-lg-recent");
                        page.writeStart("h2");
                            page.writeHtml("Recent Activity");
                        page.writeEnd();

                        page.writeStart("ul", "class", "links");
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
                page.writeEnd();
            }
        page.writeEnd();
    }
}
