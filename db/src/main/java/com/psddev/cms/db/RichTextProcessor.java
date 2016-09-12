package com.psddev.cms.db;

import org.jsoup.nodes.Document;

/**
 * Processes a Jsoup document produced by the rich text editor.
 */
public interface RichTextProcessor {

    /**
     * Performs a transformation on a rich text Jsoup Document.
     *
     * @param richText the rich text document to process.
     */
    void process(Document richText);
}
