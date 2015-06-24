package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;

public class SiteTab extends ProfileWidgetTab {
    @Override
    public void writeHtml(ToolPageContext page) throws IOException, ServletException {

        ToolUser user = page.getUser();

        if ("site".equals(page.param(String.class, "tab"))) {
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
        }

        if (Query.from(Site.class).hasMoreThan(0)) {
            Site currentSite = user.getCurrentSite();
            List<Site> sites = new ArrayList<Site>();

            for (Site site : Site.Static.findAll()) {
                if (page.hasPermission(site.getPermissionId())) {
                    sites.add(site);
                }
            }

            if (sites.size() > 1 || (!sites.isEmpty() && page.hasPermission("site/global"))) {
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
    }
}
