package com.psddev.cms.tool;

public enum GalleryDisplay {

    VERTICAL("Vertical");

    private final String label;

    GalleryDisplay(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
