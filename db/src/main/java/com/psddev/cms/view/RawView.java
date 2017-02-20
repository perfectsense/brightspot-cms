package com.psddev.cms.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A view that produces raw unprocessed output. It supports collections by
 * concatenating the output of each item within it. Unless the item is a view
 * with its own renderer (in which case the rendering logic will be delegated)
 * the output will be what is returned by its {@code toString()} method. Static
 * APIs are available to create an instance of {@code RawView} with either a
 * a collection of items or just a single item.
 */
@ViewInterface
@ViewRendererClass(RawViewRenderer.class)
public interface RawView {

    /**
     * Gets the list of items to be rendered in their raw form.
     *
     * @return the items to render.
     */
    List<?> getItems();

    /**
     * Creates a {@code RawView} for a collection of items.
     *
     * @param items the items to render.
     * @return a {@code RawView} for the collection of items.
     */
    static RawView of(Collection<?> items) {
        return () -> items != null ? new ArrayList<>(items) : Collections.emptyList();
    }

    /**
     * Creates a {@code RawView} for a single item.
     *
     * @param item the item to render.
     * @return a {code RawView} for a single item.
     */
    static RawView of(Object item) {
        return () -> item != null ? Collections.singletonList(item) : Collections.emptyList();
    }
}
