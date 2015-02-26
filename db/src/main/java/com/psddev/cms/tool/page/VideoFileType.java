package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;

public class VideoFileType implements FileContentType {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolPageContext.class);

    @Override
    public boolean isSupported(StorageItem storageItem) {
        String contentType = storageItem.getContentType();
        return !StringUtils.isBlank(contentType) && contentType.startsWith("video/");
    }

    @Override
    public boolean isPreferred(StorageItem storageItem) {
        return false;
    }

    @Override
    public void writePreview(ToolPageContext page) throws IOException, ServletException {
        HttpServletRequest request = page.getRequest();
        State state = State.getInstance(request.getAttribute("object"));
        ObjectField field = (ObjectField) request.getAttribute("field");
        String inputName = ObjectUtils.firstNonBlank(page.param(String.class, "inputName"), (String) request.getAttribute("inputName"));
        String storageName = inputName + ".storage";
        String pathName = inputName + ".path";

        String fieldName = field != null ? field.getInternalName() : page.paramOrDefault(String.class, "fieldName", "");
        StorageItem fieldValue = null;

        //TODO: move somewhere reusable?
        //handle inline upload display
        if (page.paramOrDefault(Boolean.class, "isNewUpload", false)) {
            String storageItemPath = page.param(String.class, pathName);
            if (!StringUtils.isBlank(storageItemPath)) {
                StorageItem newItem = StorageItem.Static.createIn(page.param(storageName));
                newItem.setPath(page.param(pathName));
                //newItem.setContentType(page.param(contentTypeName));
                fieldValue = newItem;
            }

            state = State.getInstance(ObjectType.getInstance(page.param(UUID.class, "typeId")));
        }

        if (fieldValue == null) {
            fieldValue = (StorageItem) state.getValue(fieldName);
        }

        if (fieldValue == null) {
            return;
        }

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

    @Override
    public void setMetadata(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException {

    }
}
