package com.psddev.cms.tool.widget;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import com.psddev.cms.db.Site;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DashboardWidget;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.util.ObjectUtils;

public class ResourcesWidget extends DashboardWidget {

    @Override
    public void writeHtml(ToolPageContext page, Dashboard dashboard) throws IOException, ServletException {
        List<CmsTool.ResourceItem> resources = null;
        Site site = page.getSite();

        if (site != null) {
            resources = site.getResources();
        }

        if (resources == null || resources.isEmpty()) {
            resources = page.getCmsTool().getResources();
        }

        if (resources != null) {
            for (Iterator<CmsTool.ResourceItem> i = resources.iterator(); i.hasNext();) {
                if (ObjectUtils.isBlank(i.next().getUrl())) {
                    i.remove();
                }
            }
        }

        page.writeStart("div", "class", "widget");
            page.writeStart("h1", "class", "icon icon-globe");
                page.writeHtml(page.localize(ResourcesWidget.class, "title"));
            page.writeEnd();

            if (resources == null || resources.isEmpty()) {
                page.writeStart("div", "class", "message message-info");
                    page.writeHtml(page.localize(ResourcesWidget.class, "message.noResources"));
                page.writeEnd();

            } else {
                page.writeStart("ul", "class", "links");
                    for (CmsTool.ResourceItem item : resources) {
                        String url = item.getUrl();

                        if (!ObjectUtils.isBlank(url)) {
                            page.writeStart("li");
                                page.writeStart("a",
                                        "href", url,
                                        "target", item.isSameWindow() ? "_top" : "_blank");
                                    page.writeHtml(item.getName());
                                page.writeEnd();
                            page.writeEnd();
                        }
                    }
                page.writeEnd();
            }
        page.writeEnd();
    }
}
