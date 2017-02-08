package com.psddev.cms.db;

import com.psddev.dari.db.State;
import com.psddev.dari.util.LazyWriterResponse;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

class FieldAccessListener extends State.Listener {

    private static final String ATTRIBUTE_PREFIX = FieldAccessListener.class.getName() + ".";
    private static final String PREVIOUS_MARKER_HTML_ATTRIBUTE = ATTRIBUTE_PREFIX + "previousMarkerHtml";

    private final HttpServletRequest request;

    public FieldAccessListener(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public void beforeFieldGet(State state, String name) {
        Object original = state.getOriginalObject();

        if (original == null || original.getClass().getName().startsWith("com.psddev.dari.")) {
            return;
        }

        try {
            com.psddev.dari.util.LazyWriter writer = FieldAccessFilter.THREAD_DEFAULT_LAZY_WRITER.get();

            if (writer == null) {
                com.psddev.dari.util.LazyWriterResponse response = (LazyWriterResponse) request.getAttribute(FieldAccessFilter.CURRENT_RESPONSE_ATTRIBUTE);

                if (response != null) {
                    writer = response.getLazyWriter();
                }
            }

            if (writer != null) {
                String markerHtml = FieldAccessFilter.createMarkerHtml(state, name);
                String previousMarkerHtml = (String) request.getAttribute(PREVIOUS_MARKER_HTML_ATTRIBUTE);

                if (!markerHtml.equals(previousMarkerHtml)) {
                    request.setAttribute(PREVIOUS_MARKER_HTML_ATTRIBUTE, markerHtml);
                    writer.writeLazily(markerHtml);
                }
            }

        } catch (IOException error) {
            // Can't write the field access marker HTML to the response,
            // but that's OK, so move on.
        }
    }
}
