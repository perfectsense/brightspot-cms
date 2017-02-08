package com.psddev.cms.db;

import java.io.IOException;
import java.io.StringWriter;
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
import com.psddev.dari.util.LazyWriter;
import com.psddev.dari.util.LazyWriterResponse;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.ThreadLocalStack;

/**
 * Internal filter that adds {@code <span data-field>} to the response
 * whenever an object field is accessed.
 */
public class FieldAccessFilter extends AbstractFilter {

    private static final String ATTRIBUTE_PREFIX = FieldAccessFilter.class.getName() + ".";
    private static final String DISPLAY_IDS_ATTRIBUTE = ATTRIBUTE_PREFIX + "displayIds";
    static final String CURRENT_RESPONSE_ATTRIBUTE = ATTRIBUTE_PREFIX + "currentResponse";

    static final ThreadLocalStack<LazyWriter> THREAD_DEFAULT_LAZY_WRITER = new ThreadLocalStack<>();

    /**
     * Returns IDs of of all objects whose field accesses should be
     * displayed.
     *
     * @param request Nonnull.
     * @return Nonnull.
     */
    public static Set<UUID> getDisplayIds(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Set<UUID> displayIds = (Set<UUID>) request.getAttribute(DISPLAY_IDS_ATTRIBUTE);

        if (displayIds == null) {
            displayIds = new HashSet<>();

            request.setAttribute(DISPLAY_IDS_ATTRIBUTE, displayIds);
        }

        return displayIds;
    }

    /**
     * Creates the marker HTML that identifies access to a field with the
     * given {@code name} in the given {@code state}.
     *
     * @param state Nonnull.
     * @param name Nonnull.
     * @return Nonnull.
     */
    public static String createMarkerHtml(State state, String name) {
        Map<String, String> markerData = new CompactMap<>();
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

        markerData.put("id", state.getId().toString());
        markerData.put("name", name);

        return PageFilter.createMarkerHtml("BrightspotCmsFieldAccess", markerData);
    }

    /**
     * Writes using the given {@code consumer}, inserting field access markers
     * as necessary, and returns the output.
     *
     * @param inBody {@code true} if the writes are within {@code <body>}.
     * @param consumer Nonnull.
     * @return Nonnull.
     */
    public static String write(boolean inBody, FieldAccessWriteConsumer consumer) {
        StringWriter stringWriter = new StringWriter();
        LazyWriter lazyWriter = new LazyWriter(stringWriter, inBody);

        THREAD_DEFAULT_LAZY_WRITER.with(lazyWriter, () -> {
            try {
                consumer.accept(lazyWriter);
                lazyWriter.writePending();

            } catch (IOException error) {
                throw new IllegalStateException(error);
            }
        });

        return stringWriter.toString();
    }

    @Override
    protected void doDispatch(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws Exception {

        if (ObjectUtils.to(boolean.class, request.getParameter("_fields"))
                || (PageFilter.Static.getMainObject(request) != null
                && PageFilter.Static.isInlineEditingAllContents(request))) {

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

        JspBufferFilter.Static.overrideBuffer(0);

        try {
            Object mainObject = PageFilter.Static.getMainObject(request);
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

    /**
     * @deprecated
     * @see FieldAccessFilter
     */
    public static class Static {

        /**
         * @deprecated
         * @see FieldAccessFilter#createMarkerHtml(State, String)
         */
        @Deprecated
        public static String createMarkerHtml(State state, String name) {
            return FieldAccessFilter.createMarkerHtml(state, name);
        }

        /**
         * @deprecated
         * @see FieldAccessFilter#getDisplayIds(HttpServletRequest)
         */
        @Deprecated
        public static Set<UUID> getDisplayIds(HttpServletRequest request) {
            return FieldAccessFilter.getDisplayIds(request);
        }
    }
}
