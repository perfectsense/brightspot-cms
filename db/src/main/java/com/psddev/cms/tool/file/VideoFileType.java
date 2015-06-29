package com.psddev.cms.tool.file;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.cms.tool.FileContentType;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.State;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;

public class VideoFileType implements FileContentType {

    @Override
    public double getPriority(StorageItem storageItem) {
        String contentType = storageItem.getContentType();

        if (StringUtils.isBlank(contentType) || !contentType.startsWith("video/")) {
            return DEFAULT_PRIORITY_LEVEL - 1;
        }

        return DEFAULT_PRIORITY_LEVEL;
    }

    @Override
    public void writePreview(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException {

        String contentType = fieldValue.getContentType();

        page.writeStart("div", "style", page.cssString("margin-bottom", "5px"));
            page.writeStart("a",
                    "class", "icon icon-action-preview",
                    "href", fieldValue.getPublicUrl(),
                    "target", "_blank");
                page.writeHtml("View Original");
            page.writeEnd();
        page.writeEnd();

        page.writeStart("video",
                "controls", "controls",
                "preload", "auto");
            page.writeElement("source",
                    "type", contentType,
                    "src", fieldValue.getPublicUrl());
        page.writeEnd();
    }
}
