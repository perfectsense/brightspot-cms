package com.psddev.cms.rte;

import java.util.function.Function;

import com.psddev.cms.db.RichTextElement;

class ElementRichTextViewNode<V> implements RichTextViewNode<V> {

    private final RichTextElement element;
    private final Function<RichTextElement, V> elementToView;

    public ElementRichTextViewNode(RichTextElement element, Function<RichTextElement, V> elementToView) {
        this.element = element;
        this.elementToView = elementToView;
    }

    @Override
    public V toView() {
        return elementToView != null ? elementToView.apply(element) : null;
    }
}
