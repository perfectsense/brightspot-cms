package com.psddev.cms.image;

/**
 * Builder that can be used to create an {@link ImageSize} instance.
 *
 * @see ImageSize#builder()
 */
public final class ImageSizeBuilder {

    private int width;
    private int height;

    ImageSizeBuilder() {
    }

    public ImageSizeBuilder width(int width) {
        this.width = width;
        return this;
    }

    public ImageSizeBuilder height(int height) {
        this.height = height;
        return this;
    }

    /**
     * Creates an {@link ImageSize} instance.
     *
     * @return Nonnull.
     */
    public ImageSize build() {
        return new ImageSize(width, height);
    }
}
