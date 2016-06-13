package com.psddev.cms.tool.page;

import com.psddev.cms.db.Site;
import com.psddev.cms.db.SiteCategory;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.RoutingFilter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RoutingFilter.Path(application = "cms", value = "/siteSwitch")
public class SiteSwitch extends PageServlet {

    private static final String SITE_CATEGORY_INPUT_NAME = "siteCategory";

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {

        ToolUser user = page.getUser();

        if (page.param(boolean.class, "switch")) {

            user.setCurrentSite(Query.from(Site.class).where("_id = ?", page.param(UUID.class, "id")).first());
            user.save();
            JspUtils.redirect(page.getRequest(), page.getResponse(), page.cmsUrl("/"));
            return;
        }

        page.writeHeader();

        if (Query.from(Site.class).hasMoreThan(0)) {
            Site currentSite = user.getCurrentSite();

            List<Site> sites = user.findOtherAccessibleSites();

            // Only render the control if there is at least one Site to which the ToolUser can change
            // Case 1: ToolUser has access to at least one other Site (not including Global)
            // Case 2: ToolUser has access to at least one Site and the Global Site
            if (!sites.isEmpty() || (currentSite != null && page.hasPermission("site/global"))) {

                page.writeStart("div", "class", "widget");
                    page.writeStart("h1");
                        page.writeHtml(page.localize(SiteSwitch.class, "title"));
                    page.writeEnd();

                    page.writeStart("div", "class", "siteSwitch-content fixedScrollable");
                        page.writeStart("form",
                                "action", page.cmsUrl("/siteSwitchResults"),
                                "method", "get",
                                "data-bsp-autosubmit", "",
                                "target", "siteSwitchResults");

                            if (Query.from(SiteCategory.class).hasMoreThan(0)) {
                                page.writeStart("select",
                                        "data-searchable", true,
                                        "name", SITE_CATEGORY_INPUT_NAME,
                                        "style", "display: block;");

                                    page.writeStart("option", "value", "");
                                        page.writeHtml(page.localize(SiteSwitch.class, "label.noCategory"));
                                    page.writeEnd();

                                    for (SiteCategory siteCategory : Query.from(SiteCategory.class).selectAll()) {
                                        page.writeStart("option", "value", siteCategory.getId());
                                            page.writeHtml(siteCategory.getLabel());
                                        page.writeEnd();
                                    }
                                page.writeEnd();
                            }

                            page.writeStart("div", "class", "searchInput");
                                page.writeStart("label", "for", page.createId());
                                    page.write(page.localize(SiteSwitch.class, "label.search"));
                                page.writeEnd();
                                page.writeTag("input", "id", page.getId(),
                                        "class", "autoFocus",
                                        "name", "query",
                                        "type", "text",
                                        "value", "");
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("div", "class", "frame", "name", "siteSwitchResults");
                        page.writeEnd();

                page.writeEnd();
            }
        }
        page.writeFooter();
    }
}
