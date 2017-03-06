package com.psddev.cms.rte;

import java.util.function.Function;

import com.psddev.cms.view.RawView;

class StringRichTextViewNode<V> implements RichTextViewNode<V> {

    private final String html;
    private final Function<String, V> htmlToView;

    public StringRichTextViewNode(String html, Function<String, V> htmlToView) {
        this.html = html;
        this.htmlToView = htmlToView;
    }

    public String getHtml() {
        return html;
    }

    @Override
    public V toView() {
        if (htmlToView != null) {
            return htmlToView.apply(html);
        }

        // Deliberately cast to an incompatible type if the htmlToView
        // function is null so that the final output can work automatically
        // in most cases.
        @SuppressWarnings("unchecked")
        V rawView = (V) RawView.of(html);
        return rawView;
    }
}
