package com.psddev.cms.tool.page;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.GridPreviewRenderer;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.RoutingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by rhseeger on 5/10/17.
 */
@RoutingFilter.Path(application = "cms", value = "/gridPreview")
public class JustifiedGridPreviewServlet extends PageServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(JustifiedGridPreviewServlet.class);

    public static final String UUID_PARAM = "uuid";

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {

        ToolUser user = page.getUser();
        UUID assetId = page.param(UUID.class, UUID_PARAM);
        if (assetId == null) {
            throw new ServletException("Required parameter [" + UUID_PARAM + "] not provided");
        }

        Object asset = Query.fromAll().where("id = ?", assetId).first();
        if (asset == null) {
            throw new ServletException("Asset with id [" + UUID_PARAM + "] does not exist");
        }

        if (!(asset instanceof Recordable)) {
            throw new ServletException("Asset with id [" + UUID_PARAM + "] is not Recordable, cannot be previewed");
        }

        // TODO: Check that the user has permission to view the asset in question

        Recordable recordable = (Recordable)asset;

        if (recordable instanceof GridPreviewRenderer.CustomGridPreviewRenderer) {
            ((GridPreviewRenderer.CustomGridPreviewRenderer) recordable).getGridPreviewRenderer().renderPreview(page);
        } else {
            new GridPreviewRenderer(recordable).renderPreview(page);
        }
    }
}
