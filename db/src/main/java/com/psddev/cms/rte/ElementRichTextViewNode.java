package com.psddev.cms.rte;

import java.util.function.Function;

import com.psddev.cms.db.RichTextElement;

class ElementRichTextViewNode<V> implements RichTextViewNode<V> {

    private final V outputView;

    public ElementRichTextViewNode(V outputView) {
        this.outputView = outputView;
    }

    public ElementRichTextViewNode(RichTextElement element, Function<RichTextElement, V> elementToView) {
        this(elementToView != null ? elementToView.apply(element) : null);
    }

    @Override
    public V toView() {
        return outputView;
    }
}
