package com.psddev.cms.tool.page;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.UploadPlugin;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.State;
import com.psddev.dari.util.AggregateException;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.ImageMetadataMap;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.SparseSet;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;

@MultipartConfig
@RoutingFilter.Path(application = "cms", value = "storageItemField")
public class StorageItemField extends PageServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageItemField.class);
    public static final String FILE_SELECTOR_ITEM_CLASS = "fileSelectorItem";
    public static final String FILE_SELECTOR_EXISTING_CLASS = "fileSelectorExisting";
    public static final String FILE_SELECTOR_NEW_URL_CLASS = "fileSelectorNewUrl";
    public static final String FILE_SELECTOR_NEW_UPLOAD_CLASS = "fileSelectorNewUpload";
    public static final String FILE_SELECTOR_DROPBOX_CLASS = "fileSelectorDropbox";

    public static void reallyDoService(ToolPageContext page) throws IOException, ServletException {

        HttpServletRequest request = page.getRequest();
        Object object = request.getAttribute("object");
        State state = State.getInstance(object);

        ObjectField field = (ObjectField) request.getAttribute("field");
        String fieldName = field.getInternalName();
        StorageItem fieldValue = (StorageItem) state.getValue(fieldName);

        writeFileSelector(page);

        if (fieldValue != null) {
            FilePreview.reallyDoService(page);
        }
    }

    public static void doFormPost(ToolPageContext page) throws IOException, ServletException {
        HttpServletRequest request = page.getRequest();
        Object object = request.getAttribute("object");

        if (object == null) {
            return;
        }

        State state = State.getInstance(object);

        ObjectField field = (ObjectField) request.getAttribute("field");
        String fieldName = field.getInternalName();
        StorageItem fieldValue = (StorageItem) state.getValue(fieldName);

        String inputName = (String) page.getRequest().getAttribute("inputName");
        String actionName = inputName + ".action";
        String storageName = inputName + ".storage";
        String fileName = inputName + ".file";
        String urlName = inputName + ".url";
        String pathName = inputName + ".path";
        String dropboxName = inputName + ".dropbox";
        String contentTypeName = inputName + ".contentType";

        File file = null;
        Part filePart = request.getPart(fileName);

        try {
            String action = page.param(actionName);
            StorageItem newItem = null;

            if ("keep".equals(action)) {
                if (fieldValue != null) {
                    newItem = fieldValue;
                } else {
                    newItem = StorageItemField.createStorageItemFromPath(page.param(pathName), page.param(storageName));
                    newItem.setContentType(page.param(contentTypeName));
                }

            } else if ("newUpload".equals(action) ||
                    "dropbox".equals(action)) {
                file = File.createTempFile("cms.", ".tmp");

                if ("dropbox".equals(action)) {
                    Map<String, Object> fileData = (Map<String, Object>) ObjectUtils.fromJson(page.param(String.class, dropboxName));

                    if (fileData != null) {
                        String name = ObjectUtils.to(String.class, fileData.get("name"));
                        InputStream fileInput = new URL(ObjectUtils.to(String.class, fileData.get("link"))).openStream();

                        try {
                            FileOutputStream fileOutput = new FileOutputStream(file);

                            try {
                                IoUtils.copy(fileInput, fileOutput);

                            } finally {
                                fileOutput.close();
                            }

                        } finally {
                            fileInput.close();
                        }

                        String storageSetting = field.as(ToolUi.class).getStorageSetting();
                        newItem = createStorageItemFromPath(createStorageItemPath(name, state.getLabel()), storageSetting != null ? Settings.getOrDefault(String.class, storageSetting, null) : null);
                    }

                } else if (filePart != null) {
                    filePart.write(file.getName());
                    newItem = StorageItemField.createStorageItemFromPart(page, filePart, field, state);
                }

            } else if ("newUrl".equals(action)) {
                newItem = StorageItem.Static.createUrl(page.param(urlName));
            }

            FilePreview.setMetadata(page, state, newItem);

            if (newItem != null &&
                    ("newUpload".equals(action) ||
                            "dropbox".equals(action))) {
                newItem.save();
            }

            state.putValue(fieldName, newItem);

        } finally {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    public static void writeFileSelector(ToolPageContext page) throws IOException, ServletException {
        HttpServletRequest request = page.getRequest();
        Object object = request.getAttribute("object");
        State state = State.getInstance(object);

        ObjectField field = (ObjectField) request.getAttribute("field");
        String fieldName = field.getInternalName();
        StorageItem fieldValue = (StorageItem) state.getValue(fieldName);

        String inputName = ObjectUtils.firstNonBlank(page.param(String.class, "inputName"),  (String) request.getAttribute("inputName"));
        String actionName = inputName + ".action";
        String fileName = inputName + ".file";
        String urlName = inputName + ".url";
        String dropboxName = inputName + ".dropbox";
        String pluginIdentifier = "";
        String storageSetting = getStorageSetting(field);

        for (Class<? extends UploadPlugin> pluginClass : ClassFinder.Static.findClasses(UploadPlugin.class)) {
            if (!pluginClass.isInterface() && !Modifier.isAbstract(pluginClass.getModifiers())) {
                UploadPlugin plugin = TypeDefinition.getInstance(pluginClass).newInstance();

                if (plugin.isSupported(storageSetting)) {
                    pluginIdentifier = plugin.getClassIdentifier();
                    plugin.writeHtml(page, storageSetting);
                    break;
                }
            }
        }

        page.writeStart("div", "class", "fileSelector");

            page.writeStart("select",
                    "id", page.getId(),
                    "class", "toggleable",
                    "data-root", ".inputSmall",
                    "name", actionName);

                if (fieldValue != null) {
                    page.writeStart("option",
                            "data-hide", "." + FILE_SELECTOR_ITEM_CLASS,
                            "data-show", "." + FILE_SELECTOR_EXISTING_CLASS,
                            "value", "keep");
                        page.write("Keep Existing");
                    page.writeEnd();
                }

                page.writeStart("option",
                        "data-hide", "." + FILE_SELECTOR_ITEM_CLASS,
                        "value", "none");
                    page.write("None");
                page.writeEnd();
                page.writeStart("option",
                        "data-hide", "." + FILE_SELECTOR_ITEM_CLASS,
                        "data-show", "." + FILE_SELECTOR_NEW_UPLOAD_CLASS,
                        "value", "newUpload",
                        fieldValue == null && field.isRequired() ? "selected" : "", "");
                    page.write("New Upload");
                page.writeEnd();
                page.writeStart("option",
                        "data-hide", "." + FILE_SELECTOR_ITEM_CLASS,
                        "data-show", "." + FILE_SELECTOR_NEW_URL_CLASS,
                        "value", "newUrl");
                    page.write("New URL");
                page.writeEnd();

                if (!ObjectUtils.isBlank(page.getCmsTool().getDropboxApplicationKey())) {
                    page.writeStart("option",
                            "data-hide", "." + FILE_SELECTOR_ITEM_CLASS,
                            "data-show", "." + FILE_SELECTOR_DROPBOX_CLASS,
                            "value", "dropbox");
                    page.write("Dropbox");
                    page.writeEnd();
                }

            page.writeEnd();

            page.writeTag("input",
                    "class", FILE_SELECTOR_ITEM_CLASS + " " + FILE_SELECTOR_NEW_UPLOAD_CLASS + " " + pluginIdentifier,
                    "type", "file",
                    "name", page.h(fileName),
                    "data-field-name", fieldName,
                    "data-type-id", state.getTypeId(),
                    "data-input-name", inputName,
                    "data-storage", storageSetting,
                    "data-path-start", StorageItemField.createStorageItemPath(null, null));
            page.writeTag("input",
                    "class", FILE_SELECTOR_ITEM_CLASS + " " + FILE_SELECTOR_NEW_URL_CLASS,
                    "type", "text",
                    "name", page.h(urlName));

            if (!ObjectUtils.isBlank(page.getCmsTool().getDropboxApplicationKey())) {
                page.writeStart("span",
                        "class", FILE_SELECTOR_ITEM_CLASS + " " + FILE_SELECTOR_DROPBOX_CLASS,
                        "style", "display: inline-block; vertical-align: bottom");
                    page.writeTag("input",
                            "type", "dropbox-chooser",
                            "name", page.h(dropboxName),
                            "data-link-type", "direct",
                            "style", "visibility:hidden");
                page.writeEnd();
                page.writeStart("script", "type", "text/javascript");
                    page.writeRaw(
                            "$('.fileSelectorDropbox input').on('DbxChooserSuccess', function(event) {\n" +
                                    "   $(this).val(JSON.stringify(event.originalEvent.files[0]));\n" +
                                    "});");
                page.writeEnd();
            }

        page.writeEnd();
    }

    public static StorageItem createStorageItemFromPart(ToolPageContext page, Part filePart, ObjectField field, State state)throws ServletException, IOException {

        if (!StorageItemField.checkFileContent(filePart, page, state, field)) {
            return null;
        }

        String fileName = filePart.getName();
        String contentType = filePart.getContentType();
        Map<String, List<String>> httpHeaders = new LinkedHashMap<String, List<String>>();

        httpHeaders.put("Cache-Control", Collections.singletonList("public, max-age=31536000"));
        httpHeaders.put("Content-Length", Collections.singletonList(String.valueOf(filePart.getSize())));
        httpHeaders.put("Content-Type", Collections.singletonList(contentType));

        String storageSetting = field.as(ToolUi.class).getStorageSetting();
        StorageItem item = StorageItem.Static.createIn(storageSetting != null ? Settings.getOrDefault(String.class, storageSetting, null) : null);

        item.setPath(createStorageItemPath(filePart.getName(), null));
        item.setContentType(contentType);
        item.getMetadata().put("http.headers", httpHeaders);
        item.getMetadata().put("originalFilename", fileName);
        item.setData(filePart.getInputStream());

        if (contentType != null && contentType.startsWith("image/")) {
            InputStream fileInput = filePart.getInputStream();

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

        item.save();

        return item;
    }

    public static StorageItem createStorageItemFromPath(String path, String storageSetting) {

        StorageItem storageItem = StorageItem.Static.createIn(storageSetting != null ? Settings.getOrDefault(String.class, storageSetting, null) : null);
        storageItem.setPath(path);
        storageItem.setContentType(storageItem.getContentType());

        //TODO: validate file content here?

        return storageItem;
    }

    public static boolean checkFileContent(Part filePart, ToolPageContext page, State state, ObjectField field) throws IOException {

        String errorMessage = null;

        if (!isAcceptedContentType(filePart)) {
            errorMessage = String.format(
                    "Invalid content type [%s]. Must match the pattern [%s].",
                    filePart.getContentType(), getContentTypeGroups());

        } else if (isDisguisedHtml(filePart)) {
            errorMessage = String.format(
                    "Can't upload [%s] file disguising as HTML!",
                    filePart.getContentType());
        }

        if (state != null && field != null) {
            state.addError(field, errorMessage);
        } else {
            page.getErrors().add(new IllegalArgumentException(errorMessage));
        }

        return StringUtils.isBlank(errorMessage);
    }

    private static boolean isAcceptedContentType(Part filePart) throws IOException {
        String contentType = filePart.getContentType();
        return getContentTypeGroups().contains(contentType);
    }

    private static boolean isDisguisedHtml(Part filePart) throws IOException {
        Set<String> contentTypeGroups = getContentTypeGroups();

        // Disallow HTML disguising as other content types per:
        // http://www.adambarth.com/papers/2009/barth-caballero-song.pdf
        if (!contentTypeGroups.contains("text/html")) {
            InputStream input = filePart.getInputStream();

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
                    return true;
                }

            } finally {
                input.close();
            }
        }
        return false;
    }

    private static Set<String> getContentTypeGroups() {
        String groupsPattern = Settings.get(String.class, "cms/tool/fileContentTypeGroups");
        return new SparseSet(ObjectUtils.isBlank(groupsPattern) ? "+/" : groupsPattern);
    }

    public static String createStorageItemPath(String fileName, String label) {
        String idString = UUID.randomUUID().toString().replace("-", "");
        StringBuilder pathBuilder = new StringBuilder();
        String extension = "";

        if (!ObjectUtils.isBlank(fileName)) {
            int lastDotAt = fileName.indexOf('.');

            if (lastDotAt > -1) {
                extension = fileName.substring(lastDotAt);
                fileName = fileName.substring(0, lastDotAt);

            }
        }

        if (ObjectUtils.isBlank(label) ||
                ObjectUtils.to(UUID.class, label) != null) {
            label = fileName;
        }

        if (ObjectUtils.isBlank(label)) {
            label = UUID.randomUUID().toString().replace("-", "");
        }

        pathBuilder.append(idString.substring(0, 2));
        pathBuilder.append('/');
        pathBuilder.append(idString.substring(2, 4));
        pathBuilder.append('/');
        pathBuilder.append(idString.substring(4));
        pathBuilder.append('/');
        pathBuilder.append(StringUtils.toNormalized(label));
        pathBuilder.append(extension);

        return pathBuilder.toString();
    }

    public static String getStorageSetting(ObjectField field) {
        String storageName = Settings.get(String.class, StorageItem.DEFAULT_STORAGE_SETTING);
        String fieldStorageSetting = field.as(ToolUi.class).getStorageSetting();

        if (!StringUtils.isBlank(fieldStorageSetting)) {
            storageName = fieldStorageSetting;
        }

        return storageName;
    }

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        reallyDoService(page);
    }
}
