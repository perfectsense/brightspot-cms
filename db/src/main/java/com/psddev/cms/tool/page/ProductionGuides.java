package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import com.psddev.cms.db.Guide;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "/misc/productionGuides")
public class ProductionGuides extends PageServlet {

    @Override
    protected String getPermissionId() {
        return "area/dashboard";
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException,
            ServletException {

        List<Guide> guides = Query.from(Guide.class).selectAll();

        page.writeStart("div", "class", "widget");

        page.writeStart("h1", "class", "icon icon-book");
        page.writeHtml("Production Guides");
        page.writeEnd();

        if (ObjectUtils.isBlank(guides)) {
            page.writeStart("div", "class", "message message-info");
            page.writeStart("p");
            page.writeHtml("There aren't any production guides.");
            page.writeEnd();
            page.writeEnd();
        } else {
            page.writeStart("table", "class",
                    "links table-striped pageThumbnails").writeStart("tbody");

            for (Guide guide : guides) {
                page.writeStart("tr");
                page.writeStart("td");
                page.write(" <a target=\"_blank\" href=\"", page.url(
                        "/content/guideType.jsp", "guideId", guide.getId()), "\">",
                        guide.getTitle(), "</a>");
                page.writeEnd();
                page.writeEnd();
            }

            page.writeEnd().writeEnd();
        }

        page.writeEnd();
    }

}
