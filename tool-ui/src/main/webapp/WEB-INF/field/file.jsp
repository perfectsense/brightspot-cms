<%@ page session="false" import="

com.psddev.cms.db.ImageCrop,
com.psddev.cms.db.ImageTag,
com.psddev.cms.db.ImageTextOverlay,
com.psddev.cms.db.ResizeOption,
com.psddev.cms.db.StandardImageSize,
com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ColorDistribution,
com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ReferentialText,
com.psddev.dari.db.State,
com.psddev.dari.util.AggregateException,
com.psddev.dari.util.BrightcoveStorageItem,
com.psddev.dari.util.ClassFinder,
com.psddev.dari.util.CollectionUtils,
com.psddev.dari.util.MultipartRequest,
com.psddev.dari.util.ImageEditor,
com.psddev.dari.util.ImageMetadataMap,
com.psddev.dari.util.IoUtils,
com.psddev.dari.util.JavaImageEditor,
com.psddev.dari.util.JspUtils,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.RoutingFilter,
com.psddev.dari.util.Settings,
com.psddev.dari.util.SparseSet,
com.psddev.dari.util.StorageItem,
com.psddev.dari.util.StorageItem.Static,
com.psddev.dari.util.StringUtils,
com.psddev.dari.util.TypeReference,

java.io.File,
java.io.FileInputStream,
java.io.FileOutputStream,
java.io.InputStream,
java.io.IOException,
java.net.URL,
java.util.AbstractMap,
java.util.ArrayList,
java.util.Collections,
java.util.HashMap,
java.util.Iterator,
java.util.LinkedHashMap,
java.util.List,
java.util.Locale,
java.util.Map,
java.util.Set,
java.util.TreeMap,
java.util.UUID,

org.apache.commons.fileupload.FileItem,

org.slf4j.Logger,
org.slf4j.LoggerFactory
" %><%!

private static final Logger LOGGER = LoggerFactory.getLogger(ToolPageContext.class);
%><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));
UUID id = state.getId();

ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();
StorageItem fieldValue = (StorageItem) state.getValue(fieldName);

String inputName = (String) request.getAttribute("inputName");
String actionName = inputName + ".action";
String storageName = inputName + ".storage";
String pathName = inputName + ".path";
String contentTypeName = inputName + ".contentType";
String fileName = inputName + ".file";
String urlName = inputName + ".url";
String dropboxName = inputName + ".dropbox";
String cropsName = inputName + ".crops.";

String brightnessName = inputName + ".brightness";
String contrastName = inputName + ".contrast";
String flipHName = inputName + ".flipH";
String flipVName = inputName + ".flipV";
String grayscaleName = inputName + ".grayscale";
String invertName = inputName + ".invert";
String rotateName = inputName + ".rotate";
String sepiaName = inputName + ".sepia";
String sharpenName = inputName + ".sharpen";
String blurName = inputName + ".blur";

String metadataFieldName = fieldName + ".metadata";
String widthFieldName = fieldName + ".width";
String heightFieldName = fieldName + ".height";
String cropsFieldName = fieldName + ".crops";

Map<String, Object> fieldValueMetadata = null;
if (fieldValue != null) {
    fieldValueMetadata = fieldValue.getMetadata();
}

if (fieldValueMetadata == null) {
    fieldValueMetadata = new LinkedHashMap<String, Object>();
}

Map<String, Object> edits = (Map<String, Object>) fieldValueMetadata.get("cms.edits");

if (edits == null) {
    edits = new HashMap<String, Object>();
    fieldValueMetadata.put("cms.edits", edits);
}

double brightness = ObjectUtils.to(double.class, edits.get("brightness"));
double contrast = ObjectUtils.to(double.class, edits.get("contrast"));
boolean flipH = ObjectUtils.to(boolean.class, edits.get("flipH"));
boolean flipV = ObjectUtils.to(boolean.class, edits.get("flipV"));
boolean grayscale = ObjectUtils.to(boolean.class, edits.get("grayscale"));
boolean invert = ObjectUtils.to(boolean.class, edits.get("invert"));
int rotate = ObjectUtils.to(int.class, edits.get("rotate"));
boolean sepia = ObjectUtils.to(boolean.class, edits.get("sepia"));
int sharpen = ObjectUtils.to(int.class, edits.get("sharpen"));

List<String> blurs = new ArrayList<String>();
if (!ObjectUtils.isBlank(edits.get("blur"))) {
    Object blur = edits.get("blur");
    if (blur instanceof String && ObjectUtils.to(String.class, blur).matches("(\\d+x){3}\\d+")) {
        blurs.add(ObjectUtils.to(String.class, blur));
    } else if (blur instanceof List) {
        for (Object blurItem : (List) blur) {
            String blurValue = ObjectUtils.to(String.class, blurItem);
            if (blurValue.matches("(\\d+x){3}\\d+")) {
                 blurs.add(blurValue);
            }
        }
    }
}

Map<String, ImageCrop> crops = ObjectUtils.to(new TypeReference<Map<String, ImageCrop>>() { }, fieldValueMetadata.get("cms.crops"));
if (crops == null) {
    // for backward compatibility
    crops = ObjectUtils.to(new TypeReference<Map<String, ImageCrop>>() { }, state.getValue(cropsFieldName));
}
if (crops == null) {
    crops = new HashMap<String, ImageCrop>();
}

crops = new TreeMap<String, ImageCrop>(crops);

Map<String, StandardImageSize> sizes = new HashMap<String, StandardImageSize>();
for (StandardImageSize size : StandardImageSize.findAll()) {
    String sizeId = size.getId().toString();
    sizes.put(sizeId, size);
    if (crops.get(sizeId) == null) {
        crops.put(sizeId, new ImageCrop());
    }
}

Class HotSpotClass = ObjectUtils.getClassByName(ImageTag.HOTSPOT_CLASS);
boolean projectUsingBrightSpotImage = HotSpotClass != null && !ObjectUtils.isBlank(ClassFinder.Static.findClasses(HotSpotClass));

if ((Boolean) request.getAttribute("isFormPost")) {
    File file = null;

    try {
        String action = wp.param(actionName);
        StorageItem newItem = null;

        brightness = wp.param(double.class, brightnessName);
        contrast = wp.param(double.class, contrastName);
        flipH = wp.param(boolean.class, flipHName);
        flipV = wp.param(boolean.class, flipVName);
        grayscale = wp.param(boolean.class, grayscaleName);
        invert = wp.param(boolean.class, invertName);
        rotate = wp.param(int.class, rotateName);
        sepia = wp.param(boolean.class, sepiaName);
        sharpen = wp.param(int.class, sharpenName);

        edits = new HashMap<String, Object>();

        if (brightness != 0.0) {
            edits.put("brightness", brightness);
        }
        if (contrast != 0.0) {
            edits.put("contrast", contrast);
        }
        if (flipH) {
            edits.put("flipH", flipH);
        }
        if (flipV) {
            edits.put("flipV", flipV);
        }
        if (invert) {
            edits.put("invert", invert);
        }
        if (rotate != 0) {
            edits.put("rotate", rotate);
        }
        if (grayscale) {
            edits.put("grayscale", grayscale);
        }
        if (sepia) {
            edits.put("sepia", sepia);
        }
        if (sharpen != 0) {
            edits.put("sharpen", sharpen);
        }

        if (!ObjectUtils.isBlank(wp.params(String.class, blurName))) {
            blurs = new ArrayList<String>();
            for (String blur : wp.params(String.class, blurName)) {
                if (!blurs.contains(blur)) {
                    blurs.add(blur);
                }
            }

            if (blurs.size() == 1) {
                edits.put("blur", blurs.get(0));
            } else {
                edits.put("blur", blurs);
            }
        }

        fieldValueMetadata.put("cms.edits", edits);

        InputStream newItemData = null;

        if ("keep".equals(action)) {
            if (fieldValue != null) {
                newItem = fieldValue;
            } else {
                newItem = StorageItem.Static.createIn(wp.param(storageName));
                newItem.setPath(wp.param(pathName));
                newItem.setContentType(wp.param(contentTypeName));
            }

        } else if ("newUpload".equals(action) ||
                "dropbox".equals(action)) {
            String name = null;
            String fileContentType = null;
            long fileSize = 0;
            file = File.createTempFile("cms.", ".tmp");

            if ("dropbox".equals(action)) {
                Map<String, Object> fileData = (Map<String, Object>) ObjectUtils.fromJson(wp.param(String.class, dropboxName));

                if (fileData != null) {
                    name = ObjectUtils.to(String.class, fileData.get("name"));
                    fileContentType = ObjectUtils.getContentType(name);
                    fileSize = ObjectUtils.to(long.class, fileData.get("bytes"));
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
                }

            } else if (request instanceof MultipartRequest) {
                MultipartRequest mpRequest = (MultipartRequest) request;
                FileItem fileItem = mpRequest.getFileItem(fileName);

                if (fileItem != null) {
                    name = fileItem.getName();
                    fileContentType = fileItem.getContentType();
                    fileSize = fileItem.getSize();

                    fileItem.write(file);
                }
            }

            if (name != null &&
                    fileContentType != null) {

                // Checks to make sure the file's content type is valid
                String groupsPattern = Settings.get(String.class, "cms/tool/fileContentTypeGroups");
                Set<String> contentTypeGroups = new SparseSet(ObjectUtils.isBlank(groupsPattern) ? "+/" : groupsPattern);
                if (!contentTypeGroups.contains(fileContentType)) {
                    state.addError(field, String.format(
                            "Invalid content type [%s]. Must match the pattern [%s].",
                            fileContentType, contentTypeGroups));
                    return;
                }

                // Disallow HTML disguising as other content types per:
                // http://www.adambarth.com/papers/2009/barth-caballero-song.pdf
                if (!contentTypeGroups.contains("text/html")) {
                    InputStream input = new FileInputStream(file);

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
                            state.addError(field, String.format(
                                    "Can't upload [%s] file disguising as HTML!",
                                    fileContentType));
                            return;
                        }

                    } finally {
                        input.close();
                    }
                }

                if (fileSize > 0) {
                    String idString = UUID.randomUUID().toString().replace("-", "");
                    StringBuilder pathBuilder = new StringBuilder();
                    String label = state.getLabel();

                    fieldValueMetadata.put("originalFilename", name);

                    int lastDotAt = name.indexOf('.');
                    String extension;

                    if (lastDotAt > -1) {
                        extension = name.substring(lastDotAt);
                        name = name.substring(0, lastDotAt);

                    } else {
                        extension = "";
                    }

                    if (ObjectUtils.isBlank(label) ||
                            ObjectUtils.to(UUID.class, label) != null) {
                        label = name;
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

                    String storageSetting = field.as(ToolUi.class).getStorageSetting();

                    newItem = StorageItem.Static.createIn(storageSetting != null ? Settings.getOrDefault(String.class, storageSetting, null) : null);
                    newItem.setPath(pathBuilder.toString());
                    newItem.setContentType(fileContentType);

                    Map<String, List<String>> httpHeaders = new LinkedHashMap<String, List<String>>();
                    httpHeaders.put("Cache-Control", Collections.singletonList("public, max-age=31536000"));
                    httpHeaders.put("Content-Length", Collections.singletonList(String.valueOf(fileSize)));
                    httpHeaders.put("Content-Type", Collections.singletonList(fileContentType));
                    fieldValueMetadata.put("http.headers", httpHeaders);

                    newItem.setData(new FileInputStream(file));

                    newItemData = new FileInputStream(file);
                }
            }

        } else if ("newUrl".equals(action)) {
            newItem = StorageItem.Static.createUrl(wp.param(urlName));

            newItemData = newItem.getData();
        }

        // Automatic image metadata extraction.
        if (newItem != null && !"keep".equals(action)) {
            if (newItemData == null) {
                newItemData = newItem.getData();
            }

            String contentType = newItem.getContentType();

            if (contentType != null && contentType.startsWith("image/")) {
                try {
                    ImageMetadataMap metadata = new ImageMetadataMap(newItemData);
                    fieldValueMetadata.putAll(metadata);

                    List<Throwable> errors = metadata.getErrors();
                    if (!errors.isEmpty()) {
                        LOGGER.debug("Can't read image metadata!", new AggregateException(errors));
                    }

                } finally {
                    IoUtils.closeQuietly(newItemData);
                }
            }
        }

        // Standard sizes.
        for (Iterator<Map.Entry<String, ImageCrop>> i = crops.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<String, ImageCrop> e = i.next();
            String cropId = e.getKey();
            double x = wp.doubleParam(cropsName + cropId + ".x");
            double y = wp.doubleParam(cropsName + cropId + ".y");
            double width = wp.doubleParam(cropsName + cropId + ".width");
            double height = wp.doubleParam(cropsName + cropId + ".height");
            String texts = wp.param(cropsName + cropId + ".texts");
            String textSizes = wp.param(cropsName + cropId + ".textSizes");
            String textXs = wp.param(cropsName + cropId + ".textXs");
            String textYs = wp.param(cropsName + cropId + ".textYs");
            String textWidths = wp.param(cropsName + cropId + ".textWidths");
            if (x != 0.0 || y != 0.0 || width != 0.0 || height != 0.0 || !ObjectUtils.isBlank(texts)) {
                ImageCrop crop = e.getValue();
                crop.setX(x);
                crop.setY(y);
                crop.setWidth(width);
                crop.setHeight(height);
                crop.setTexts(texts);
                crop.setTextSizes(textSizes);
                crop.setTextXs(textXs);
                crop.setTextYs(textYs);
                crop.setTextWidths(textWidths);

                for (Iterator<ImageTextOverlay> j = crop.getTextOverlays().iterator(); j.hasNext(); ) {
                    ImageTextOverlay textOverlay = j.next();
                    String text = textOverlay.getText();

                    if (text != null) {
                        StringBuilder cleaned = new StringBuilder();

                        for (Object item : new ReferentialText(text, true)) {
                            if (item instanceof String) {
                                cleaned.append((String) item);
                            }
                        }

                        text = cleaned.toString();

                        if (ObjectUtils.isBlank(text.replaceAll("<[^>]*>", ""))) {
                            j.remove();

                        } else {
                            textOverlay.setText(text);
                        }
                    }
                }

            } else {
                i.remove();
            }
        }
        fieldValueMetadata.put("cms.crops", crops);
        // Removes legacy cropping information
        if (state.getValue(cropsFieldName) != null) {
            state.remove(cropsFieldName);
        }

        // Transfers legacy metadata over to it's new location within the StorageItem object
        Map<String, Object> legacyMetadata = ObjectUtils.to(new TypeReference<Map<String, Object>>() { }, state.getValue(metadataFieldName));
        if (legacyMetadata != null && !legacyMetadata.isEmpty()) {
            for (Map.Entry<String, Object> entry : legacyMetadata.entrySet()) {
                if (!fieldValueMetadata.containsKey(entry.getKey())) {
                    fieldValueMetadata.put(entry.getKey(), entry.getValue());
                }
            }
            state.remove(metadataFieldName);
        }

        if (newItem != null) {
            newItem.setMetadata(fieldValueMetadata);
        }

        if (newItem != null &&
                ("newUpload".equals(action) ||
                "dropbox".equals(action))) {
            newItem.save();
        }

        state.putValue(fieldName, newItem);

        if (projectUsingBrightSpotImage) {
            JspUtils.include(request, response, out, "set/hotSpot.jsp");
        }
        return;

    } finally {
        if (file != null && file.exists()) {
            file.delete();
        }
    }
}

// --- Presentation ---

%><div class="inputSmall">
    <div class="fileSelector">
        <select class="toggleable" data-root=".inputSmall" id="<%= wp.getId() %>" name="<%= wp.h(actionName) %>">
            <% if (fieldValue != null) { %>
                <option data-hide=".fileSelectorItem" data-show=".fileSelectorExisting" value="keep">Keep Existing</option>
            <% } %>
            <option data-hide=".fileSelectorItem" value="none">None</option>
            <option data-hide=".fileSelectorItem" data-show=".fileSelectorNewUpload" value="newUpload"<%= fieldValue == null && field.isRequired() ? " selected" : "" %>>New Upload</option>
            <option data-hide=".fileSelectorItem" data-show=".fileSelectorNewUrl" value="newUrl">New URL</option>
            <% if (!ObjectUtils.isBlank(wp.getCmsTool().getDropboxApplicationKey())) { %>
                <option data-hide=".fileSelectorItem" data-show=".fileSelectorDropbox" value="dropbox">Dropbox</option>
            <% } %>
        </select>

        <input class="fileSelectorItem fileSelectorNewUpload" type="file" name="<%= wp.h(fileName) %>">
        <input class="fileSelectorItem fileSelectorNewUrl" type="text" name="<%= wp.h(urlName) %>">
        <% if (!ObjectUtils.isBlank(wp.getCmsTool().getDropboxApplicationKey())) { %>
            <span class="fileSelectorItem fileSelectorDropbox" style="display: inline-block; vertical-align: bottom;">
                <input type="dropbox-chooser" name="<%= wp.h(dropboxName) %>" data-link-type="direct" style="visibility: hidden;">
            </span>
            <script type="text/javascript">
                $('.fileSelectorDropbox input').on('DbxChooserSuccess', function(event) {
                    $(this).val(JSON.stringify(event.originalEvent.files[0]));
                });
            </script>
        <% } %>
    </div>

    <%
    if (fieldValue != null) {
        String contentType = fieldValue.getContentType();
        %>
        <div class="fileSelectorItem fileSelectorExisting filePreview">
            <input name="<%= wp.h(storageName) %>" type="hidden" value="<%= wp.h(fieldValue.getStorage()) %>">
            <input name="<%= wp.h(pathName) %>" type="hidden" value="<%= wp.h(fieldValue.getPath()) %>">
            <input name="<%= wp.h(contentTypeName) %>" type="hidden" value="<%= wp.h(contentType) %>">

            <% if (field.as(ToolUi.class).getStoragePreviewProcessorPath() != null) {

                ToolUi ui = field.as(ToolUi.class);
                String processorPath = ui.getStoragePreviewProcessorPath();
                if (processorPath != null) {
                    JspUtils.include(request, response, out,
                            RoutingFilter.Static.getApplicationPath(ui.getStoragePreviewProcessorApplication()) +
                            StringUtils.ensureStart(processorPath, "/"));
                }

               } else if (contentType != null && contentType.startsWith("image/")) { %>
                <div class="imageEditor">

                    <div class="imageEditor-aside">
                        <div class="imageEditor-tools">
                            <h2>Tools</h2>
                            <ul>
                                <% if (state.as(ColorDistribution.Data.class).getDistribution() != null) { %>
                                    <li><a class="icon icon-tint" href="<%= wp.h(wp.cmsUrl("/contentColors", "id", state.getId())) %>" target="contentColors">Colors</a></li>
                                <% } %>
                                <li><a class="action-preview" href="<%= wp.h(fieldValue.getPublicUrl()) %>" target="_blank">View Original</a></li>
                                <li><a class="icon icon-crop" href="<%= wp.h(wp.url("/contentImages", "id", id, "field", fieldName)) %>" target="contentImages">View Resized</a></li>
                            </ul>
                        </div>

                        <div class="imageEditor-edit">
                            <h2>Adjustments</h2>
                            <%
                                boolean usingJavaImageEditor = ImageEditor.Static.getDefault() != null && (ImageEditor.Static.getDefault() instanceof JavaImageEditor);
                            %>
                            <table><tbody>
                                <% if (usingJavaImageEditor) { %>
                                    <tr>
                                        <th>Blur</th>
                                        <td>
                                            <a class="imageEditor-addBlurOverlay">Add Blur</a><br/>
                                            <%
                                                if (!ObjectUtils.isBlank(blurs)) {
                                                    for (String blur : blurs) {
                                                        %><input type="hidden" name="<%=blurName%>" value="<%=blur%>"><%
                                                    }
                                                }
                                            %>
                                        </td>
                                    </tr>
                                <% } %>
                                <tr>
                                    <th>Brightness</th>
                                    <td><input type="range" name="<%= brightnessName %>" value="<%= brightness %>" min="-1.0" max="1.0" step="0.01"></td>
                                </tr>
                                <tr>
                                    <th>Contrast</th>
                                    <td><input type="range" name="<%= contrastName %>" value="<%= contrast %>" min="-1.0" max="1.0" step="0.01"></td>
                                </tr>
                                <tr>
                                    <th>Flip H</th>
                                    <td><input type="checkbox" name="<%= flipHName %>" value="true"<%= flipH ? " checked" : "" %>></td>
                                </tr>
                                <tr>
                                    <th>Flip V</th>
                                    <td><input type="checkbox" name="<%= flipVName %>" value="true"<%= flipV ? " checked" : "" %>></td>
                                </tr>
                                <tr>
                                    <th>Invert</th>
                                    <td><input type="checkbox" name="<%= invertName %>" value="true"<%= invert ? " checked" : "" %>></td>
                                </tr>
                                <tr>
                                    <th>Grayscale</th>
                                    <td><input type="checkbox" name="<%= grayscaleName %>" value="true"<%= grayscale ? " checked" : "" %>></td>
                                </tr>
                                <tr>
                                    <th>Rotate</th>
                                    <td><input type="range" name="<%= rotateName %>" value="<%= rotate %>" min="-90" max="90" step="90"></td>
                                </tr>
                                <tr>
                                    <th>Sepia</th>
                                    <td><input type="checkbox" name="<%= sepiaName %>" value="true"<%= sepia ? " checked" : "" %>></td>
                                </tr>
                                <% if (usingJavaImageEditor) { %>
                                    <tr>
                                        <th>Sharpen</th>
                                        <td><input type="range" name="<%= sharpenName %>" value="<%= sharpen %>" min="0" max="10" step="1"></td>
                                    </tr>
                                <% } %>
                            </tbody></table>
                        </div>

                        <% if (!crops.isEmpty()) { %>
                            <div class="imageEditor-sizes">
                                <h2>Standard Sizes</h2>
                                <table><tbody>
                                    <%
                                    for (Map.Entry<String, ImageCrop> e : crops.entrySet()) {
                                        String cropId = e.getKey();
                                        ImageCrop crop = e.getValue();
                                        StandardImageSize size = sizes.get(cropId);
                                        if (size == null && ObjectUtils.to(UUID.class, cropId) != null) {
                                            continue;
                                        }
                                        %>
                                        <% if (size != null) { %>
                                            <tr data-size-name="<%= size.getInternalName() %>" data-size-independent="<%= size.isIndependent() %>" data-size-width="<%= size.getWidth() %>" data-size-height="<%= size.getHeight() %>">
                                                <th><%= wp.h(size.getDisplayName()) %></th>
                                        <% } else { %>
                                            <tr>
                                                <th><%= wp.h(cropId) %></th>
                                        <% } %>
                                            <td><input name="<%= wp.h(cropsName + cropId + ".x") %>" type="text" value="<%= crop.getX() %>"></td>
                                            <td><input name="<%= wp.h(cropsName + cropId + ".y") %>" type="text" value="<%= crop.getY() %>"></td>
                                            <td><input name="<%= wp.h(cropsName + cropId + ".width") %>" type="text" value="<%= crop.getWidth() %>"></td>
                                            <td><input name="<%= wp.h(cropsName + cropId + ".height") %>" type="text" value="<%= crop.getHeight() %>"></td>
                                            <td><input name="<%= wp.h(cropsName + cropId + ".texts") %>" type="text" value="<%= wp.h(crop.getTexts()) %>"></td>
                                            <td><input name="<%= wp.h(cropsName + cropId + ".textSizes") %>" type="text" value="<%= wp.h(crop.getTextSizes()) %>"></td>
                                            <td><input name="<%= wp.h(cropsName + cropId + ".textXs") %>" type="text" value="<%= crop.getTextXs() %>"></td>
                                            <td><input name="<%= wp.h(cropsName + cropId + ".textYs") %>" type="text" value="<%= crop.getTextYs() %>"></td>
                                            <td><input name="<%= wp.h(cropsName + cropId + ".textWidths") %>" type="text" value="<%= crop.getTextWidths() %>"></td>
                                        </tr>
                                    <% } %>
                                </tbody></table>
                            </div>
                        <% } %>
                    </div>

                    <div class="imageEditor-image">
                        <%
                        String fieldValueUrl;
                        String resizeScale = "";
                        if (ImageEditor.Static.getDefault() != null) {
                            ImageTag.Builder imageTagBuilder = new ImageTag.Builder(fieldValue).
                                    setWidth(1000).
                                    setResizeOption(ResizeOption.ONLY_SHRINK_LARGER).
                                    setEdits(false);
                            Number originalWidth = null;
                            if (!ObjectUtils.isBlank(CollectionUtils.getByPath(imageTagBuilder.getItem().getMetadata(), "image/originalWidth"))) {
                                originalWidth = (Number) CollectionUtils.getByPath(imageTagBuilder.getItem().getMetadata(), "image/originalWidth");
                            } else if (!ObjectUtils.isBlank(CollectionUtils.getByPath(imageTagBuilder.getItem().getMetadata(), "dims/originalWidth"))) {
                                originalWidth = (Number) CollectionUtils.getByPath(imageTagBuilder.getItem().getMetadata(), "dims/originalWidth");
                            } else if (!ObjectUtils.isBlank(CollectionUtils.getByPath(imageTagBuilder.getItem().getMetadata(), "width"))) {
                                originalWidth = (Number) CollectionUtils.getByPath(imageTagBuilder.getItem().getMetadata(), "width");
                            }
                            if (originalWidth != null) {
                                if (originalWidth.intValue() > 1000) {
                                    resizeScale = String.format("%.2f", (double) 1000 /originalWidth.intValue());
                                }
                            }
                            fieldValueUrl = imageTagBuilder.toUrl();
                        } else {
                            fieldValueUrl = fieldValue.getPublicUrl();
                        }
                        %>
                        <img alt="" data-scale="<%=resizeScale%>" src="<%= wp.url("/misc/proxy.jsp",
                                "url", fieldValueUrl,
                                "hash", StringUtils.hex(StringUtils.hmacSha1(Settings.getSecret(), fieldValueUrl))) %>">
                    </div>

                </div>

            <% } else if(fieldValue instanceof BrightcoveStorageItem) { %>

                <% String playerKey = ((BrightcoveStorageItem) fieldValue).getPreviewPlayerKey(); %>
                <% String playerId = ((BrightcoveStorageItem) fieldValue).getPreviewPlayerId(); %>

                <% if(!ObjectUtils.isBlank(playerKey) && !ObjectUtils.isBlank(playerId)) { %>
                    <!-- Start of Brightcove Player -->

                    <!--
                    By use of this code snippet, I agree to the Brightcove Publisher T and C
                    found at https://accounts.brightcove.com/en/terms-and-conditions/.
                    -->
                    <script language="JavaScript" type="text/javascript" src="http://admin.brightcove.com/js/BrightcoveExperiences.js"></script>

                    <script type="text/javascript">


                        // Store reference to the player
                        var player;

                        // Store reference to the modules in the player
                        var modVP;
                        var modExp;
                        var modCon;

                        // This method is called when the player loads with the ID of the player
                        // We can use that ID to get a reference to the player, and then the modules
                        // The name of this method can vary but should match the value you specified
                        // in the player publishing code for templateLoadHandler.

                        var myTemplateLoaded = function(experienceID) {
                            // Get a reference to the player itself
                            player = brightcove.api.getExperience(experienceID);

                          // Get a reference to individual modules in the player
                          modVP = player.getModule(brightcove.api.modules.APIModules.VIDEO_PLAYER);
                          modExp = player.getModule(brightcove.api.modules.APIModules.EXPERIENCE);
                          modCon = player.getModule(brightcove.api.modules.APIModules.CONTENT);

                          if(modVP.loadVideoByID(<%=((BrightcoveStorageItem)fieldValue).getBrightcoveId()%>) === null) {
                              if(typeof(console) !== 'undefined') { console.log("Video with id=<%=((BrightcoveStorageItem)fieldValue).getBrightcoveId()%> could not be found"); }
                          }
                        };
                    </script>

                    <object id="myExperience" class="BrightcoveExperience">
                      <param name="bgcolor" value="#FFFFFF" />
                      <param name="width" value="480" />
                      <param name="height" value="270" />
                      <param name="playerID" value="<%=playerId%>" />
                      <param name="playerKey" value="<%=playerKey%>" />
                      <param name="isVid" value="true" />
                      <param name="isUI" value="true" />
                      <param name="dynamicStreaming" value="true" />
                      <param name="includeAPI" value="true" />
                      <param name="templateLoadHandler" value="myTemplateLoaded" />
                    </object>

                    <!--
                    This script tag will cause the Brightcove Players defined above it to be created as soon
                    as the line is read by the browser. If you wish to have the player instantiated only after
                    the rest of the HTML is processed and the page load is complete, remove the line.
                    -->
                    <script type="text/javascript">brightcove.createExperiences();</script>

                    <!-- End of Brightcove Player -->
                <% } else { %>
                    <p>No Brightcove player is configured for previewing videos.</p>
                <% } %>

            <%
            } else if (contentType != null && contentType.startsWith("video/")) {
                wp.writeStart("div", "style", wp.cssString("margin-bottom", "5px"));
                    wp.writeStart("a",
                            "class", "icon icon-action-preview",
                            "href", fieldValue.getPublicUrl(),
                            "target", "_blank");
                        wp.writeHtml("View Original");
                    wp.writeEnd();
                wp.writeEnd();

                wp.writeStart("video",
                        "controls", "controls",
                        "preload", "auto");
                    wp.writeElement("source",
                            "type", contentType,
                            "src", fieldValue.getPublicUrl());
                wp.writeEnd();
            %>

            <% } else { %>
                <a href="<%= wp.h(fieldValue.getPublicUrl()) %>" target="_blank"><%= wp.h(contentType) %>: <%= wp.h(fieldValue.getPath()) %></a>
            <% } %>
        </div>
    <% } %>
</div>
<%
    if (projectUsingBrightSpotImage) {
        JspUtils.include(request, response, out, "set/hotSpot.jsp");
    }
%>