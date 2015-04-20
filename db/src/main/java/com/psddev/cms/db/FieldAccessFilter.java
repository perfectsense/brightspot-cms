package com.psddev.cms.db;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.State;
import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.JspBufferFilter;
import com.psddev.dari.util.LazyWriterResponse;
import com.psddev.dari.util.ObjectUtils;

/**
 * Internal filter that adds {@code <span data-field>} to the response
 * whenever an object field is accessed.
 */
public class FieldAccessFilter extends AbstractFilter {

    private static final String ATTRIBUTE_PREFIX = FieldAccessFilter.class.getName() + ".";
    private static final String CURRENT_RESPONSE_ATTRIBUTE = ATTRIBUTE_PREFIX + "currentResponse";

    @Override
    protected void doDispatch(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws Exception {

        if (ObjectUtils.to(boolean.class, request.getParameter("_fields"))) {
            super.doDispatch(request, response, chain);

        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    protected void doInclude(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        Object current = request.getAttribute(CURRENT_RESPONSE_ATTRIBUTE);
        LazyWriterResponse lazyResponse = new LazyWriterResponse(request, response);

        try {
            request.setAttribute(CURRENT_RESPONSE_ATTRIBUTE, lazyResponse);
            chain.doFilter(request, lazyResponse);

        } finally {
            request.setAttribute(CURRENT_RESPONSE_ATTRIBUTE, current);
            lazyResponse.getLazyWriter().writePending();
        }
    }

    @Override
    protected void doRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        try {
            JspBufferFilter.Static.overrideBuffer(0);

            Object mainObject = PageFilter.Static.getMainObject(request);
            Set<UUID> displayIds = Static.getDisplayIds(request);

            if (mainObject != null) {
                State mainState = State.getInstance(mainObject);

                displayIds.add(mainState.getId());
                addDisplayIds(displayIds, mainState.getSimpleValues());
            }

            FieldAccessListener listener = new FieldAccessListener(request);

            try {
                State.Static.addListener(listener);
                doInclude(request, response, chain);

            } finally {
                State.Static.removeListener(listener);
            }

        } finally {
            JspBufferFilter.Static.restoreBuffer();
        }
    }

    private void addDisplayIds(Set<UUID> displayIds, Object value) {
        if (value instanceof Iterable) {
            for (Object item : (Collection<?>) value) {
                addDisplayIds(displayIds, item);
            }

        } else if (value instanceof Map) {
            Map<?, ?> valueMap = (Map<?, ?>) value;
            UUID id = ObjectUtils.to(UUID.class, valueMap.get("_id"));

            if (id != null) {
                displayIds.add(id);

            } else {
                for (Object item : valueMap.values()) {
                    addDisplayIds(displayIds, item);
                }
            }
        }
    }

    /**
     * Creates the marker HTML that identifies access to a field with the
     * given {@code name} in the given {@code state}.
     *
     * @param state Can't be {@code null}.
     * @param name Can't be {@code null}.
     * @return Never blank.
     *
     * @deprecated Use {@link Static#createMarkerHtml} instead.
     */
    @Deprecated
    public static String createMarkerHtml(State state, String name) {
        return Static.createMarkerHtml(state, name);
    }

    /**
     * {@link FieldAccessFilter} utility methods.
     */
    public static class Static {

        private static final String ATTRIBUTE_PREFIX = Static.class.getName() + ".";
        private static final String DISPLAY_IDS_ATTRIBUTE = ATTRIBUTE_PREFIX + "displayIds";

        /**
         * Creates the marker HTML that identifies access to a field with the
         * given {@code name} in the given {@code state}.
         *
         * @param state Can't be {@code null}.
         * @param name Can't be {@code null}.
         * @return Never blank.
         */
        public static String createMarkerHtml(State state, String name) {
            Map<String, Object> markerData = new CompactMap<>();
            ObjectType type = state.getType();

            if (type != null) {
                ObjectField field = type.getField(name);

                if (field != null) {
                    String fieldType = field.getInternalType();

                    if (ObjectField.TEXT_TYPE.equals(fieldType)) {
                        Object value = state.get(name);

                        if (value != null) {
                            markerData.put("text", value.toString());
                        }
                    }
                }
            }

            markerData.put("name", state.getId().toString() + "/" + name);

            return "<!--brightspot.field-access " + ObjectUtils.toJson(markerData) + "-->";
        }

        /**
         * Returns IDs of of all objects whose field accesses should be
         * displayed.
         *
         * @param request Can't be {@code null}.
         * @return Never {@code null}.
         */
        public static Set<UUID> getDisplayIds(HttpServletRequest request) {
            @SuppressWarnings("unchecked")
            Set<UUID> displayIds = (Set<UUID>) request.getAttribute(DISPLAY_IDS_ATTRIBUTE);

            if (displayIds == null) {
                displayIds = new HashSet<UUID>();

                request.setAttribute(DISPLAY_IDS_ATTRIBUTE, displayIds);
            }

            return displayIds;
        }
    }

    private static class FieldAccessListener extends State.Listener {

        private final HttpServletRequest request;

        public FieldAccessListener(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public void beforeFieldGet(State state, String name) {
            if (!Static.getDisplayIds(request).contains(state.getId())) {
                return;
            }

            LazyWriterResponse response = (LazyWriterResponse) request.getAttribute(CURRENT_RESPONSE_ATTRIBUTE);

            if (response != null) {
                try {
                    response.getLazyWriter().writeLazily(createMarkerHtml(state, name));
                } catch (IOException error) {
                    // Can't write the field access marker HTML to the response,
                    // but that's OK, so move on.
                }
            }
        }
    }
}
