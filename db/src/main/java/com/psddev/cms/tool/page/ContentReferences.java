package com.psddev.cms.tool.page;

import com.psddev.cms.tool.ContentEditSection;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.content.ReferencesWidget;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.RoutingFilter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;

/**
 * @deprecated Use {@link ReferencesWidget} instead.
 */
@Deprecated
@RoutingFilter.Path(application = "cms", value = "/content/references")
@SuppressWarnings("serial")
public class ContentReferences extends PageServlet {

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
            new ReferencesWidget().display(page, content, ContentEditSection.RIGHT);
        }
    }
}
