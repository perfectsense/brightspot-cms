package com.psddev.cms.tool.page;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "contentRaw")
@SuppressWarnings("serial")
public class ContentRaw extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {

        Object object = page.findOrReserve();

        page.writeHeader();
            page.writeStart("div", "class", "widget");
                page.writeStart("h1", "class", "icon icon-barcode");
                    page.writeHtml("Raw Data");
                page.writeEnd();

                page.writeStart("pre");
                    page.writeHtml(ObjectUtils.toJson(State.getInstance(object).getSimpleValues(), true));
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }
}
