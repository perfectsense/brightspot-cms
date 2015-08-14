package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "queryField")
public class QueryField extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        page.getRequest().setAttribute("containerObject", Query
                .fromAll()
                .where("_id = ?", page.param(UUID.class, "containerObjectId"))
                .first());

        page.writeHeader();
            page.writeStart("div", "class", "widget widget-queryField");
                JspUtils.include(
                        page.getRequest(),
                        page.getResponse(),
                        page,
                        page.toolPath(CmsTool.class, "/WEB-INF/search.jsp"),
                        "resultJsp", "/queryFieldResult");
            page.writeEnd();
        page.writeFooter();
    }
}
