package com.psddev.cms.tool.page.content.field;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.psddev.cms.db.ImageTag;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.FileContentType;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.file.ContentTypeValidator;
import com.psddev.cms.tool.file.MetadataAfterSave;
import com.psddev.cms.tool.file.MetadataBeforeSave;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RandomUuidStorageItemPathGenerator;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StorageItemFilter;
import com.psddev.dari.util.StorageItemUploadPart;
import com.psddev.dari.util.StringUtils;

@RoutingFilter.Path(application = "cms", value = "/content/field/file")
public class FileField extends PageServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileField.class);

    public static void processField(ToolPageContext page) throws IOException, ServletException {

        HttpServletRequest request = page.getRequest();

        State state = State.getInstance(request.getAttribute("object"));

        ObjectField field = (ObjectField) request.getAttribute("field");

        String inputName = ObjectUtils.firstNonBlank((String) request.getAttribute("inputName"), page.param(String.class, "inputName"));
        String actionName = inputName + ".action";
        String fileParamName = inputName + ".file";
        String fileJsonParamName = fileParamName + ".json";
        String urlName = inputName + ".url";
        String dropboxName = inputName + ".dropbox";

        String fieldName = field != null ? field.getInternalName() : page.param(String.class, "fieldName");
        StorageItem fieldValue = null;

        if (state != null) {
            fieldValue = (StorageItem) state.get(fieldName);
        } else if (page.isAjaxRequest()) {
            // Handles requests from front end upload
            UUID typeId = page.param(UUID.class, "typeId");
            ObjectType type = ObjectType.getInstance(typeId);
            field = type.getField(fieldName);
            state = State.getInstance(type.createObject(null));
            fieldValue = StorageItemFilter.getParameter(request, fileJsonParamName, getStorageSetting(Optional.ofNullable(field)));
            request.setAttribute("object", state);
            request.setAttribute("field", field);
        }

        String action = page.param(String.class, actionName);
        boolean isFormPost = request.getAttribute("isFormPost") != null ? (Boolean) request.getAttribute("isFormPost") : false;

        Class hotSpotClass = ObjectUtils.getClassByName(ImageTag.HOTSPOT_CLASS);
        boolean projectUsingBrightSpotImage = hotSpotClass != null && !ObjectUtils.isBlank(ClassFinder.Static.findClasses(hotSpotClass));

        if (isFormPost) {

            StorageItem newItem = null;

            if ("keep".equals(action)) {
                newItem = StorageItemFilter.getParameter(request, fileJsonParamName, getStorageSetting(Optional.ofNullable(field)));

            } else if ("newUpload".equals(action)) {
                newItem = StorageItemFilter.getParameter(request, fileParamName, getStorageSetting(Optional.ofNullable(field)));

            } else if ("dropbox".equals(action)) {
                Map<String, Object> fileData = (Map<String, Object>) ObjectUtils.fromJson(page.param(String.class, dropboxName));

                if (fileData != null) {
                    File file = null;
                    try {
                        file = File.createTempFile("cms.", ".tmp");
                        String name = ObjectUtils.to(String.class, fileData.get("name"));
                        String fileContentType = ObjectUtils.getContentType(name);
                        long fileSize = ObjectUtils.to(long.class, fileData.get("bytes"));

                        try (InputStream fileInput = new URL(ObjectUtils.to(String.class, fileData.get("link"))).openStream();
                             FileOutputStream fileOutput = new FileOutputStream(file)) {

                            IoUtils.copy(fileInput, fileOutput);
                        }

                        StorageItemUploadPart part = new StorageItemUploadPart();
                        part.setName(name);
                        part.setFile(file);
                        part.setContentType(fileContentType);

                        if (name != null
                                && fileContentType != null) {
                            new ContentTypeValidator().beforeSave(null, part);
                        }

                        if (fileSize > 0) {

                            newItem = StorageItem.Static.createIn(getStorageSetting(Optional.of(field)));
                            newItem.setPath(new RandomUuidStorageItemPathGenerator().createPath(name));
                            newItem.setContentType(fileContentType);
                            newItem.setData(new FileInputStream(file));

                            new MetadataBeforeSave().beforeSave(newItem, part);
                            newItem.save();
                            new MetadataAfterSave().afterSave(newItem);
                        }

                    } finally {
                        if (file != null && file.exists()) {
                            file.delete();
                        }
                    }
                }
            } else if ("newUrl".equals(action)) {
                newItem = StorageItem.Static.createUrl(page.param(String.class, urlName));
            }

            // Do additional processing specific to content type
            if (newItem != null) {
                FileContentType contentType = FileContentType.getFileContentType(newItem);
                if (contentType != null) {
                    contentType.process(page, newItem);
                }
            }

            state.put(fieldName, newItem);

            if (projectUsingBrightSpotImage) {
                page.include("/WEB-INF/field/set/hotSpot.jsp");
            }
            return;

        }

        // --- Presentation ---
        page.writeStart("div", "class", "inputSmall");

            page.writeStart("div", "class", "fileSelector");

                page.writeStart("select",
                        "class", "toggleable",
                        "data-root", ".inputSmall",
                        "name", page.h(actionName));

                    if (fieldValue != null) {
                        page.writeStart("option",
                                "data-hide", ".fileSelectorItem",
                                "data-show", ".fileSelectorExisting",
                                "value", "keep");
                            page.writeHtml(page.localize(FileField.class, "option.keep"));
                        page.writeEnd();
                    }

                    page.writeStart("option",
                            "data-hide", ".fileSelectorItem",
                            "value", "none");
                        page.writeHtml(page.localize(FileField.class, "option.none"));
                    page.writeEnd();

                    page.writeStart("option",
                            "data-hide", ".fileSelectorItem",
                            "data-show", ".fileSelectorNewUpload",
                            "value", "newUpload",
                            fieldValue == null && field.isRequired() ? " selected" : "");
                        page.writeHtml(page.localize(FileField.class, "option.newUpload"));
                    page.writeEnd();

                    page.writeStart("option",
                            "data-hide", ".fileSelectorItem",
                            "data-show", ".fileSelectorNewUrl",
                            "value", "newUrl");
                        page.writeHtml(page.localize(FileField.class, "option.newUrl"));
                    page.writeEnd();

                    if (!ObjectUtils.isBlank(page.getCmsTool().getDropboxApplicationKey())) {
                        page.writeStart("option",
                                "data-hide", ".fileSelectorItem",
                                "data-show", ".fileSelectorDropbox",
                                "value", "dropbox");
                            page.write("Dropbox");
                        page.writeEnd();
                    }
                page.writeEnd();

                page.writeTag("input",
                        "class", "fileSelectorItem fileSelectorNewUpload",
                        "type", "file",
                        page.getCmsTool().isEnableFrontEndUploader() ? "data-bsp-uploader" : "", "",
                        "name", page.h(fileParamName),
                        "data-input-name", inputName,
                        "data-type-id", state.getTypeId());

                page.writeTag("input",
                        "class", "fileSelectorItem fileSelectorNewUrl",
                        "type", "text",
                        "name", page.h(urlName));

                if (fieldValue != null) {
                    page.writeTag("input",
                            "type", "hidden",
                            "name", fileJsonParamName,
                            "value", ObjectUtils.toJson(fieldValue));
                }

                if (!ObjectUtils.isBlank(page.getCmsTool().getDropboxApplicationKey())) {
                    page.writeStart("span", "class", "fileSelectorItem fileSelectorDropbox", "style", page.cssString("display", "inline-block", "vertical-align", "bottom"));
                        page.writeTag("input",
                                "type", "dropbox-chooser",
                                "name", page.h(dropboxName),
                                "data-link-type", "direct",
                                "style", page.cssString("visibility", "hidden"));
                    page.writeEnd();

                    page.writeStart("script", "type", "text/javascript");
                        page.writeRaw(
                                "$('.fileSelectorDropbox input').on('DbxChooserSuccess', function(event) {\n"
                                        + "   $(this).val(JSON.stringify(event.originalEvent.files[0]));\n"
                                        + "});"
                        );
                    page.writeEnd();
                }
            page.writeEnd();

            if (fieldValue != null) {

                page.writeStart("div",
                        "class", "fileSelectorItem fileSelectorExisting filePreview");

                    if (field.as(ToolUi.class).getStoragePreviewProcessorApplication() != null) {

                        ToolUi ui = field.as(ToolUi.class);
                        String processorPath = ui.getStoragePreviewProcessorPath();
                        if (processorPath != null) {
                            JspUtils.include(request, page.getResponse(), page.getWriter(),
                                    RoutingFilter.Static.getApplicationPath(ui.getStoragePreviewProcessorApplication())
                                            + StringUtils.ensureStart(processorPath, "/"));
                        }
                    } else {
                        FileContentType.writeFilePreview(page, state, fieldValue);
                    }
                page.writeEnd();
            }
        page.writeEnd();

        if (projectUsingBrightSpotImage) {
            page.include("/WEB-INF/field/set/hotSpot.jsp");
        }
    }

    /**
     * Gets storageSetting for current field,
     * if non exists, get {@code StorageItem.DEFAULT_STORAGE_SETTING}
     *
     * @param field to check for storage setting
     */
    public static String getStorageSetting(Optional<ObjectField> field) {
        String storageSetting = null;

        if (field.isPresent()) {
            String fieldStorageSetting = field.get().as(ToolUi.class).getStorageSetting();
            if (!StringUtils.isBlank(fieldStorageSetting)) {
                storageSetting = Settings.get(String.class, fieldStorageSetting);
            }
        }

        if (StringUtils.isBlank(storageSetting)) {
            storageSetting = Settings.get(String.class, StorageItem.DEFAULT_STORAGE_SETTING);
        }

        return storageSetting;
    }

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        processField(page);
    }
}
