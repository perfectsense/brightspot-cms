package com.psddev.cms.tool.page;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.cms.db.ExternalContent;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "/content/externalPreviewFrame")
@SuppressWarnings("serial")
public class ContentExternalPreviewFrame extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(final ToolPageContext page) throws IOException, ServletException {
        ExternalContent content = new ExternalContent();

        content.setUrl(page.param(String.class, "url"));

        boolean noResponse = content.getResponse() == null;

        page.writeTag("!doctype html");
        page.writeStart("html");
            page.writeStart("head");
                if (noResponse) {
                    page.writeStylesAndScripts();

                    page.writeStart("style", "type", "text/css");
                    page.writeCss("body",
                            "background", "transparent",
                            "margin", "10px");
                    page.writeEnd();
                }

                page.writeStart("script",
                        "type", "text/javascript",
                        "src", page.cmsUrl("/script/iframeResizer.contentWindow.js"));
                page.writeEnd();
            page.writeEnd();

            page.writeStart("body");
                if (noResponse) {
                    page.writeStart("div", "class", "message message-info");
                        page.writeHtml(page.localize(ContentExternalPreviewFrame.class, "error.noPreview"));
                    page.writeEnd();

                } else {
                    content.renderObject(page.getRequest(), page.getResponse(), page);
                }
            page.writeEnd();
        page.writeEnd();
    }
}
