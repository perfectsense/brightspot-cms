package com.psddev.cms.image;

import java.util.List;

public interface ImageSizeProvider {

    /**
     * Returns an image size appropriate for use with the given {@code field}
     * in the given {@code contexts}.
     *
     * @param contexts Nullable.
     * @param field Nullable.
     * @return Nullable.
     */
    ImageSize get(List<String> contexts, String field);
}
