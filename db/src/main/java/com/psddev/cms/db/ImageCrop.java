package com.psddev.cms.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.psddev.dari.db.Record;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeReference;

@Record.Embedded
public class ImageCrop extends Record {

    private static final String DELIMITER = "aaaf7c5a9e604daaa126f11e23e321d8";

    private double x;
    private double y;
    private double width;
    private double height;
    private List<ImageTextOverlay> textOverlays;

    @SuppressWarnings("unchecked")
    public static Map<String, ImageCrop> createCrops(Object crops) {
        Map<String, ImageCrop> cropsMap = new HashMap<>();

        if (crops instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) crops).entrySet()) {
                Object crop = entry.getValue();

                cropsMap.put(entry.getKey(), crop instanceof Map
                        ? new ImageCrop((Map<String, Object>) crop)
                        : new ImageCrop());
            }
        }

        return cropsMap;
    }

    public ImageCrop() {
    }

    public ImageCrop(Map<String, Object> map) {
        getState().setId(ObjectUtils.to(UUID.class, map.get("_id")));
        x = ObjectUtils.to(double.class, map.get("x"));
        y = ObjectUtils.to(double.class, map.get("y"));
        width = ObjectUtils.to(double.class, map.get("width"));
        height = ObjectUtils.to(double.class, map.get("height"));
        textOverlays = ObjectUtils.to(new TypeReference<List<ImageTextOverlay>>() { }, map.get("textOverlays"));
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public List<ImageTextOverlay> getTextOverlays() {
        if (textOverlays == null) {
            textOverlays = new ArrayList<ImageTextOverlay>();
        }
        return textOverlays;
    }

    public void setTextOverlays(List<ImageTextOverlay> textOverlays) {
        this.textOverlays = textOverlays;
    }

    private ImageTextOverlay getOrCreateTextOverlay(int index) {
        List<ImageTextOverlay> textOverlays = getTextOverlays();

        for (int i = textOverlays.size(); i <= index; ++ i) {
            textOverlays.add(new ImageTextOverlay());
        }

        return textOverlays.get(index);
    }

    public String getTexts() {
        StringBuilder texts = new StringBuilder();

        for (ImageTextOverlay textOverlay : getTextOverlays()) {
            String text = textOverlay.getText();
            texts.append(DELIMITER);
            if (text != null) {
                texts.append(text);
            }
        }

        return texts.toString();
    }

    public void setTexts(String texts) {
        if (texts != null) {
            String[] split = texts.split(DELIMITER, -1);
            for (int i = 1, length = split.length; i < length; ++ i) {
                getOrCreateTextOverlay(i - 1).setText(split[i]);
            }
        }
    }

    public String getTextSizes() {
        StringBuilder textSizes = new StringBuilder();

        for (ImageTextOverlay textOverlay : getTextOverlays()) {
            double textSize = textOverlay.getSize();
            textSizes.append(DELIMITER);
            textSizes.append(textSize);
        }

        return textSizes.toString();
    }

    public void setTextSizes(String textSizes) {
        if (textSizes != null) {
            String[] split = textSizes.split(DELIMITER, -1);
            for (int i = 1, length = split.length; i < length; ++ i) {
                getOrCreateTextOverlay(i - 1).setSize(ObjectUtils.to(double.class, split[i]));
            }
        }
    }

    public String getTextXs() {
        StringBuilder textXs = new StringBuilder();

        for (ImageTextOverlay textOverlay : getTextOverlays()) {
            double textX = textOverlay.getX();
            textXs.append(DELIMITER);
            textXs.append(textX);
        }

        return textXs.toString();
    }

    public void setTextXs(String textXs) {
        if (textXs != null) {
            String[] split = textXs.split(DELIMITER, -1);
            for (int i = 1, length = split.length; i < length; ++ i) {
                getOrCreateTextOverlay(i - 1).setX(ObjectUtils.to(double.class, split[i]));
            }
        }
    }

    public String getTextYs() {
        StringBuilder textYs = new StringBuilder();

        for (ImageTextOverlay textOverlay : getTextOverlays()) {
            double textY = textOverlay.getY();
            textYs.append(DELIMITER);
            textYs.append(textY);
        }

        return textYs.toString();
    }

    public void setTextYs(String textYs) {
        if (textYs != null) {
            String[] split = textYs.split(DELIMITER, -1);
            for (int i = 1, length = split.length; i < length; ++ i) {
                getOrCreateTextOverlay(i - 1).setY(ObjectUtils.to(double.class, split[i]));
            }
        }
    }

    public String getTextWidths() {
        StringBuilder textWidths = new StringBuilder();

        for (ImageTextOverlay textOverlay : getTextOverlays()) {
            double textWidth = textOverlay.getWidth();
            textWidths.append(DELIMITER);
            textWidths.append(textWidth);
        }

        return textWidths.toString();
    }

    public void setTextWidths(String textWidths) {
        if (textWidths != null) {
            String[] split = textWidths.split(DELIMITER, -1);
            for (int i = 1, length = split.length; i < length; ++ i) {
                getOrCreateTextOverlay(i - 1).setWidth(ObjectUtils.to(double.class, split[i]));
            }
        }
    }
}
