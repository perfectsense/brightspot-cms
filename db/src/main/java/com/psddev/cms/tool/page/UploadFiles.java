package com.psddev.cms.tool.page;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.psddev.cms.db.BulkUploadDraft;
import com.psddev.cms.db.ImageTag;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.Variation;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectFieldComparator;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.State;
import com.psddev.dari.util.AggregateException;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.ImageMetadataMap;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.MultipartRequest;
import com.psddev.dari.util.MultipartRequestFilter;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.SparseSet;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;

@RoutingFilter.Path(application = "cms", value = "/content/uploadFiles")
@SuppressWarnings("serial")
public class UploadFiles extends PageServlet {

    private static final String CONTAINER_ID_PARAMETER = "containerId";
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadFiles.class);

    @Override
    protected String getPermissionId() {
        return "area/dashboard";
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        if (page.paramOrDefault(Boolean.class, "writeInputsOnly", false)) {
            writeFileInput(page);
        } else {
            reallyDoService(page);
        }
    }

    private static void reallyDoService(ToolPageContext page) throws IOException, ServletException {
        Database database = Database.Static.getDefault();
        DatabaseEnvironment environment = database.getEnvironment();
        Exception postError = null;
        ObjectType selectedType = environment.getTypeById(page.param(UUID.class, "type"));
        String containerId = page.param(String.class, "containerId");

        if (page.isFormPost()) {
            database.beginWrites();

            try {
                MultipartRequest request = MultipartRequestFilter.Static.getInstance(page.getRequest());

                if (request == null) {
                    throw new IllegalStateException("Not multipart!");
                }

                ErrorUtils.errorIfNull(selectedType, "type");

                ObjectField previewField = getPreviewField(selectedType);

                if (previewField == null) {
                    throw new IllegalStateException("No file field!");
                }

                String inputName = ObjectUtils.firstNonBlank(page.param(String.class, "inputName"), (String) page.getRequest().getAttribute("inputName"), "file");
                String pathName = inputName + ".path";
                List<String> paths = page.params(String.class, pathName);
                List<StorageItem> newStorageItems = new ArrayList<>();
                FileItem[] files = request.getFileItems("file");
                StringBuilder js = new StringBuilder();
                Object common = selectedType.createObject(page.param(UUID.class, "typeForm-" + selectedType.getId()));
                page.updateUsingParameters(common);

                if (!ObjectUtils.isBlank(paths)) {
                    //get existing storage item
                    for (String path : paths) {
                        String defaultStorageSetting = Settings.get(String.class, StorageItem.DEFAULT_STORAGE_SETTING);
                        String fieldStorageSetting = previewField.as(ToolUi.class).getStorageSetting();
                        StorageItem newStorageItem = StorageItem.Static.createIn(defaultStorageSetting);
                        newStorageItem.setPath(path);
                        if (!StringUtils.isBlank(fieldStorageSetting) && !fieldStorageSetting.equals(defaultStorageSetting)) {
                            newStorageItem = StorageItem.Static.copy(newStorageItem, fieldStorageSetting);
                        }

                        newStorageItems.add(newStorageItem);

                    }
                } else {
                    if (files != null && files.length > 0) {

                        for (FileItem file : files) {

                            // Checks to make sure the file's content type is valid
                            String groupsPattern = Settings.get(String.class, "cms/tool/fileContentTypeGroups");
                            Set<String> contentTypeGroups = new SparseSet(ObjectUtils.isBlank(groupsPattern) ? "+/" : groupsPattern);

                            if (!contentTypeGroups.contains(file.getContentType())) {
                                page.getErrors().add(new IllegalArgumentException(String.format(
                                        "Invalid content type [%s]. Must match the pattern [%s].",
                                        file.getContentType(), contentTypeGroups)));
                                continue;
                            }

                            // Disallow HTML disguising as other content types per:
                            // http://www.adambarth.com/papers/2009/barth-caballero-song.pdf
                            if (!contentTypeGroups.contains("text/html")) {
                                InputStream input = file.getInputStream();

                                try {
                                    byte[] buffer = new byte[1024];
                                    String data = new String(buffer, 0, input.read(buffer)).toLowerCase(Locale.ENGLISH);
                                    String ptr = data.trim();

                                    if (ptr.startsWith("<!") ||
                                            ptr.startsWith("<?") ||
                                            data.startsWith("<html") ||
                                            data.startsWith("<script") ||
                                            data.startsWith("<title") ||
                                            data.startsWith("<body") ||
                                            data.startsWith("<head") ||
                                            data.startsWith("<plaintext") ||
                                            data.startsWith("<table") ||
                                            data.startsWith("<img") ||
                                            data.startsWith("<pre") ||
                                            data.startsWith("text/html") ||
                                            data.startsWith("<a") ||
                                            ptr.startsWith("<frameset") ||
                                            ptr.startsWith("<iframe") ||
                                            ptr.startsWith("<link") ||
                                            ptr.startsWith("<base") ||
                                            ptr.startsWith("<style") ||
                                            ptr.startsWith("<div") ||
                                            ptr.startsWith("<p") ||
                                            ptr.startsWith("<font") ||
                                            ptr.startsWith("<applet") ||
                                            ptr.startsWith("<meta") ||
                                            ptr.startsWith("<center") ||
                                            ptr.startsWith("<form") ||
                                            ptr.startsWith("<isindex") ||
                                            ptr.startsWith("<h1") ||
                                            ptr.startsWith("<h2") ||
                                            ptr.startsWith("<h3") ||
                                            ptr.startsWith("<h4") ||
                                            ptr.startsWith("<h5") ||
                                            ptr.startsWith("<h6") ||
                                            ptr.startsWith("<b") ||
                                            ptr.startsWith("<br")) {
                                        page.getErrors().add(new IllegalArgumentException(String.format(
                                                "Can't upload [%s] file disguising as HTML!",
                                                file.getContentType())));
                                        continue;
                                    }

                                } finally {
                                    input.close();
                                }
                            }

                            if (file.getSize() == 0) {
                                continue;
                            }

                            String fileName = file.getName();
                            String path = StorageItemField.createStorageItemPath(fileName, null);

                            Map<String, List<String>> httpHeaders = new LinkedHashMap<String, List<String>>();

                            httpHeaders.put("Cache-Control", Collections.singletonList("public, max-age=31536000"));
                            httpHeaders.put("Content-Length", Collections.singletonList(String.valueOf(file.getSize())));
                            httpHeaders.put("Content-Type", Collections.singletonList(file.getContentType()));

                            String storageSetting = previewField.as(ToolUi.class).getStorageSetting();
                            StorageItem item = StorageItem.Static.createIn(storageSetting != null ? Settings.getOrDefault(String.class, storageSetting, null) : null);
                            String contentType = file.getContentType();

                            item.setPath(path);
                            item.setContentType(contentType);
                            item.getMetadata().put("http.headers", httpHeaders);
                            item.getMetadata().put("originalFilename", fileName);
                            item.setData(file.getInputStream());

                            if (contentType != null && contentType.startsWith("image/")) {
                                InputStream fileInput = file.getInputStream();

                                try {
                                    ImageMetadataMap metadata = new ImageMetadataMap(fileInput);
                                    List<Throwable> errors = metadata.getErrors();

                                    item.getMetadata().putAll(metadata);

                                    if (!errors.isEmpty()) {
                                        LOGGER.info("Can't read image metadata!", new AggregateException(errors));
                                    }

                                } finally {
                                    IoUtils.closeQuietly(fileInput);
                                }
                            }

                            newStorageItems.add(item);
                        }
                    }
                }

                if (!ObjectUtils.isBlank(newStorageItems)) {
                    for (StorageItem item : newStorageItems) {
                        if (item == null) {
                            continue;
                        }

                        item.save();

                        Object object = selectedType.createObject(null);
                        State state = State.getInstance(object);

                        state.setValues(State.getInstance(common));

                        Site site = page.getSite();

                        if (site != null &&
                                site.getDefaultVariation() != null) {
                            state.as(Variation.Data.class).setInitialVariation(site.getDefaultVariation());
                        }

                        state.put(previewField.getInternalName(), item);
                        state.as(BulkUploadDraft.class).setContainerId(containerId);
                        page.publish(state);

                        js.append("$addButton.repeatable('add', function() {");
                        js.append("var $added = $(this);");
                        js.append("$input = $added.find(':input.objectId').eq(0);");
                        js.append("$input.attr('data-label', '").append(StringUtils.escapeJavaScript(state.getLabel())).append("');");
                        js.append("$input.attr('data-preview', '").append(StringUtils.escapeJavaScript(page.getPreviewThumbnailUrl(object))).append("');");
                        js.append("$input.val('").append(StringUtils.escapeJavaScript(state.getId().toString())).append("');");
                        js.append("$input.change();");
                        js.append("});");
                    }

                    database.commitWrites();
                }

                if (page.getErrors().isEmpty()) {
                    page.writeStart("div", "id", page.createId()).writeEnd();

                    page.writeStart("script", "type", "text/javascript");
                        page.write("if (typeof jQuery !== 'undefined') (function($, win, undef) {");
                            page.write("var $page = $('#" + page.getId() + "'),");
                            page.write("$init = $page.popup('source').repeatable('closestInit'),");
                            page.write("$addButton = $init.find('.addButton').eq(0),");
                            page.write("$input;");
                            page.write("if ($addButton.length > 0) {");
                                page.write(js.toString());
                                page.write("$page.popup('close');");
                            page.write("} else {");
                                page.write("win.location.reload();");
                            page.write("}");
                        page.write("})(jQuery, window);");
                    page.writeEnd();

                    return;
                }

            } catch (Exception error) {
                postError = error;

            } finally {
                database.endWrites();
            }
        }

        Set<ObjectType> typesSet = new HashSet<ObjectType>();

        for (UUID typeId : page.params(UUID.class, "typeId")) {
            ObjectType type = environment.getTypeById(typeId);

            if (type != null) {
                for (ObjectType t : type.as(ToolUi.class).findDisplayTypes()) {
                    for (ObjectField field : t.getFields()) {
                        if (ObjectField.FILE_TYPE.equals(field.getInternalItemType())) {
                            typesSet.add(t);
                            break;
                        }
                    }
                }
            }
        }

        List<ObjectType> types = new ArrayList<ObjectType>(typesSet);
        Collections.sort(types, new ObjectFieldComparator("name", false));
        Uploader uploader = Uploader.getUploader(Optional.empty());

        page.writeStart("h1").writeHtml("Upload Files").writeEnd();

        page.writeStart("form",
                "method", "post",
                "enctype", "multipart/form-data",
                "action", page.url(null));

            page.writeElement("input",
                    "type", "hidden",
                    "name", CONTAINER_ID_PARAMETER,
                    "value", containerId);

            for (ObjectType type : types) {
                page.writeElement("input", "type", "hidden", "name", "typeId", "value", type.getId());
            }

            if (postError != null) {
                page.writeStart("div", "class", "message message-error");
                    page.writeObject(postError);
                page.writeEnd();

            } else if (!page.getErrors().isEmpty()) {
                page.writeStart("div", "class", "message message-error");
                    for (Throwable error : page.getErrors()) {
                        page.writeHtml(error.getMessage());
                    }
                page.writeEnd();
            }

            page.writeStart("div", "class", "inputContainer bulk-upload-files");
                page.writeStart("div", "class", "inputLabel");
                    page.writeStart("label", "for", page.createId()).writeHtml("Files").writeEnd();
                page.writeEnd();
                page.writeStart("div", "class", "inputSmall");
                    if (uploader != null) {
                        uploader.writeHtml(page, Optional.empty());
                    }
                    page.writeElement("input",
                            "id", page.getId(),
                            "class", uploader != null ? uploader.getClassIdentifier() : null,
                            "type", "file",
                            "name", "file",
                            "multiple", "multiple");
                page.writeEnd();
            page.writeEnd();

            page.writeStart("div", "class", "inputContainer");
                page.writeStart("div", "class", "inputLabel");
                    page.writeStart("label", "for", page.createId()).writeHtml("Type").writeEnd();
                page.writeEnd();
                page.writeStart("div", "class", "inputSmall");
                    page.writeStart("select",
                            "class", "toggleable",
                            "data-root", "form",
                            "id", page.getId(),
                            "name", "type");
                        for (ObjectType type : types) {
                            page.writeStart("option",
                                    "data-hide", ".typeForm",
                                    "data-show", ".typeForm-" + type.getId(),
                                    "selected", type.equals(selectedType) ? "selected" : null,
                                    "value", type.getId());
                                page.writeHtml(type.getDisplayName());
                            page.writeEnd();
                        }
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();

            for (ObjectType type : types) {
                String name = "typeForm-" + type.getId();
                Object common = type.createObject(null);

                page.writeStart("div", "class", "typeForm " + name);
                    page.writeElement("input",
                            "type", "hidden",
                            "name", name,
                            "value", State.getInstance(common).getId());

                    ObjectField previewField = getPreviewField(type);

                    List<String> excludedFields = null;
                    if (previewField != null) {
                        excludedFields = Arrays.asList(previewField.getInternalName());
                    }

                    page.writeSomeFormFields(common, false, null, excludedFields);
                page.writeEnd();
            }

            page.writeStart("div", "class", "buttons");
                page.writeStart("button", "name", "action-upload").writeHtml("Upload").writeEnd();
            page.writeEnd();

        page.writeEnd();
    }

    public static void writeFileInput(ToolPageContext page) throws IOException, ServletException {

        String inputName = ObjectUtils.firstNonBlank(page.param(String.class, "inputName"), (String) page.getRequest().getAttribute("inputName"), "file");
        String pathName = inputName + ".path";

        String path = page.param(String.class, pathName);
        if (ObjectUtils.isBlank(path)) {
            return;
        }

        HttpServletResponse response = page.getResponse();
        StorageItem newStorageItem = StorageItem.Static.createIn(Settings.get(String.class, StorageItem.DEFAULT_STORAGE_SETTING));
        newStorageItem.setPath(path);
        ImageTag.Builder imageTagBuilder = new ImageTag.Builder(newStorageItem);
        imageTagBuilder.setWidth(170);

        response.setContentType("text/html");
        page.writeStart("div");
            page.write(imageTagBuilder.toHtml());
            page.writeTag("input", "type", "hidden", "name", pathName, "value", page.h(path));
        page.writeEnd();
    }

    private static ObjectField getPreviewField(ObjectType type) {
        ObjectField previewField = type.getField(type.getPreviewField());

        if (previewField == null) {
            for (ObjectField field : type.getFields()) {
                if (ObjectField.FILE_TYPE.equals(field.getInternalItemType())) {
                    previewField = field;
                    break;
                }
            }
        }

        return previewField;
    }
}
