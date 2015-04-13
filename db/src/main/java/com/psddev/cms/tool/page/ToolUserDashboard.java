package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.ToolUserAction;
import com.psddev.cms.db.ToolUserDevice;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "toolUserDashboard")
@SuppressWarnings("serial")
public class ToolUserDashboard extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        ToolUser user = page.getUser();
        String tab = page.param(String.class, "tab");
        Collection<String> excludeFields = new ArrayList<String>();

        excludeFields.add("role");
        excludeFields.add("changePasswordOnLogIn");

        if (user.isExternal()) {
            excludeFields.add("password");
        }

        if ("site".equals(tab)) {
            Site newCurrentSite = Query.
                    from(Site.class).
                    where("_id = ?", page.param(UUID.class, "id")).
                    first();

            user.setCurrentSite(newCurrentSite);
            page.publish(user);

            page.writeStart("script", "type", "text/javascript");
                page.writeRaw("window.top.window.location = window.top.window.location;");
            page.writeEnd();
            return;

        } else if ("profile".equals(tab) && page.isFormPost()) {
            try {
                page.include("/WEB-INF/objectPost.jsp", "object", user, "excludeFields", excludeFields);
                user.save();

                page.writeStart("script", "type", "text/javascript");
                    page.writeRaw("window.top.window.location = window.top.window.location;");
                page.writeEnd();
                return;

            } catch (Exception ex) {
                page.getErrors().add(ex);
            }
        }

        Map<String, List<ToolUserDevice>> devicesByUserAgent = new CompactMap<String, List<ToolUserDevice>>();

        for (ToolUserDevice device : Query.
                from(ToolUserDevice.class).
                where("user = ?", user).
                selectAll()) {
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
                List<ToolUserAction> a = Query.
                        from(ToolUserAction.class).
                        where("device = ?", d).
                        sortDescending("time").
                        selectAll();

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
            page.writeStart("div", "class", "widget p-tud");
                page.writeStart("h1");
                    page.writeHtml(user.getName());
                page.writeEnd();

                page.writeStart("div", "class", "tabbed tabbed-vertical");

                    // Site.
                    if (Query.from(Site.class).hasMoreThan(0)) {
                        Site currentSite = user.getCurrentSite();
                        List<Site> sites = new ArrayList<Site>();

                        for (Site site : Site.Static.findAll()) {
                            if (page.hasPermission(site.getPermissionId())) {
                                sites.add(site);
                            }
                        }

                        if (sites.size() > 1) {
                            page.writeStart("div",
                                    "class", "p-tud-site",
                                    "data-tab", "Site: " + (currentSite != null ? currentSite.getLabel() : "Global"));

                                page.writeStart("ul", "class", "links");
                                    if (page.hasPermission("site/global")) {
                                        page.writeStart("li");
                                            page.writeStart("a", "href", page.url("", "tab", "site"));
                                                page.writeHtml("Global");
                                            page.writeEnd();
                                        page.writeEnd();
                                    }

                                    for (Site site : sites) {
                                        page.writeStart("li");
                                            page.writeStart("a", "href", page.url("", "tab", "site", "id", site.getId()));
                                                page.writeObjectLabel(site);
                                            page.writeEnd();
                                        page.writeEnd();
                                    }
                                page.writeEnd();
                            page.writeEnd();
                        }
                    }

                    // Profile.
                    page.writeStart("div",
                            "class", "p-tud-profile",
                            "data-tab", "Profile");

                        page.writeStart("ul", "class", "piped");
                            page.writeStart("li");
                                page.writeStart("a",
                                        "class", "icon icon-key",
                                        "href", page.cmsUrl("/toolUserTfa"),
                                        "target", "toolUserTfa");
                                    page.writeHtml(user.isTfaEnabled() ? "Disable" : "Enable");
                                    page.writeHtml(" Two Factor Authentication");
                                page.writeEnd();
                            page.writeEnd();
                        page.writeEnd();

                        page.include("/WEB-INF/errors.jsp");

                        page.writeStart("form",
                                "method", "post",
                                "enctype", "multipart/form-data",
                                "action", page.objectUrl("", user));

                            page.writeElement("input",
                                    "type", "hidden",
                                    "name", "tab",
                                    "value", "profile");

                            page.writeStart("div", "class", "fixedScrollable");
                                page.include("/WEB-INF/objectForm.jsp", "object", user, "excludeFields", excludeFields);
                            page.writeEnd();

                            page.writeStart("div", "class", "actions");
                                page.writeStart("button", "class", "icon icon-action-save");
                                    page.writeHtml("Save");
                                page.writeEnd();
                            page.writeEnd();
                        page.writeEnd();
                    page.writeEnd();

                    // Looking glass.
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
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }
}
