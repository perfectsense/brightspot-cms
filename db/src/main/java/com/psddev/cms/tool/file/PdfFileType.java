package com.psddev.cms.tool.file;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.cms.tool.FileContentType;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.State;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;

public class PdfFileType implements FileContentType {

    @Override
    public double getPriority(StorageItem storageItem) {
        String contentType = storageItem.getContentType();

        if (StringUtils.isBlank(contentType)
                || (!contentType.equals("application/pdf") && !contentType.equals("application/x-pdf"))) {
            return DEFAULT_PRIORITY_LEVEL - 1;
        }

        return DEFAULT_PRIORITY_LEVEL;
    }

    @Override
    public void writePreview(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException {

        page.writeStart("a",
                "href", page.h(fieldValue.getPublicUrl()),
                "target", "_blank");
            page.write(page.h(fieldValue.getContentType()));
            page.write(": ");
            page.write(page.h(fieldValue.getPath()));
        page.writeEnd();
        page.writeStart("object",
                "style", "display:block;",
                "data", fieldValue.getPublicUrl(),
                "width", "200",
                "height", "200",
                "type", "application/pdf");
        page.writeEnd();
    }
}
