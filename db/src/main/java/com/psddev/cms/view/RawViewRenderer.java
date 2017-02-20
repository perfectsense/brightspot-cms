package com.psddev.cms.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A renderer for {@link RawView} that concatenates the calls to {@code toString()}
 * for each of its items. If an item is itself a renderable view, then normal
 * view delegation logic will apply. Items that resolve to null will be
 * suppressed from the final output.
 */
public class RawViewRenderer implements ViewRenderer {

    private static final String ITEMS_VIEW_KEY = "items";

    @Override
    public ViewOutput render(Object view, ViewTemplateLoader templateLoader) {
        ViewMap viewMap = null;

        if (view instanceof ViewMap) {
            viewMap = (ViewMap) view;

        } else if (view != null) {
            viewMap = new ViewMap(view);
        }

        List<ViewOutput> outputs = new ArrayList<>();

        if (viewMap != null) {

            Object itemsObject = viewMap.get(ITEMS_VIEW_KEY);
            if (itemsObject instanceof Collection) {

                for (Object item : (Collection<?>) itemsObject) {

                    if (item != null) {

                        ViewRenderer itemViewRenderer = ViewRenderer.createRenderer(item);
                        if (itemViewRenderer != null) {
                            outputs.add(itemViewRenderer.render(item, templateLoader));

                        } else {
                            outputs.add(item::toString);
                        }
                    }
                }
            }
        }

        return () -> outputs.stream()
                .filter(Objects::nonNull)
                .map(ViewOutput::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }
}
