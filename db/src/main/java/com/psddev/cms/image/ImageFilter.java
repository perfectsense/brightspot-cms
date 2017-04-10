package com.psddev.cms.image;

enum ImageFilter {

    GRAYSCALE("Grayscale"),
    INVERT("Invert"),
    SEPIA("Sepia");

    private final String label;

    ImageFilter(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
