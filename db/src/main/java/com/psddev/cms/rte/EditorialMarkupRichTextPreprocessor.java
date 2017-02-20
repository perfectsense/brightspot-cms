package com.psddev.cms.rte;

import org.jsoup.nodes.Element;

/**
 * {@link RichTextPreprocessor} implementation that removes editorial markup
 * meant for internal use.
 *
 * <p>For example, this preprocessor removes all markup produced by the track
 * changes feature, which are wrapped in {@code <del>} or {@code <ins>}.</p>
 */
public class EditorialMarkupRichTextPreprocessor implements RichTextPreprocessor {

    @Override
    public void preprocess(Element body) {
        body.getElementsByTag("del").remove();
        body.getElementsByTag("ins").unwrap();
        body.getElementsByClass("rte").remove();
        body.select("code[data-annotations]").remove();
    }
}
