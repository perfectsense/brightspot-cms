package com.psddev.cms.tool.widget;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DefaultDashboardWidget;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.page.UploadFiles;
import com.psddev.dari.db.ObjectType;

public class BulkUploadWidget extends DefaultDashboardWidget {

    @Override
    public int getColumnIndex() {
        return 1;
    }

    @Override
    public int getWidgetIndex() {
        return 1;
    }

    @Override
    public void writeHtml(ToolPageContext page, Dashboard dashboard) throws IOException, ServletException {
        CmsTool.BulkUploadSettings settings = null;
        Site site = page.getSite();

        if (site != null) {
            settings = site.getBulkUploadSettings();
        }

        if (settings == null) {
            settings = page.getCmsTool().getBulkUploadSettings();
        }

        ObjectType defaultType = settings != null ? settings.getDefaultType() : null;

        page.writeStart("div", "class", "widget uploadable");
            page.writeStart("h1", "class", "icon icon-action-upload");
                page.writeHtml(page.localize(BulkUploadWidget.class, "title"));
            page.writeEnd();
            page.writeStart("div", "class", "message message-info");

                if (ObjectType.getInstance(Content.class).as(ToolUi.class).findDisplayTypes().stream()
                        .noneMatch(type -> type.getField(type.as(ToolUi.class).getBulkUploadableField()) != null)) {

                    page.writeHtml(page.localize(BulkUploadWidget.class, "message.noTypes"));

                } else {
                    //TODO: LOCALIZE
                    String uploadFilesPath = page.getCmsTool().isEnableFrontEndUploader()
                            ? "/content/upload"
                            : "/content/uploadFiles";

                    page.writeHtml("Drag and drop or ");
                    page.writeStart("a",
                            "class", "uploadableLink",
                            "target", "uploadFiles",
                            "href", page.url(uploadFilesPath,
                                    "typeId", ObjectType.getInstance(Content.class).getId(),
                                    "type", defaultType != null ? defaultType.getId() : null),
                                    "context", UploadFiles.Context.GLOBAL);
                        page.writeHtml("select");
                    page.writeEnd();
                    page.writeHtml(" files.");
                }

            page.writeEnd();
        page.writeEnd();
    }
}
