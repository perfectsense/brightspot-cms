package com.psddev.cms.image;

import java.util.List;
import java.util.Set;

public interface ImageSizeProvider {

    /**
     * Returns all image sizes.
     *
     * @return Nullable.
     */
    Set<ImageSize> getAll();

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
