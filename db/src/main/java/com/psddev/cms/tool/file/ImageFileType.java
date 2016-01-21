package com.psddev.cms.tool.file;

import java.io.IOException;
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
import com.psddev.dari.db.Query;
import com.psddev.dari.db.ReferentialText;
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

    /**
     * Processes metadata inputs, and adds resulting metadata and edits
     * to image StorageItem.
     *
     */
    @Override
    public void process(ToolPageContext page, StorageItem storageItem) {

        if (storageItem == null) {
            return;
        }

        String inputName = ObjectUtils.firstNonBlank((String) page.getRequest().getAttribute("inputName"), page.param(String.class, "inputName"));

        // Edits.
        double brightness = page.param(double.class, inputName + ".brightness");
        double contrast = page.param(double.class, inputName + ".contrast");
        boolean flipH = page.param(boolean.class, inputName + ".flipH");
        boolean flipV = page.param(boolean.class, inputName + ".flipV");
        boolean grayscale = page.param(boolean.class, inputName + ".grayscale");
        boolean invert = page.param(boolean.class, inputName + ".invert");
        int rotate = page.param(int.class, inputName + ".rotate");
        boolean sepia = page.param(boolean.class, inputName + ".sepia");
        int sharpen = page.param(int.class, inputName + ".sharpen");

        Map<String, Object> edits = new HashMap<>();

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

        String blurName = inputName + ".blur";
        if (!ObjectUtils.isBlank(page.params(String.class, blurName))) {
            List<String> blurs = new ArrayList<>();
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

        Map<String, Object> fieldValueMetadata = new LinkedHashMap<>(storageItem.getMetadata());

        // Crops.
        Map<String, ImageCrop> crops = ObjectUtils.firstNonNull(
                ObjectUtils.to(new TypeReference<Map<String, ImageCrop>>() { }, fieldValueMetadata.get("cms.crops")),
                new HashMap<>()
        );

        crops = new TreeMap<>(crops);

        for (StandardImageSize size : Query.from(StandardImageSize.class).selectAll()) {
            String sizeId = size.getId().toString();
            if (crops.get(sizeId) == null) {
                crops.put(sizeId, new ImageCrop());
            }
        }

        // Standard sizes.
        String cropsName = inputName + ".crops.";
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

        // Focus Point.
        Map<String, Double> focusPoint = new HashMap<>();
        Double focusX = page.paramOrDefault(Double.class, inputName + ".focusX", null);
        Double focusY = page.paramOrDefault(Double.class, inputName + ".focusY", null);
        if (focusX != null && focusY != null) {

            // Handle legacy focus points stored as a value 1-100, instead of 0-1
            if (focusX > 1 && focusX < 100) {
                focusX /= 100;
            }

            if (focusY > 1 && focusY < 100) {
                focusY /= 100;
            }

            focusPoint.put("x", focusX);
            focusPoint.put("y", focusY);
        }

        fieldValueMetadata.put("cms.edits", edits);
        fieldValueMetadata.put("cms.crops", crops);
        fieldValueMetadata.put("cms.focus", focusPoint);

        storageItem.setMetadata(fieldValueMetadata);
    }

    @Override
    public void writePreview(ToolPageContext page, State state, StorageItem storageItem) throws IOException, ServletException {

        if (storageItem == null) {
            return;
        }

        HttpServletRequest request = page.getRequest();

        String inputName = page.paramOrDefault(String.class, "inputName", (String) request.getAttribute("inputName"));
        String originalWidthName = inputName + ".originalWidth";
        String cropsName = inputName + ".crops.";

        Map<String, Object> metadata = ObjectUtils.firstNonNull(
                ObjectUtils.to(new TypeReference<Map<String, Object>>() { }, storageItem.getMetadata()),
                new LinkedHashMap<>()
        );

        // Edits.
        Map<String, Object> edits = ObjectUtils.firstNonNull(
                ObjectUtils.to(new TypeReference<Map<String, Object>>() { }, metadata.get("cms.edits")),
                new HashMap<>()
        );

        // Crops and StandardImageSizes.
        Map<String, ImageCrop> crops = ObjectUtils.firstNonNull(
                ObjectUtils.to(new TypeReference<TreeMap<String, ImageCrop>>() { }, metadata.get("cms.crops")),
                new HashMap<>()
        );

        crops = new TreeMap<>(crops);

        Map<String, StandardImageSize> sizes = new HashMap<>();
        for (StandardImageSize size : Query.from(StandardImageSize.class).selectAll()) {
            String sizeId = size.getId().toString();
            sizes.put(sizeId, size);
            if (crops.get(sizeId) == null) {
                crops.put(sizeId, new ImageCrop());
            }
        }

        // Focus Point.
        Map<String, Double> focusPoint = ObjectUtils.firstNonNull(
                ObjectUtils.to(new TypeReference<Map<String, Double>>() { }, metadata.get("cms.focus")),
                new HashMap<>()
        );

        page.writeStart("div",
                "class", "imageEditor");
            page.writeStart("div", "class", "imageEditor-aside");
                page.writeStart("div", "class", "imageEditor-tools");

                    page.writeStart("h2");
                        page.writeHtml(page.localize(ImageFileType.class, "subtitle.tools"));
                    page.writeEnd();

                    page.writeStart("ul");
                        if (state.as(ColorDistribution.Data.class).getDistribution() != null) {
                            page.writeStart("li");
                                page.writeStart("a",
                                        "class", "icon icon-tint",
                                        "href", page.h(page.cmsUrl("/contentColors", "id", state.getId())),
                                        "target", "contentColors");
                                    page.writeHtml(page.localize(ImageFileType.class, "action.viewContentColors"));
                                page.writeEnd();
                            page.writeEnd();
                        }

                        page.writeStart("li");
                            page.writeStart("a",
                                    "class", "action-preview",
                                    "href", storageItem.getPublicUrl(),
                                    "target", "_blank");
                                page.writeHtml(page.localize(ImageFileType.class, "action.viewOriginal"));
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("li");
                            page.writeStart("a",
                                    "class", "icon icon-crop",
                                    "href", page.h(page.url("/contentImages", "data", ObjectUtils.toJson(storageItem))),
                                    "target", "contentImages");
                                page.writeHtml(page.localize(ImageFileType.class, "action.viewResized"));
                            page.writeEnd();
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("div", "class", "imageEditor-edit");
                    page.writeStart("h2");
                        page.writeHtml(page.localize(ImageFileType.class, "subtitle.adjustments"));
                    page.writeEnd();

                    boolean usingJavaImageEditor = ImageEditor.Static.getDefault() != null && (ImageEditor.Static.getDefault() instanceof JavaImageEditor);

                    page.writeStart("table");
                        page.writeStart("tbody");
                            if (usingJavaImageEditor) {

                                String blurName = inputName + ".blur";

                                // Blurs (only with JavaImageEditor).
                                List<String> blurs = new ArrayList<>();
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

                                page.writeStart("tr");
                                    page.writeStart("th");
                                        page.writeHtml(page.localize(ImageFileType.class, "label.blur"));
                                    page.writeEnd();

                                    page.writeStart("td");
                                        page.writeStart("a", "class", "imageEditor-addBlurOverlay");
                                            page.writeHtml(page.localize(ImageFileType.class, "action.addBlur"));
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

                            String brightnessName = inputName + ".brightness";
                            String contrastName = inputName + ".contrast";
                            String flipHName = inputName + ".flipH";
                            String flipVName = inputName + ".flipV";
                            String grayscaleName = inputName + ".grayscale";
                            String invertName = inputName + ".invert";
                            String rotateName = inputName + ".rotate";
                            String sepiaName = inputName + ".sepia";

                            double brightness = ObjectUtils.to(double.class, edits.get("brightness"));
                            double contrast = ObjectUtils.to(double.class, edits.get("contrast"));
                            boolean flipH = ObjectUtils.to(boolean.class, edits.get("flipH"));
                            boolean flipV = ObjectUtils.to(boolean.class, edits.get("flipV"));
                            boolean grayscale = ObjectUtils.to(boolean.class, edits.get("grayscale"));
                            boolean invert = ObjectUtils.to(boolean.class, edits.get("invert"));
                            int rotate = ObjectUtils.to(int.class, edits.get("rotate"));
                            boolean sepia = ObjectUtils.to(boolean.class, edits.get("sepia"));

                            // Brightness
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.writeHtml(page.localize(ImageFileType.class, "label.brightness"));
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "range", "name", page.h(brightnessName), "value", page.h(brightness), "min", "-1.0", "max", "1.0", "step", "0.01");
                                page.writeEnd();
                            page.writeEnd();

                            // Contrast
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.writeHtml(page.localize(ImageFileType.class, "label.contrast"));
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "range", "name", page.h(contrastName), "value", page.h(contrast), "min", "-1.0", "max", "1.0", "step", "0.01");
                                page.writeEnd();
                            page.writeEnd();

                            // Flip H
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.writeHtml(page.localize(ImageFileType.class, "label.flipH"));
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "checkbox", "name", page.h(flipHName), "value", page.h("true"), flipH ? "checked" : "", "");
                                page.writeEnd();
                            page.writeEnd();

                            // Flip V
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.writeHtml(page.localize(ImageFileType.class, "label.flipV"));
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "checkbox", "name", page.h(flipVName), "value", page.h("true"), flipV ? "checked" : "", "");
                                page.writeEnd();
                            page.writeEnd();

                            // Invert
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.writeHtml(page.localize(ImageFileType.class, "label.invert"));
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "checkbox", "name", page.h(invertName), "value", page.h("true"), invert ? "checked" : "", "");
                                page.writeEnd();
                            page.writeEnd();

                            // Grayscale
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.writeHtml(page.localize(ImageFileType.class, "label.grayscale"));
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "checkbox", "name", page.h(grayscaleName), "value", page.h("true"), grayscale ? "checked" : "", "");
                                page.writeEnd();
                            page.writeEnd();

                            // Rotate
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.writeHtml(page.localize(ImageFileType.class, "label.rotate"));
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "range", "name", page.h(rotateName), "value", page.h(rotate), "min", "-90", "max", "90", "step", "90");
                                page.writeEnd();
                            page.writeEnd();

                            // Sepia
                            page.writeStart("tr");
                                page.writeStart("th");
                                    page.writeHtml(page.localize(ImageFileType.class, "label.sepia"));
                                page.writeEnd();
                                page.writeStart("td");
                                    page.writeTag("input", "type", "checkbox", "name", page.h(sepiaName), "value", page.h("true"), sepia ? "checked" : "", "");
                                page.writeEnd();
                            page.writeEnd();

                            if (usingJavaImageEditor) {

                                String sharpenName = inputName + ".sharpen";
                                int sharpen = ObjectUtils.to(int.class, edits.get("sharpen"));

                                // Sharpen
                                page.writeStart("tr");
                                page.writeStart("th");
                                page.writeHtml(page.localize(ImageFileType.class, "label.sharpen"));
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
                            page.writeHtml(page.localize(ImageFileType.class, "subtitle.sizes"));
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
                    ImageTag.Builder imageTagBuilder = new ImageTag.Builder(storageItem)
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
                    fieldValueUrl = storageItem.getPublicUrl();
                }
                page.writeTag("img",
                        "alt", "",
                        "data-scale", resizeScale,
                        "src", page.url("/misc/proxy.jsp",
                                "url", fieldValueUrl,
                                "hash", StringUtils.hex(StringUtils.hmacSha1(Settings.getSecret(), fieldValueUrl))));

                //TODO: move focus inputs inside .imageEditor.edits
                page.writeTag("input",
                        "type", "hidden",
                        "name", page.h(inputName + ".focusX"),
                        "value", page.h(focusPoint.containsKey("x") ? focusPoint.get("x") : ""));
                page.writeTag("input",
                        "type", "hidden",
                        "name", page.h(inputName + ".focusY"),
                        "value", page.h(focusPoint.containsKey("y") ? focusPoint.get("y") : ""));
            page.writeEnd();
        page.writeEnd();
    }
}
