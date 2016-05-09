package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "enhancementSelect")
public class EnhancementSelect extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Set<UUID> validTypeIds = new HashSet<UUID>();

        for (ObjectType type : Database.Static.getDefault().getEnvironment().getTypes()) {
            if (type.as(ToolUi.class).isReferenceable()) {
                validTypeIds.add(type.getId());
            }
        }

        JspUtils.include(
                page.getRequest(),
                page.getResponse(),
                page,
                page.toolPath(CmsTool.class, "/WEB-INF/search.jsp"),
                        "newJsp", "/content/edit.jsp",
                        "newTarget", "objectId-create",
                        "resultJsp", "/enhancementSearchResult",
                        "validTypeIds", validTypeIds.toArray(new UUID[validTypeIds.size()]));
    }
}
