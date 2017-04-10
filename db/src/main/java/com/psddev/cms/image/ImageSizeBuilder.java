package com.psddev.cms.image;

/**
 * Builder that can be used to create an {@link ImageSize} instance.
 *
 * @see ImageSize#builder()
 */
public final class ImageSizeBuilder {

    private String group;
    private String internalName;
    private String displayName;
    private int width;
    private int height;
    private String format;
    private int quality;

    ImageSizeBuilder() {
    }

    public ImageSizeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public ImageSizeBuilder internalName(String internalName) {
        this.internalName = internalName;
        return this;
    }

    public ImageSizeBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ImageSizeBuilder width(int width) {
        this.width = width;
        return this;
    }

    public ImageSizeBuilder height(int height) {
        this.height = height;
        return this;
    }

    public ImageSizeBuilder format(String format) {
        this.format = format;
        return this;
    }

    public ImageSizeBuilder quality(int quality) {
        this.quality = quality;
        return this;
    }

    /**
     * Creates an {@link ImageSize} instance.
     *
     * @return Nonnull.
     */
    public ImageSize build() {
        return new ImageSize(
                group,
                internalName,
                displayName,
                width,
                height,
                format,
                quality);
    }
}
