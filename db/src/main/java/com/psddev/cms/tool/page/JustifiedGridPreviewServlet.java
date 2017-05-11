package com.psddev.cms.tool.page;

import com.psddev.cms.db.ImageTag;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.file.SvgFileType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ImageEditor;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StorageItem;
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
        Recordable recordable = (Recordable)asset;

        // TODO: We want ObjectTypes to be able to define their own preview response... but for now, something generic
        page.writeStart("div", "class", "image");
            page.writeElement("img",
                            "src", getPreviewUrl(recordable),
                            "alt", page.getObjectLabel(recordable));
        page.writeEnd();
        page.writeStart("div", "class", "metadata");
            page.writeStart("div", "class", "field", "data-field", "uuid");
                page.writeStart("span", "class", "label");
                    page.writeHtml("UUID");
                page.writeEnd();
                page.writeStart("span", "class", "value");
                    page.writeHtml(recordable.getState().getId());
                page.writeEnd();
            page.writeEnd();
            page.writeStart("div", "class", "field", "data-field", "label");
                page.writeStart("span", "class", "label");
                    page.writeHtml("Label");
                page.writeEnd();
                page.writeStart("span", "class", "value");
                    page.writeHtml(recordable.getState().getLabel());
                page.writeEnd();
            page.writeEnd();
        page.writeEnd();
    }

    public String getPreviewUrl(Object object) {
        if (object != null) {

            StorageItem preview = object instanceof StorageItem
                    ? (StorageItem) object
                    : State.getInstance(object).getPreview();

            if (preview != null) {

                String contentType = preview.getContentType();

                if (ImageEditor.Static.getDefault() != null
                        && (contentType != null && !contentType.equals(SvgFileType.CONTENT_TYPE))) {

                    return new ImageTag.Builder(preview)
                            //.setHeight(300)
                            //.setResizeOption(ResizeOption.ONLY_SHRINK_LARGER)
                            .toUrl();

                } else {
                    return preview.getPublicUrl();
                }
            }
        }

        return null;
    }

}
