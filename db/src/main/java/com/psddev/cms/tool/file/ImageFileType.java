package com.psddev.cms.tool.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.psddev.cms.db.ImageCrop;
import com.psddev.cms.db.ImageTag;
import com.psddev.cms.db.ImageTextOverlay;
import com.psddev.cms.db.ResizeOption;
import com.psddev.cms.db.StandardImageSize;
import com.psddev.cms.tool.FileContentType;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ColorDistribution;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ReferentialText;
import com.psddev.dari.db.State;
import com.psddev.dari.util.AggregateException;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.CollectionUtils;
import com.psddev.dari.util.DimsImageEditor;
import com.psddev.dari.util.ImageEditor;
import com.psddev.dari.util.ImageMetadataMap;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.JavaImageEditor;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageFileType implements FileContentType {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageFileType.class);

    @Override
    public boolean isSupported(StorageItem storageItem) {
        String contentType = storageItem.getContentType();
        return !StringUtils.isBlank(contentType) && contentType.startsWith("image/");
    }

    @Override
    public boolean isPreferred(StorageItem storageItem) {
        return false;
    }

    @Override
    public void writePreview(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException {
        writeImageEditor(page, state, fieldValue);
    }

    /**
     * Metadata for images includes:
     * {@link ImageCrop}, {@link ImageAdjustment}, and
     * automatically extracted Exif data via {@link #setExtractedMetaData(File, StorageItem, String, Map)}
     * @param page
     * @param state
     * @param fieldValue
     * @param file
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void setMetadata(ToolPageContext page, State state, StorageItem fieldValue, File file) throws IOException, ServletException {
        if (fieldValue == null) {
            return;
        }

        HttpServletRequest request = page.getRequest();

        ObjectField field = (ObjectField) request.getAttribute("field");
        String fieldName = field.getInternalName();

        String inputName = (String) request.getAttribute("inputName");
        String actionName = inputName + ".action";

        String metadataFieldName = fieldName + ".metadata";

        String action = page.param(actionName);

        Map<String, Object> fieldValueMetadata = null;
        if ((!((Boolean) request.getAttribute("isFormPost")) || "keep".equals(action))) {
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

        //should only extract metadata on intitial save
        if (!"keep".equals(action)) {
            setExtractedMetaData(file, fieldValue, action, fieldValueMetadata);
        }

        //only sets crops/edits if image has already been uploaded
        if ("keep".equals(action)) {
            setEdits(page, fieldValueMetadata);
            setCrops(page, fieldValueMetadata, state);
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

        fieldValue.setMetadata(fieldValueMetadata);
    }

    /**
     * Automatically extracts the metadata from the provided File or StorageItem.
     * Only needs to be performed once.
     * @param file
     * @param fieldValue
     * @param action
     * @param fieldValueMetadata
     * @throws IOException
     */
    private void setExtractedMetaData(File file, StorageItem fieldValue, String action, Map<String, Object> fieldValueMetadata) throws IOException {
        InputStream itemData = null;
        if (file != null) {
            itemData = new FileInputStream(file);
        } else {
            itemData = fieldValue.getData();
        }

        String contentType = fieldValue.getContentType();

        if (contentType != null && contentType.startsWith("image/")) {
            try {
                ImageMetadataMap metadata = new ImageMetadataMap(itemData);
                fieldValueMetadata.putAll(metadata);

                List<Throwable> errors = metadata.getErrors();
                if (!errors.isEmpty()) {
                    LOGGER.debug("Can't read image metadata!", new AggregateException(errors));
                }
            } catch (Exception e) {
                LOGGER.error("Can't read image metadata!", e);
            } finally {
                IoUtils.closeQuietly(itemData);
            }
        }
    }

    /**
     * Sets the image edits from Image editor form POST.
     * Edits include: brightness, contrast, flip,
     * rotation, grayscale, invert, sepia, sharpen, and blurs
     * @param page
     * @param fieldValueMetadata
     */
    private void setEdits(ToolPageContext page, Map<String, Object> fieldValueMetadata) {

        Map<String, Object> edits = new HashMap<>();
        String inputName = (String) page.getRequest().getAttribute("inputName");

        double brightness = page.param(double.class, inputName + ".brightness");
        double contrast = page.param(double.class, inputName + ".contrast");
        boolean flipH = page.param(boolean.class, inputName + ".flipH");
        boolean flipV = page.param(boolean.class, inputName + ".flipV");
        boolean grayscale = page.param(boolean.class, inputName + ".grayscale");
        boolean invert = page.param(boolean.class, inputName + ".invert");
        int rotate = page.param(int.class, inputName + ".rotate");
        boolean sepia = page.param(boolean.class, inputName + ".sepia");
        int sharpen = page.param(int.class, inputName + ".sharpen");

        //setting image adjustments
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

        //setting blurs
        String blurName = inputName + ".blur";
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

         if (!ObjectUtils.isBlank(page.params(String.class, blurName))) {
            blurs = new ArrayList<String>();
            for (String blur : page.params(String.class, blurName)) {
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
    }

    /**
     * Sets the crops from the Image Editor form POST
     * @param page
     * @param fieldValueMetadata
     * @param state
     */
    private void setCrops(ToolPageContext page, Map<String, Object> fieldValueMetadata, State state) {
        ObjectField field = (ObjectField) page.getRequest().getAttribute("field");
        String fieldName = field.getInternalName();
        String cropsFieldName = fieldName + ".crops";

        String inputName = (String) page.getRequest().getAttribute("inputName");
        String cropsName = inputName + ".crops.";

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

        // Standard sizes.
        for (Iterator<Map.Entry<String, ImageCrop>> i = crops.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, ImageCrop> e = i.next();
            String cropId = e.getKey();
            double x = page.param(double.class, cropsName + cropId + ".x");
            double y = page.param(double.class, cropsName + cropId + ".y");
            double width = page.param(double.class, cropsName + cropId + ".width");
            double height = page.param(double.class, cropsName + cropId + ".height");
            String texts = page.param(String.class, cropsName + cropId + ".texts");
            String textSizes = page.param(String.class, cropsName + cropId + ".textSizes");
            String textXs = page.param(String.class, cropsName + cropId + ".textXs");
            String textYs = page.param(String.class, cropsName + cropId + ".textYs");
            String textWidths = page.param(String.class, cropsName + cropId + ".textWidths");
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

                for (Iterator<ImageTextOverlay> j = crop.getTextOverlays().iterator(); j.hasNext();) {
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
    }

    /**
     * Writes the image editor for display in the CMS
     * @param page
     * @throws IOException
     * @throws ServletException
     */
    public void writeImageEditor(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException {

        page.writeStart("div", "class", "imageEditor");
            writeImageEditorAside(page, fieldValue, state);
            writeImageEditorImage(page, fieldValue);
        page.writeEnd();

        Class hotspotClass = ObjectUtils.getClassByName(ImageTag.HOTSPOT_CLASS);
        boolean projectUsingBrightSpotImage = hotspotClass != null && !ObjectUtils.isBlank(ClassFinder.Static.findClasses(hotspotClass));

        if (projectUsingBrightSpotImage) {
            page.include("set/hotSpot.jsp");
        }
    }

    /**
     * Wrapper for writing image editor tools via:
     * {@link #writeImageEditorTools(ToolPageContext, StorageItem, State)}
     * {@link #writeImageEditorEdit(ToolPageContext, Map)}
     * {@link #writeImageEditorSizes(ToolPageContext, State, Map)}
     * @param page
     * @param fieldValue
     * @param state
     * @throws IOException
     */
    private void writeImageEditorAside(ToolPageContext page, StorageItem fieldValue, State state) throws IOException {

        Map<String, Object> fieldValueMetadata = null;
        if (fieldValue != null) {
            fieldValueMetadata = fieldValue.getMetadata();
        }

        if (fieldValueMetadata == null) {
            fieldValueMetadata = new LinkedHashMap<String, Object>();
        }

        page.writeStart("div", "class", "imageEditor-aside");
            writeImageEditorTools(page, fieldValue, state);
            writeImageEditorEdit(page, fieldValueMetadata);
            writeImageEditorSizes(page, state, fieldValueMetadata);
        page.writeEnd();
    }

    /**
     * Writes some tools for the Image Editor, including:
     * View Original link, View Resized Link and (Optional) {@link com.psddev.dari.db.ColorDistribution.Data}
     * @param page
     * @param fieldValue
     * @param state
     * @throws IOException
     */
    private void writeImageEditorTools(ToolPageContext page, StorageItem fieldValue, State state) throws IOException {

        page.writeStart("div", "class", "imageEditor-tools");
            page.writeStart("h2");
                page.write("Tools");
            page.writeEnd();

            page.writeStart("ul");

                if (state.as(ColorDistribution.Data.class).getDistribution() != null) {
                    page.writeStart("li");
                        page.writeStart("a",
                                "class", "icon icon-tint",
                                "href", page.h(page.cmsUrl("/contentColors", "id", state.getId())),
                                "target", "contentColors");
                            page.write("Colors");
                        page.writeEnd();
                    page.writeEnd();
                }

                page.writeStart("li");
                    page.writeStart("a",
                            "class", "action-preview",
                            "href", page.h(fieldValue.getPublicUrl()),
                            "target", "_blank");
                        page.write("View Original");
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("li");
                    page.writeStart("a",
                            "class", "icon icon-crop",
                            "href", page.h(page.url("/contentImages", "data", ObjectUtils.toJson(fieldValue))),
                            "target", "contentImages");
                        page.write("View Resized");
                    page.writeEnd();
                page.writeEnd();

            page.writeEnd();

        page.writeEnd();
    }

    /**
     * Writes the inputs for image editing:
     * blurs and all {@link com.psddev.cms.tool.file.ImageFileType.ImageAdjustment} types
     * @param page
     * @param fieldValueMetadata
     * @throws IOException
     */
    private void writeImageEditorEdit(ToolPageContext page, Map<String, Object> fieldValueMetadata) throws IOException {
        HttpServletRequest request = page.getRequest();
        String inputName = ObjectUtils.firstNonBlank(page.param(String.class, "inputName"),  (String) request.getAttribute("inputName"));
        boolean useJavaImageEditor = ImageEditor.Static.getDefault() != null && (ImageEditor.Static.getDefault() instanceof JavaImageEditor);
        String blurName = inputName + ".blur";

        Map<String, Object> edits = (Map<String, Object>) fieldValueMetadata.get("cms.edits");

        if (edits == null) {
            edits = new HashMap<String, Object>();
            fieldValueMetadata.put("cms.edits", edits);
        }

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

        page.writeStart("div", "class", "imageEditor-edit");

            page.writeStart("h2");
                page.write("Adjustments");
            page.writeEnd();

            page.writeStart("table");
                page.writeStart("tbody");

                    if (useJavaImageEditor) {
                        page.writeStart("tr");
                            page.writeStart("th");
                                page.write("Blur");
                            page.writeEnd();
                            page.writeStart("td");
                                page.writeStart("a",
                                        "class", "imageEditor-addBlurOverlay");
                                    page.write("Add Blur");
                                page.writeEnd();
                                page.writeTag("br");

                                if (!ObjectUtils.isBlank(blurs)) {
                                    for (String blur : blurs) {
                                        page.writeTag("input",
                                                "type", "hidden",
                                                "name", page.h(blurName),
                                                "value", blur);
                                    }
                                }
                            page.writeEnd();
                        page.writeEnd();
                    }

                    for (ImageAdjustment adj : ImageAdjustment.values()) {
                        page.writeStart("tr");
                            page.writeStart("th");
                                page.writeHtml(page.h(StringUtils.toPascalCase(adj.title)));
                            page.writeEnd();
                            page.writeStart("td");
                            if (!adj.javaImageEditorOnly || useJavaImageEditor) {
                                boolean isCheckbox = adj.inputType.equals("checkbox");
                                boolean isChecked = isCheckbox && ObjectUtils.equals(edits.get(adj.title), true);
                                page.writeTag("input",
                                        "type", adj.inputType,
                                        "name", inputName + "." + adj.title,
                                        adj.inputType.equals("range") ? "min" : "", adj.inputType.equals("range") ? adj.min : "",
                                        adj.inputType.equals("range") ? "max" : "", adj.inputType.equals("range") ? adj.max : "",
                                        adj.inputType.equals("range") ? "step" : "", adj.inputType.equals("range") ? adj.step : "",
                                        "value", ObjectUtils.to(adj.valueType, isCheckbox ? true : edits.get(adj.title)),
                                        isChecked ? "checked" : "",
                                        isChecked ? "true" : "");
                            }

                            page.writeEnd();
                        page.writeEnd();
                    }

                page.writeEnd();
            page.writeEnd();

        page.writeEnd();
    }

    /**
     * Writes the tools to allow editors to select from {@link StandardImageSize}s
     * and edit their crops
     * @param page
     * @param state
     * @param fieldValueMetadata
     * @throws IOException
     */
    private void writeImageEditorSizes(ToolPageContext page, State state, Map<String, Object> fieldValueMetadata) throws IOException {

        String inputName = ObjectUtils.firstNonBlank(page.param(String.class, "inputName"), (String) page.getRequest().getAttribute("inputName"));
        String cropsFieldName = inputName + ".crops";

        Map<String, ImageCrop> crops = ObjectUtils.to(new TypeReference<Map<String, ImageCrop>>() {
        }, fieldValueMetadata.get("cms.crops"));
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

        ImageEditor defaultImageEditor = ImageEditor.Static.getDefault();
        boolean centerCrop = !(defaultImageEditor instanceof DimsImageEditor) || ((DimsImageEditor) defaultImageEditor).isUseLegacyThumbnail();

        if (crops.isEmpty()) {
            return;
        }

        page.writeStart("div", "class", "imageEditor-sizes");
            page.writeStart("h2");
                page.write("Standard Sizes");
            page.writeEnd();

            page.writeStart("table", "data-crop-center", centerCrop);
                page.writeStart("tbody");
                    for (Map.Entry<String, ImageCrop> e : crops.entrySet()) {
                        String cropId = e.getKey();
                        ImageCrop crop = e.getValue();
                        StandardImageSize size = sizes.get(cropId);
                        if (size == null && ObjectUtils.to(UUID.class, cropId) != null) {
                            continue;
                        }
                        if (size != null) {
                            page.writeStart("tr",
                                    "data-size-name", size.getInternalName(),
                                    "data-size-independent", size.isIndependent(),
                                    "data-size-width", size.getWidth(),
                                    "data-size-height", size.getHeight());
                                page.writeStart("th");
                                    page.write(page.h(size.getDisplayName()));
                                page.writeEnd();
                        } else {
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.write(page.h(cropId));
                                page.writeEnd();
                        }
                            page.writeStart("td");
                                page.writeTag("input",
                                        "name", page.h(cropsFieldName + "." + cropId + ".x"),
                                        "type", "text",
                                        "value", crop.getX());
                            page.writeEnd();
                            page.writeStart("td");
                                page.writeTag("input",
                                        "name", page.h(cropsFieldName + "." + cropId + ".y"),
                                        "type", "text",
                                        "value", crop.getY());
                            page.writeEnd();
                            page.writeStart("td");
                                page.writeTag("input",
                                        "name", page.h(cropsFieldName + "." + cropId + ".width"),
                                        "type", "text",
                                        "value", crop.getWidth());
                            page.writeEnd();
                            page.writeStart("td");
                                page.writeTag("input",
                                        "name", page.h(cropsFieldName + "." + cropId + ".height"),
                                        "type", "text",
                                        "value", crop.getHeight());
                            page.writeEnd();
                            page.writeStart("td");
                                page.writeTag("input",
                                        "name", page.h(cropsFieldName + "." + cropId + ".texts"),
                                        "type", "text",
                                        "value", crop.getTexts());
                            page.writeEnd();
                            page.writeStart("td");
                                page.writeTag("input",
                                        "name", page.h(cropsFieldName + "." + cropId + ".textSizes"),
                                        "type", "text",
                                        "value", crop.getTextSizes());
                            page.writeEnd();
                            page.writeStart("td");
                                page.writeTag("input",
                                        "name", page.h(cropsFieldName + "." + cropId + ".textXs"),
                                        "type", "text",
                                        "value", crop.getTextXs());
                            page.writeEnd();
                            page.writeStart("td");
                                page.writeTag("input",
                                        "name", page.h(cropsFieldName + "." + cropId + ".textYs"),
                                        "type", "text",
                                        "value", crop.getTextYs());
                            page.writeEnd();
                            page.writeStart("td");
                                page.writeTag("input",
                                        "name", page.h(cropsFieldName + "." + cropId + ".textWidths"),
                                        "type", "text",
                                        "value", crop.getTextWidths());
                            page.writeEnd();
                        page.writeEnd();
                    }
                page.writeEnd();
            page.writeEnd();
        page.writeEnd();
    }

    /**
     * Writes the image preview
     * @param page
     * @param fieldValue
     * @throws IOException
     */
    private void writeImageEditorImage(ToolPageContext page, StorageItem fieldValue) throws IOException {

        String fieldValueUrl;
        String resizeScale = "";

        if (ImageEditor.Static.getDefault() != null) {
           ImageTag.Builder imageTagBuilder = new ImageTag.Builder(fieldValue)
                   .setWidth(1000)
                   .setResizeOption(ResizeOption.ONLY_SHRINK_LARGER)
                   .setEdits(false);
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
                    resizeScale = String.format("%.2f", (double) 1000 / originalWidth.intValue());
                }
            }
            fieldValueUrl = imageTagBuilder.toUrl();
        } else {
            fieldValueUrl = fieldValue.getPublicUrl();
        }

        page.writeStart("div", "class", "imageEditor-image");
            page.writeTag("img",
                    "alt", "",
                    "data-scale", resizeScale,
                    "src", page.url("/misc/proxy.jsp",
                            "url", fieldValueUrl,
                            "hash", StringUtils.hex(StringUtils.hmacSha1(Settings.getSecret(), fieldValueUrl))));
        page.writeEnd();
    }

    private enum ImageAdjustment {

        BRIGHTNESS("brightness", -1.0, 1.0, 0.01, double.class),
        CONTRAST("contrast", -1.0, 1.0, 0.01, double.class),
        FLIP_H("flipH"),
        FLIP_V("flipV"),
        INVERT("invert"),
        GRAYSCALE("grayscale"),
        ROTATE("rotate", -90.0, 90.0, 90.0, int.class),
        SEPIA("sepia"),
        SHARPEN("sharpen", true),
        BLUR("blur");

        private String title;
        private String inputType;
        private Class valueType;
        private boolean javaImageEditorOnly = false;
        private double min;
        private double max;
        private double step;

        ImageAdjustment(String title) {
            this.title = title;
            this.inputType = "checkbox";
            this.valueType = boolean.class;
            this.javaImageEditorOnly = false;
        }

        ImageAdjustment(String title, boolean javaImageEditorOnly) {
            this.title = title;
            this.inputType = "checkbox";
            this.valueType = boolean.class;
            this.javaImageEditorOnly = javaImageEditorOnly;
        }

        ImageAdjustment(String title, double min, double max, double step, Class valueType) {
            this.title = title;
            this.min = min;
            this.max = max;
            this.step = step;
            this.inputType = "range";
            this.valueType = valueType;
        }
    }
}
