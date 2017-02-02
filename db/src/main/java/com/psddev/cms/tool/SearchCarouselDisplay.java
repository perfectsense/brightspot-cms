package com.psddev.cms.tool;

public enum SearchCarouselDisplay {

    HORIZONTAL("Horizontal"),
    DISABLED("Disabled");

    private final String label;

    SearchCarouselDisplay(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
