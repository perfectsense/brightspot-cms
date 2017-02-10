package com.psddev.cms.rte;

import org.jsoup.nodes.Element;

/**
 * Interface that's used by {@link RichTextViewBuilder} to preprocess HTML
 * before it's converted into views.
 */
@FunctionalInterface
public interface RichTextPreprocessor {

    /**
     * Performs the transformation on the given {@code body}.
     *
     * @param body Nonnull.
     */
    void preprocess(Element body);
}
