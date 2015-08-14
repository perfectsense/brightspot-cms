package com.psddev.cms.db;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TryCatchFinally;

import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Reference;
import com.psddev.dari.db.ReferentialText;
import com.psddev.dari.db.State;
import com.psddev.dari.util.HtmlGrid;
import com.psddev.dari.util.HtmlNode;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.LazyWriter;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

/**
 * Renders the given {@code value} safely in HTML context.
 *
 * <p>If the value is blank, the expression inside the tag is evaluated.
 * For example, given the following script where <code>${foo}</code> is
 * {@code null}:</p>
 *
 * <blockquote><pre><code data-type="java">{@literal
 *<cms:render value="${foo}">
 *    This is the fallback text.
 *</cms:render>
 * }</code></pre></blockquote>
 *
 * <p>The output would be {@code This is the fallback text.}</p>
 *
 * <p>If the value is an instance of {@link Iterable}, each item in it is
 * rendered in order.</p>
 *
 * <p>If the value is an instance of {@link ReferentialText}, the text is
 * written to the output as-is, and the objects in the references are rendered
 * according to the rules here.</p>
 *
 * <p>If the value is an instance of {@link String}, unsafe characters are
 * escaped, and the result is written to the output.</p>
 *
 * <p>Otherwise, the value is rendered using {@link PageFilter#renderObject}.
 * </p>
 */
public class RenderTag extends BodyTagSupport implements DynamicAttributes, TryCatchFinally {

    private static final long serialVersionUID = 1L;

    private static final Pattern EMPTY_PARAGRAPH_PATTERN = Pattern.compile("(?is)\\s*<p[^>]*>\\s*&nbsp;\\s*</p>\\s*");
    private static final String FIELD_ACCESS_MARKER_BEGIN = "\ue014\ue027\ue041";
    private static final String FIELD_ACCESS_MARKER_END = "\ue068\ue077\ue063";
    private static final String REFERENCE_ATTRIBUTE = "reference";

    private String area;
    private String context;
    private Object value;
    private String beginMarker;
    private int beginOffset;
    private String endMarker;
    private int endOffset;
    private final Map<String, String> attributes = new LinkedHashMap<String, String>();

    private transient HtmlWriter pageWriter;
    private transient LayoutTag layoutTag;
    private transient Map<String, Object> areas;
    private transient FieldAccessListener fieldAccessListener;

    public void setArea(String area) {
        this.area = area;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setBeginMarker(String beginMarker) {
        this.beginMarker = beginMarker;
    }

    public void setBeginOffset(int beginOffset) {
        this.beginOffset = beginOffset;
    }

    public void setEndMarker(String endMarker) {
        this.endMarker = endMarker;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public void setMarker(String marker) {
        setBeginMarker(marker);
        setEndMarker(marker);
    }

    public void setOffset(int offset) {
        setBeginOffset(offset - 1);
        setEndOffset(offset);
    }

    // --- DynamicAttributes support ---

    @Override
    public void setDynamicAttribute(String uri, String localName, Object value) {
        if (value != null) {
            attributes.put(localName, value.toString());
        }
    }

    // --- TagSupport support ---

    @Override
    @SuppressWarnings("deprecation")
    public int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        if (!ObjectUtils.isBlank(context)) {
            ContextTag.Static.pushContext(request, context);
        }

        try {
            pageWriter = new HtmlWriter(pageContext.getOut());
            layoutTag = null;
            areas = null;

            for (Tag parent = getParent(); parent != null; parent = parent.getParent()) {
                if (parent instanceof RenderTag) {
                    break;

                } else if (parent instanceof LayoutTag) {
                    layoutTag = ((LayoutTag) parent);
                    areas = layoutTag.getAreas();
                    break;
                }
            }

            if (ObjectUtils.isBlank(value)) {
                if (areas != null) {
                    return EVAL_BODY_BUFFERED;

                } else {
                    if (!attributes.isEmpty()) {
                        pageWriter.writeStart("div", attributes);
                    }

                    setBodyContent(null);
                    return EVAL_BODY_INCLUDE;
                }

            } else {
                if (value instanceof Map) {
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                        writeArea(request, entry.getKey(), entry.getValue());
                    }

                } else if (value instanceof Iterable
                        && !(value instanceof ReferentialText)) {
                    int index = 0;
                    for (Object item : (Iterable<?>) value) {
                        writeArea(request, index, item);
                        ++ index;
                    }

                } else if (value instanceof Page.Area) {
                    Page.Area pageArea = (Page.Area) value;
                    writeArea(request, pageArea.getInternalName(), pageArea.getContents());

                } else if (value instanceof Section) {
                    Section section = (Section) value;
                    writeArea(request, section.getInternalName(), section);

                } else {
                    writeArea(request, area, value);
                }

                setBodyContent(null);
                return SKIP_BODY;
            }

        } catch (IOException error) {
            throw new JspException(error);

        } catch (ServletException error) {
            throw new JspException(error);
        }
    }

    @Override
    public void doInitBody() {
        if (ObjectUtils.to(boolean.class, pageContext.getRequest().getParameter("_fields"))) {
            fieldAccessListener = new FieldAccessListener();

            State.Static.addListener(fieldAccessListener);
        }
    }

    @Override
    public int doAfterBody() {
        if (fieldAccessListener != null) {
            State.Static.removeListener(fieldAccessListener);

            fieldAccessListener = null;
            BodyContent bodyContent = getBodyContent();
            String oldBody = bodyContent.getString();
            StringWriter newBody = new StringWriter();
            LazyWriter newBodyLazy = new LazyWriter((HttpServletRequest) pageContext.getRequest(), newBody);
            int beginAt;
            int endAt = 0;

            try {
                while ((beginAt = oldBody.indexOf(FIELD_ACCESS_MARKER_BEGIN, endAt)) > -1) {
                    newBodyLazy.write(oldBody.substring(endAt, beginAt));

                    endAt = oldBody.indexOf(FIELD_ACCESS_MARKER_END, beginAt);

                    if (endAt > -1) {
                        newBodyLazy.writeLazily(oldBody.substring(beginAt + FIELD_ACCESS_MARKER_BEGIN.length(), endAt));

                        endAt += FIELD_ACCESS_MARKER_END.length();

                    } else {
                        newBodyLazy.write(oldBody.substring(beginAt, beginAt + FIELD_ACCESS_MARKER_BEGIN.length()));

                        endAt = beginAt + FIELD_ACCESS_MARKER_BEGIN.length();
                    }
                }

                newBodyLazy.write(oldBody.substring(endAt));
                newBodyLazy.writePending();
                bodyContent.clearBody();
                bodyContent.write(newBody.toString());

            } catch (IOException error) {
                // Should never happen when writing to StringWriter.
            }
        }

        return SKIP_BODY;
    }

    private void writeArea(HttpServletRequest request, Object area, Object value) throws IOException, ServletException {
        if (layoutTag != null && areas != null) {
            if (!ObjectUtils.isBlank(area)) {
                HtmlGrid oldGrid = (HtmlGrid) request.getAttribute("grid");
                Object oldGridArea = request.getAttribute("gridArea");
                StringWriter body = new StringWriter();
                boolean contextSet = false;

                try {
                    HtmlGrid grid = layoutTag.getGrid(request, area);
                    Object gridArea = layoutTag.getAreaName(request, area);

                    if (grid != null) {
                        String context = grid.getContexts().get(String.valueOf(gridArea));

                        if (!ObjectUtils.isBlank(context)) {
                            contextSet = true;
                            ContextTag.Static.pushContext(request, context);
                        }
                    }

                    request.setAttribute("grid", grid);
                    request.setAttribute("gridArea", gridArea);
                    writeValueWithAttributes(new HtmlWriter(body), value);
                    areas.put(area.toString(), body.toString());

                } finally {
                    if (contextSet) {
                        ContextTag.Static.popContext(request);
                    }

                    request.setAttribute("grid", oldGrid);
                    request.setAttribute("gridArea", oldGridArea);
                }
            }

        } else {
            writeValueWithAttributes(pageWriter, value);
        }
    }

    private void writeValueWithAttributes(HtmlWriter writer, Object value) throws IOException, ServletException {
        if (attributes.isEmpty()) {
            writeValue(writer, value);

        } else {
            writer.writeStart("div", attributes);
                writeValue(writer, value);
            writer.writeEnd();
        }
    }

    @SuppressWarnings("deprecation")
    private void writeValue(HtmlWriter writer, Object value) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();

        if (value instanceof ReferentialText) {
            List<Object> items = ((ReferentialText) value).toPublishables(new RichTextCleaner());

            // Slice items based on markers.
            if (!(items.isEmpty() || (ObjectUtils.isBlank(beginMarker) && ObjectUtils.isBlank(endMarker)))) {
                int beginIndex = 0;
                int endIndex = items.size();

                if (!ObjectUtils.isBlank(beginMarker)) {
                    beginIndex = findMarker(items, beginMarker, beginOffset);
                }

                if (!ObjectUtils.isBlank(endMarker)) {
                    endIndex = findMarker(items, endMarker, endOffset);
                }

                if (beginIndex >= 0 && endIndex >= 0) {
                    if (beginIndex >= endIndex) {
                        items = items.subList(endIndex, beginIndex);

                    } else {
                        items = items.subList(beginIndex, endIndex);
                    }

                } else if (beginIndex < 0 && endIndex >= 0) {
                    items = items.subList(0, endIndex);

                } else if (endIndex < 0 && beginIndex >= 0) {
                    items = items.subList(beginIndex, items.size());

                } else if (beginOffset >= 0 || endOffset != 0) {
                    items.clear();
                }
            }

            Application.Static.getInstance(CmsTool.class).writeCss(request, writer);

            for (Object item : items) {
                if (item instanceof String) {
                    writer.write(EMPTY_PARAGRAPH_PATTERN.matcher((String) item).replaceAll(""));

                } else if (item instanceof Reference) {
                    Object oldReferenceAttribute = null;
                    Map<String, Object> oldAttributes = new LinkedHashMap<String, Object>();

                    try {
                        Reference itemReference = (Reference) item;
                        Object object = itemReference.getObject();

                        if (object != null && !(object instanceof ReferentialTextMarker)) {
                            oldReferenceAttribute = request.getAttribute(REFERENCE_ATTRIBUTE);
                            request.setAttribute(REFERENCE_ATTRIBUTE, itemReference);

                            // For backward compatibility, ensure these field values are set directly as request attributes
                            for (ObjectField field : ObjectType.getInstance(RichTextReference.class).getFields()) {
                                String fieldName = field.getInternalName();
                                String fieldNamePc = StringUtils.toCamelCase(fieldName);

                                oldAttributes.put(fieldName, request.getAttribute(fieldName));
                                oldAttributes.put(fieldNamePc, request.getAttribute(fieldNamePc));
                                request.setAttribute(fieldName, itemReference.getState().get(fieldName));
                                request.setAttribute(fieldNamePc, itemReference.getState().get(fieldName));
                            }

                            PageFilter.renderObject(request, response, writer, object);
                        }

                    } finally {
                        request.setAttribute(REFERENCE_ATTRIBUTE, oldReferenceAttribute);

                        for (Map.Entry<String, Object> entry : oldAttributes.entrySet()) {
                            request.setAttribute(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }

        } else if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                writeValue(writer, entry.getValue());
            }

        } else if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                writeValue(writer, item);
            }

        } else if (value instanceof Page.Area) {
            writeValue(writer, ((Page.Area) value).getContents());

        } else if (value instanceof String) {
            writer.html(value);

        } else if (value instanceof HtmlNode) {
            ((HtmlNode) value).writeHtml(writer);

        } else {
            PageFilter.renderObject(request, response, writer, value);
        }
    }

    private int findMarker(List<Object> items, String internalName, int offset) {
        int itemIndex = 0;
        int markerIndex = 0;

        for (Object item : items) {
            if (item instanceof Reference) {
                Object referenced = ((Reference) item).getObject();

                if (referenced instanceof ReferentialTextMarker
                        && internalName.equals(((ReferentialTextMarker) referenced).getInternalName())) {
                    if (offset == markerIndex) {
                        return itemIndex;
                    } else {
                        ++ markerIndex;
                    }
                }
            }

            ++ itemIndex;
        }

        return -1;
    }

    @Override
    public int doEndTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        if (!ObjectUtils.isBlank(context)) {
            ContextTag.Static.popContext(request);
        }

        try {
            if (ObjectUtils.isBlank(value)) {
                if (bodyContent != null) {
                    String body = bodyContent.getString();

                    if (body != null) {
                        if (areas != null) {
                            if (!ObjectUtils.isBlank(area)) {
                                if (!attributes.isEmpty()) {
                                    StringWriter stringWriter = new StringWriter();
                                    @SuppressWarnings("resource")
                                    HtmlWriter htmlWriter = new HtmlWriter(stringWriter);

                                    htmlWriter.writeStart("div", attributes);
                                        htmlWriter.write(body);
                                    htmlWriter.writeEnd();

                                    body = stringWriter.toString();
                                }

                                areas.put(area, body);
                            }

                        } else {
                            pageWriter.write(body);

                            if (!attributes.isEmpty()) {
                                pageWriter.writeEnd();
                            }
                        }
                    }
                }
            }

            return EVAL_PAGE;

        } catch (IOException error) {
            throw new JspException(error);
        }
    }

    // --- TryCatchFinally support ---

    @Override
    public void doCatch(Throwable error) throws Throwable {
        throw error;
    }

    @Override
    public void doFinally() {
        doAfterBody();

        setArea(null);
        setContext(null);
        setValue(null);
        setBeginMarker(null);
        setBeginOffset(0);
        setEndMarker(null);
        setEndOffset(0);
        attributes.clear();

        pageWriter = null;
        layoutTag = null;
        areas = null;
        fieldAccessListener = null;
    }

    private class FieldAccessListener extends State.Listener {

        @Override
        public void beforeFieldGet(State state, String name) {
            if (!FieldAccessFilter.Static.getDisplayIds((HttpServletRequest) pageContext.getRequest()).contains(state.getId())) {
                return;
            }

            BodyContent bodyContent = getBodyContent();

            try {
                bodyContent.write(FIELD_ACCESS_MARKER_BEGIN);
                bodyContent.write(FieldAccessFilter.createMarkerHtml(state, name));
                bodyContent.write(FIELD_ACCESS_MARKER_END);

            } catch (IOException error) {
                // Should never happen when writing to BodyContent.
            }
        }
    }
}
