package com.psddev.cms.image;

import com.psddev.dari.util.DimsImageEditor;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;

import java.util.Map;

class DimsImageUrl {

    private String baseUrl;
    private String sharedSecret;
    private String originalUrl;
    private Integer cropLeft;
    private Integer cropTop;
    private Integer cropWidth;
    private Integer cropHeight;
    private Integer resizeWidth;
    private Integer resizeHeight;
    private ImageFilter filter;
    private String format;
    private int quality;

    public DimsImageUrl() {
    }

    public DimsImageUrl(DimsImageEditor editor, StorageItem image, ImageSize size) {
        baseUrl = editor.getBaseUrl();
        sharedSecret = editor.getSharedSecret();

        setOriginalUrl(image.getPublicUrl());

        // Absolute final size.
        Map<String, Object> metadata = image.getMetadata();
        double originalWidth = ObjectUtils.to(double.class, metadata.get("width"));
        double originalHeight = ObjectUtils.to(double.class, metadata.get("height"));
        double resizeWidth = size.getWidth();
        double resizeHeight = size.getHeight();

        // Try to figure out the crop based on the original dimensions.
        double focusX;
        double focusY;
        double cropWidth;
        double cropHeight;

        // Editorial crop?
        @SuppressWarnings("unchecked")
        Map<String, Object> crops = (Map<String, Object>) metadata.get("cms.crops");
        @SuppressWarnings("unchecked")
        Map<String, Object> crop = crops != null ? (Map<String, Object>) crops.get(size.getInternalName()) : null;

        if (crop != null) {
            double w = ObjectUtils.to(double.class, crop.get("width"));
            double h = ObjectUtils.to(double.class, crop.get("height"));
            focusX = originalWidth * (ObjectUtils.to(double.class, crop.get("x")) + (w / 2));
            focusY = originalHeight * (ObjectUtils.to(double.class, crop.get("y")) + (h / 2));
            cropWidth = originalWidth * w;
            cropHeight = originalHeight * h;

            // Auto width or height.
            if (resizeWidth <= 0.0) {
                if (resizeHeight <= 0.0) {
                    resizeWidth = cropWidth;
                    resizeHeight = cropHeight;

                } else {
                    resizeWidth = resizeHeight / cropHeight * cropWidth;
                }

            } else if (resizeHeight <= 0.0) {
                resizeHeight = resizeWidth / cropWidth * cropHeight;

            } else {

                // Crop width and height could be slightly wrong at this point.
                double s = Math.sqrt(cropWidth * cropHeight / resizeWidth / resizeHeight);
                cropWidth = resizeWidth * s;
                cropHeight = resizeHeight * s;
            }

        } else {

            // Use the focus point or default to center.
            @SuppressWarnings("unchecked")
            Map<String, Object> focus = (Map<String, Object>) metadata.get("cms.focus");
            focusX = originalWidth * ObjectUtils.firstNonNull(ObjectUtils.to(Double.class, focus.get("x")), 0.5);
            focusY = originalHeight * ObjectUtils.firstNonNull(ObjectUtils.to(Double.class, focus.get("y")), 0.5);

            // Auto width or height.
            if (resizeWidth <= 0.0) {
                if (resizeHeight <= 0.0) {
                    resizeWidth = originalWidth;
                    resizeHeight = originalHeight;

                } else {
                    resizeWidth = resizeHeight / originalHeight * originalWidth;
                }

            } else if (resizeHeight <= 0.0) {
                resizeHeight = resizeWidth / originalWidth * originalHeight;
            }

            double s = Math.min(originalWidth / resizeWidth, originalHeight / resizeHeight);
            cropWidth = resizeWidth * s;
            cropHeight = resizeHeight * s;
        }

        // Make sure that the crop is within the original bounds.
        double cropLeft = focusX - (cropWidth / 2.0);

        if (cropLeft < 0.0) {
            cropLeft = 0.0;

        } else if (cropLeft + cropWidth > originalWidth) {
            cropLeft = originalWidth - cropWidth;
        }

        double cropTop = focusY - (cropHeight / 2.0);

        if (cropTop < 0.0) {
            cropTop = 0.0;

        } else if (cropTop + cropHeight > originalHeight) {
            cropTop = originalHeight - cropHeight;
        }

        setCropLeft((int) Math.round(cropLeft));
        setCropTop((int) Math.round(cropTop));
        setCropWidth((int) Math.round(cropWidth));
        setCropHeight((int) Math.round(cropHeight));
        setResizeWidth((int) Math.round(resizeWidth));
        setResizeHeight((int) Math.round(resizeHeight));

        @SuppressWarnings("unchecked")
        Map<String, Object> edits = (Map<String, Object>) metadata.get("cms.edits");

        if (edits != null) {
            if (ObjectUtils.to(boolean.class, edits.get("grayscale"))) {
                setFilter(ImageFilter.GRAYSCALE);

            } else if (ObjectUtils.to(boolean.class, edits.get("invert"))) {
                setFilter(ImageFilter.INVERT);

            } else if (ObjectUtils.to(boolean.class, edits.get("sepia"))) {
                setFilter(ImageFilter.SEPIA);
            }
        }

        setFormat(size.getFormat());
        setQuality(size.getQuality());
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public Integer getCropLeft() {
        return cropLeft;
    }

    public void setCropLeft(Integer cropLeft) {
        this.cropLeft = cropLeft;
    }

    public Integer getCropTop() {
        return cropTop;
    }

    public void setCropTop(Integer cropTop) {
        this.cropTop = cropTop;
    }

    public Integer getCropWidth() {
        return cropWidth;
    }

    public void setCropWidth(Integer cropWidth) {
        this.cropWidth = cropWidth;
    }

    public Integer getCropHeight() {
        return cropHeight;
    }

    public void setCropHeight(Integer cropHeight) {
        this.cropHeight = cropHeight;
    }

    public Integer getResizeWidth() {
        return resizeWidth;
    }

    public void setResizeWidth(Integer resizeWidth) {
        this.resizeWidth = resizeWidth;
    }

    public Integer getResizeHeight() {
        return resizeHeight;
    }

    public void setResizeHeight(Integer resizeHeight) {
        this.resizeHeight = resizeHeight;
    }

    public ImageFilter getFilter() {
        return filter;
    }

    public void setFilter(ImageFilter filter) {
        this.filter = filter;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    public String toUrl() {
        StringBuilder commands = new StringBuilder();

        commands.append("strip/true/");

        Integer cropWidth = getCropWidth();
        Integer cropHeight = getCropHeight();

        if (cropWidth != null && cropHeight != null) {
            Integer cropLeft = getCropLeft();
            Integer cropTop = getCropTop();

            commands.append("crop/");
            commands.append(cropWidth);
            commands.append('x');
            commands.append(cropHeight);
            commands.append('+');
            commands.append(cropLeft != null ? cropLeft : 0);
            commands.append('+');
            commands.append(cropTop != null ? cropTop : 0);
            commands.append('/');
        }

        Integer resizeWidth = getResizeWidth();
        Integer resizeHeight = getResizeHeight();

        if (resizeWidth != null || resizeHeight != null) {
            commands.append("resize/");

            if (resizeWidth != null) {
                commands.append(resizeWidth);
                commands.append('x');

                if (resizeHeight != null) {
                    commands.append(resizeHeight);
                    commands.append('!');

                } else {
                    commands.append('^');
                }

            } else {
                commands.append('x');
                commands.append(resizeHeight);
                commands.append('^');
            }

            commands.append('/');
        }

        ImageFilter filter = getFilter();

        if (filter == ImageFilter.GRAYSCALE) {
            commands.append("grayscale/true/");

        } else if (filter == ImageFilter.INVERT) {
            commands.append("invert/true/");

        } else if (filter == ImageFilter.SEPIA) {
            commands.append("sepia/0.8/");
        }

        String format = getFormat();

        if (!StringUtils.isBlank(format)) {
            commands.append("format/");
            commands.append(StringUtils.encodeUri(format));
            commands.append('/');
        }

        Integer quality = getQuality();

        if (quality != null && quality > 0) {
            commands.append("quality/");
            commands.append(quality);
            commands.append('/');
        }

        StringBuilder url = new StringBuilder();

        if (baseUrl != null) {
            url.append(StringUtils.ensureEnd(baseUrl, "/"));
        }

        int expire = Integer.MAX_VALUE;
        String originalUrl = getOriginalUrl();

        if (originalUrl.startsWith("/")) {
            originalUrl = "http://localhost" + originalUrl;
        }

        if (sharedSecret != null) {
            String signature = expire + sharedSecret + commands + originalUrl;

            url.append(StringUtils.hex(StringUtils.md5(signature)).substring(0, 7));
            url.append('/');
            url.append(expire);
            url.append('/');
        }

        url.append(commands);
        url.append("?url=");
        url.append(StringUtils.encodeUri(originalUrl));

        return url.toString();
    }
}
