package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.Site;
import com.psddev.cms.db.SiteCategory;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;

@RoutingFilter.Path(application = "cms", value = "/siteSwitchResults")
public class SiteSwitchResults extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {

        ToolUser user = page.getUser();
        Site currentSite = user.getCurrentSite();
        List<Site> sites = user.findOtherAccessibleSites();

        UUID siteCategoryId = page.param(UUID.class, "siteCategory");

        // Filter sites by site category and query string (if necessary)
        final SiteCategory siteCategory = siteCategoryId != null ? Query.findById(SiteCategory.class, siteCategoryId) : null;
        final String queryString = page.param(String.class, "query");

        if (siteCategory != null) {
            sites.removeIf(s -> !siteCategory.equals(s.getSiteCategory()));

        } else {
            sites.removeIf(s -> s.getSiteCategory() != null);
        }

        if (!StringUtils.isBlank(queryString)) {
            sites.removeIf(s -> !s.is("name != missing && name ^=[c] ?", queryString));
        }

        page.writeStart("ul", "class", "links");
            if (currentSite != null && page.hasPermission("site/global") && siteCategoryId == null) {
                page.writeStart("li");
                    page.writeStart("a",
                            "href", page.cmsUrl("/siteSwitch", "switch", true),
                            "target", "_top");
                        page.writeHtml(page.localize(SiteSwitch.class, "label.global"));
                    page.writeEnd();
                page.writeEnd();
            }

            for (Site site : sites) {
                page.writeStart("li");
                    page.writeStart("a",
                            "href", page.cmsUrl("/siteSwitch", "switch", true, "id", site.getId()),
                            "target", "_top");
                        page.writeObjectLabel(site);
                    page.writeEnd();
                page.writeEnd();
            }
        page.writeEnd();
    }
}
