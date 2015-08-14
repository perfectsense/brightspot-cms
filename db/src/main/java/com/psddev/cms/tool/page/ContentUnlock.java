package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.ContentLock;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "contentUnlock")
public class ContentUnlock extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Object content = Query
                .fromAll()
                .where("_id = ?", page.param(UUID.class, "id"))
                .first();

        if (content != null) {
            ContentLock.Static.unlock(content, null, page.getUser());
        }

        String returnUrl = page.param(String.class, "returnUrl");

        if (ObjectUtils.isBlank(returnUrl)) {
            page.writeRaw("OK");

        } else {
            JspUtils.redirect(
                    page.getRequest(),
                    page.getResponse(),
                    page.param(String.class, "returnUrl"));
        }
    }
}
