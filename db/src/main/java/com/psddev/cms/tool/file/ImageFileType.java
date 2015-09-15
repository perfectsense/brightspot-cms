package com.psddev.cms.tool.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.psddev.cms.db.ImageCrop;
import com.psddev.cms.db.ImageTag;
import com.psddev.cms.db.ResizeOption;
import com.psddev.cms.db.StandardImageSize;
import com.psddev.cms.tool.FileContentType;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ColorDistribution;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CollectionUtils;
import com.psddev.dari.util.DimsImageEditor;
import com.psddev.dari.util.ImageEditor;
import com.psddev.dari.util.JavaImageEditor;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeReference;

public class ImageFileType implements FileContentType {

    @Override
    public double getPriority(StorageItem storageItem) {
        String contentType = storageItem.getContentType();

        if (StringUtils.isBlank(contentType) || !contentType.startsWith("image/")) {
            return DEFAULT_PRIORITY_LEVEL - 1;
        }

        return DEFAULT_PRIORITY_LEVEL;
    }

    @Override
    public void writePreview(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException {

        HttpServletRequest request = page.getRequest();

        ObjectField field = (ObjectField) request.getAttribute("field");

        String fieldName;
        if (field != null) {
            fieldName = field.getInternalName();
        } else {
            fieldName = page.param(String.class, "fieldName");
        }

        String inputName = page.paramOrDefault(String.class, "inputName", (String) request.getAttribute("inputName"));
        String originalWidthName = inputName + ".originalWidth";
        String actionName = inputName + ".action";
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

        String cropsFieldName = fieldName + ".crops";

        String action = page.param(actionName);

        Map<String, Object> fieldValueMetadata = null;
        boolean isFormPost = request.getAttribute("isFormPost") != null ? (Boolean) request.getAttribute("isFormPost") : false;
        if (fieldValue != null && (!isFormPost || "keep".equals(action))) {
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

        Map<String, ImageCrop> crops = ObjectUtils.to(new TypeReference<Map<String, ImageCrop>>() {
        }, fieldValueMetadata.get("cms.crops"));
        if (crops == null) {
            // for backward compatibility
            crops = ObjectUtils.to(new TypeReference<Map<String, ImageCrop>>() {
            }, state.getValue(cropsFieldName));
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

        Map<String, Double> focusPoint = ObjectUtils.to(new TypeReference<Map<String, Double>>() {
        }, fieldValueMetadata.get("cms.focus"));

        if (focusPoint == null) {
            focusPoint = new HashMap<String, Double>();
        }

        page.writeStart("div",
                "class", "imageEditor");
            page.writeStart("div", "class", "imageEditor-aside");
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
                                    "href", fieldValue.getPublicUrl(),
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

                page.writeStart("div", "class", "imageEditor-edit");
                    page.writeStart("h2");
                        page.write("Adjustments");
                    page.writeEnd();

                    boolean usingJavaImageEditor = ImageEditor.Static.getDefault() != null && (ImageEditor.Static.getDefault() instanceof JavaImageEditor);

                    page.writeStart("table");
                        page.writeStart("tbody");
                            if (usingJavaImageEditor) {
                                page.writeStart("tr");
                                    page.writeStart("th");
                                        page.write("Blur");
                                    page.writeEnd();

                                    page.writeStart("td");
                                        page.writeStart("a", "class", "imageEditor-addBlurOverlay");
                                            page.write("Add Blur");
                                        page.writeEnd();
                                        page.writeTag("br");

                                        if (!ObjectUtils.isBlank(blurs)) {
                                            for (String blur : blurs) {
                                                page.writeTag("input", "type", "hidden", "name", page.h(blurName), "value", page.h(blur));
                                            }
                                        }
                                    page.writeEnd();
                                page.writeEnd();
                            }

                            // Brightness
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.write("Brightness");
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "range", "name", page.h(brightnessName), "value", page.h(brightness), "min", "-1.0", "max", "1.0", "step", "0.01");
                                page.writeEnd();
                            page.writeEnd();

                            // Contrast
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.write("Contrast");
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "range", "name", page.h(contrastName), "value", page.h(contrast), "min", "-1.0", "max", "1.0", "step", "0.01");
                                page.writeEnd();
                            page.writeEnd();

                            // Flip H
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.write("Flip H");
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "checkbox", "name", page.h(flipHName), "value", page.h("true"), flipH ? "checked" : "", "");
                                page.writeEnd();
                            page.writeEnd();

                            // Flip V
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.write("Flip V");
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "checkbox", "name", page.h(flipVName), "value", page.h("true"), flipV ? "checked" : "", "");
                                page.writeEnd();
                            page.writeEnd();

                            // Invert
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.write("Invert");
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "checkbox", "name", page.h(invertName), "value", page.h("true"), invert ? "checked" : "", "");
                                page.writeEnd();
                            page.writeEnd();

                            // Grayscale
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.write("Grayscale");
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "checkbox", "name", page.h(grayscaleName), "value", page.h("true"), grayscale ? "checked" : "", "");
                                page.writeEnd();
                            page.writeEnd();

                            // Rotate
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.write("Rotate");
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "range", "name", page.h(rotateName), "value", page.h(rotate), "min", "-90", "max", "90", "step", "90");
                                page.writeEnd();
                            page.writeEnd();

                            // Sepia
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.write("Sepia");
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "checkbox", "name", page.h(sepiaName), "value", page.h("true"), sepia ? "checked" : "", "");
                                page.writeEnd();
                            page.writeEnd();

                            if (usingJavaImageEditor) {
                                // Sharpen
                                page.writeStart("tr");
                                    page.writeStart("th");
                                        page.write("Sharpen");
                                    page.writeEnd();
                                    page.writeStart("td");
                                        page.writeTag("input", "type", "range", "name", page.h(sharpenName), "value", page.h(sharpen), "min", "0", "max", "10", "step", "1");
                                    page.writeEnd();
                                page.writeEnd();
                            }

                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();

                ImageEditor defaultImageEditor = ImageEditor.Static.getDefault();
                boolean centerCrop = !(defaultImageEditor instanceof DimsImageEditor) || ((DimsImageEditor) defaultImageEditor).isUseLegacyThumbnail();

                if (!crops.isEmpty()) {
                    page.writeStart("div", "class", "imageEditor-sizes");
                        page.writeStart("h2");
                            page.write("Standard Sizes");
                        page.writeEnd();
                        page.writeStart("table", "data-crop-center", page.h(centerCrop));
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
                                                "data-size-name", page.h(size.getInternalName()),
                                                "data-size-independent", page.h(size.isIndependent()),
                                                "data-size-width", page.h(size.getWidth()),
                                                "data-size-height", page.h(size.getHeight()));
                                            page.writeStart("th");
                                                page.write(page.h(size.getDisplayName()));
                                            page.writeEnd();
                                    } else {
                                        page.writeStart("tr");
                                            page.writeStart("th");
                                                page.write(page.h(cropId));
                                            page.writeEnd();
                                    }

                                    // Crop X
                                    page.writeStart("td");
                                        page.writeTag("input", "name", page.h(cropsName + cropId + ".x"), "type", "text", "value", crop.getX());
                                    page.writeEnd();

                                    // Crop Y
                                    page.writeStart("td");
                                        page.writeTag("input", "name", page.h(cropsName + cropId + ".y"), "type", "text", "value", crop.getY());
                                    page.writeEnd();

                                    // Crop Width
                                    page.writeStart("td");
                                        page.writeTag("input", "name", page.h(cropsName + cropId + ".width"), "type", "text", "value", crop.getWidth());
                                    page.writeEnd();

                                    // Crop Height
                                    page.writeStart("td");
                                        page.writeTag("input", "name", page.h(cropsName + cropId + ".height"), "type", "text", "value", crop.getHeight());
                                    page.writeEnd();

                                    // Crop Texts
                                    page.writeStart("td");
                                        page.writeTag("input", "name", page.h(cropsName + cropId + ".texts"), "type", "text", "value", page.h(crop.getTexts()));
                                    page.writeEnd();

                                    // Crop Texts Sizes
                                    page.writeStart("td");
                                        page.writeTag("input", "name", page.h(cropsName + cropId + ".textSizes"), "type", "text", "value", page.h(crop.getTextSizes()));
                                    page.writeEnd();

                                    // Crop Texts Xs
                                    page.writeStart("td");
                                        page.writeTag("input", "name", page.h(cropsName + cropId + ".textXs"), "type", "text", "value", crop.getTextXs());
                                    page.writeEnd();

                                    // Crop Texts Ys
                                    page.writeStart("td");
                                        page.writeTag("input", "name", page.h(cropsName + cropId + ".textYs"), "type", "text", "value", crop.getTextYs());
                                    page.writeEnd();

                                    // Crop Texts Widths
                                    page.writeStart("td");
                                        page.writeTag("input", "name", page.h(cropsName + cropId + ".textWidths"), "type", "text", "value", crop.getTextWidths());
                                    page.writeEnd();

                                    //end tr
                                    page.writeEnd();
                                }
                            page.writeEnd();
                        page.writeEnd();
                    page.writeEnd();
                }
            page.writeEnd();

            page.writeStart("div", "class", "imageEditor-image");

                String fieldValueUrl;
                String resizeScale = "";
                if (ImageEditor.Static.getDefault() != null) {
                    ImageTag.Builder imageTagBuilder = new ImageTag.Builder(fieldValue)
                            .setWidth(1000)
                            .setResizeOption(ResizeOption.ONLY_SHRINK_LARGER)
                            .setEdits(false);
                    Object originalWidthObject = ObjectUtils.firstNonBlank(
                        CollectionUtils.getByPath(imageTagBuilder.getItem().getMetadata(), "image/originalWidth"),
                        CollectionUtils.getByPath(imageTagBuilder.getItem().getMetadata(), "dims/originalWidth"),
                        CollectionUtils.getByPath(imageTagBuilder.getItem().getMetadata(), "width"),
                        page.param(String.class, originalWidthName)
                    );

                    int originalWidth;

                    if (originalWidthObject instanceof Number) {
                        originalWidth = ((Number) originalWidthObject).intValue();
                    } else {
                        originalWidth = ObjectUtils.to(double.class, originalWidthObject).intValue();
                    }

                    if (originalWidth > 1000) {
                        resizeScale = String.format("%.2f", (double) 1000 / originalWidth);
                    }
                    fieldValueUrl = imageTagBuilder.toUrl();
                } else {
                    fieldValueUrl = fieldValue.getPublicUrl();
                }
                page.writeTag("img",
                        "alt", "",
                        "data-scale", resizeScale,
                        "src", page.url("/misc/proxy.jsp",
                                "url", fieldValueUrl,
                                "hash", StringUtils.hex(StringUtils.hmacSha1(Settings.getSecret(), fieldValueUrl))));
                page.writeTag("input",
                        "type", "hidden",
                        "name", page.h(inputName + ".focusX"),
                        "value", page.h(focusPoint != null && focusPoint.containsKey("x") ? focusPoint.get("x") : ""));
                page.writeTag("input",
                        "type", "hidden",
                        "name", page.h(inputName + ".focusY"),
                        "value", page.h(focusPoint != null && focusPoint.containsKey("y") ? focusPoint.get("y") : ""));
            page.writeEnd();
        page.writeEnd();
    }
}
