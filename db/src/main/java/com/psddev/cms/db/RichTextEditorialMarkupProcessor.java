package com.psddev.cms.db;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Removes rich text editor specific markup that is generally just for internal
 * use and not meant for consumption by external sources. For example, markup
 * produced by the Track Changes feature would be removed.
 */
public class RichTextEditorialMarkupProcessor implements RichTextProcessor {

    @Override
    public void process(Document document) {
        Element body = document.body();

        body.getElementsByTag("del").remove();
        body.getElementsByTag("ins").unwrap();
        body.getElementsByClass("rte").remove();
        body.select("code[data-annotations]").remove();
    }
}
