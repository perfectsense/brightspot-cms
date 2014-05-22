package com.psddev.cms.db;

import com.psddev.dari.util.ImageEditor;

public enum CropOption {

    NONE("None", ImageEditor.CROP_OPTION_NONE),
    AUTOMATIC("Automatic", ImageEditor.CROP_OPTION_AUTOMATIC),
    CIRCLE("Circle", ImageEditor.CROP_OPTION_CIRCLE),
    STAR("Star", ImageEditor.CROP_OPTION_STAR),
    STARBURST("Starburst", ImageEditor.CROP_OPTION_STARBURST);

    private final String displayName;
    private final String imageEditorOption;

    private CropOption(String displayName, String imageEditorOption) {
        this.displayName = displayName;
        this.imageEditorOption = imageEditorOption;
    }

    public String getImageEditorOption() {
        return imageEditorOption;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static final class Static {

        private Static() {
        }

        public static CropOption fromImageEditorOption(String imageEditorOption) {
            for (CropOption option : CropOption.values()) {
                if (option.getImageEditorOption().equals(imageEditorOption)) {
                    return option;
                }
            }
            return null;
        }
    }
}
