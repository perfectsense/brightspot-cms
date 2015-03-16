package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.ImageTag;
import com.psddev.cms.db.StandardImageSize;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StorageItem;

@RoutingFilter.Path(application = "cms", value = "contentImages")
public class ContentImages extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(final ToolPageContext page) throws IOException, ServletException {
        String imageJsonString = page.param(String.class, "data");
        StorageItem image;

        if (!ObjectUtils.isBlank(imageJsonString)) {
            Map<String, Object> imageJson = (Map<String, Object>) ObjectUtils.fromJson(imageJsonString);
            image = StorageItem.Static.createIn((String) imageJson.get("storage"));

            new ObjectMap(image).putAll(imageJson);

        } else {
            Object object = Query.fromAll().where("_id = ?", page.param(UUID.class, "id")).first();
            image = (StorageItem) State.getInstance(object).get(page.param(String.class, "field"));
        }

        page.writeHeader();
            page.writeStart("div", "class", "widget");
                page.writeStart("h1", "class", "icon icon-crop");
                    page.writeHtml("Resized Images");
                page.writeEnd();

                page.writeStart("select",
                        "class", "toggleable",
                        "data-root", ".widget",
                        "style", page.cssString(
                                "margin-bottom", "10px"));
                    for (StandardImageSize size : StandardImageSize.findAll()) {
                        page.writeStart("option",
                                "data-hide", ".resizedImage",
                                "data-show", ".resizedImage-" + size.getInternalName());
                            page.writeHtml(size.getDisplayName());
                        page.writeEnd();
                    }
                page.writeEnd();

                for (StandardImageSize size : StandardImageSize.findAll()) {
                    String url = new ImageTag.Builder(image).setStandardImageSize(size).toUrl();

                    page.writeStart("div", "class", "resizedImage resizedImage-" + size.getInternalName());
                        page.writeElement("img",
                                "src", url,
                                "style", page.cssString(
                                        "max-width", "100%"));

                        page.writeElement("br");
                        page.writeStart("textarea",
                                "class", "code",
                                "data-expandable-class", "code",
                                "readonly", "readonly",
                                "onclick", "this.select();");
                            page.writeHtml(url);
                        page.writeEnd();
                    page.writeEnd();
                }
            page.writeEnd();
        page.writeFooter();
    }
}
