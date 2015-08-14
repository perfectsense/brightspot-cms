package com.psddev.cms.db;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.TryCatchFinally;

import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

/**
 * Equivalent to the HTML {@code A} tag where its {@code href} attribute
 * may be set to a URL or a Dari object. Note that the context path will
 * automatically be prepended to the final URL.
 */
public class AnchorTag extends BodyTagSupport implements DynamicAttributes, TryCatchFinally {

    private static final long serialVersionUID = 1L;

    private Object href;
    private final Map<String, String> attributes = new LinkedHashMap<String, String>();

    public Object getHref() {
        return href;
    }

    public void setHref(Object href) {
        this.href = href;
    }

    // --- TagSupport support ---

    @Override
    public int doStartTag() throws JspException {
        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag() throws JspException {
        Object href = getHref();
        String hrefString = null;

        if (href == null) {
            hrefString = null;

        } else if (href instanceof String
                || href instanceof URI
                || href instanceof URL) {
            hrefString = href.toString();

        } else if (href instanceof Content) {
            hrefString = ((Content) href).getPermalink();

        } else {
            hrefString = State.getInstance(href).as(Directory.ObjectModification.class).getPermalink();
        }

        if (!ObjectUtils.isBlank(hrefString)) {
            hrefString = JspUtils.getAbsolutePath((HttpServletRequest) pageContext.getRequest(), hrefString);
            hrefString = StringUtils.escapeHtml(hrefString);
        } else {
            hrefString = "";
        }

        try {
            JspWriter writer = pageContext.getOut();

            if (!hrefString.isEmpty()) {
                writer.print("<a href=\"");
                writer.print(hrefString);
                writer.print("\"");

                for (Map.Entry<String, String> e : attributes.entrySet()) {
                    String key = e.getKey();
                    String value = e.getValue();
                    if (!(ObjectUtils.isBlank(key) || ObjectUtils.isBlank(value))) {
                        writer.print(" ");
                        writer.print(StringUtils.escapeHtml(key));
                        writer.print("=\"");
                        writer.print(StringUtils.escapeHtml(value));
                        writer.print("\"");
                    }
                }

                writer.print(">");
            }

            String body = null;
            if (bodyContent != null) {
                body = bodyContent.getString();
            }

            if (ObjectUtils.isBlank(body)) {
                if (href instanceof Recordable) {
                    body = ((Recordable) href).getState().getLabel();
                    if (ObjectUtils.isBlank(body)) {
                        body = "";
                    } else {
                        body = StringUtils.escapeHtml(body);
                    }
                } else {
                    body = "";
                }
            }

            writer.print(body);

            if (!hrefString.isEmpty()) {
                writer.print("</a>");
            }

        } catch (IOException ex) {
            throw new JspException(ex);
        }

        return EVAL_PAGE;
    }

    // --- DynamicAttribute support ---

    @Override
    public void setDynamicAttribute(String uri, String localName, Object value) {
        attributes.put(localName, value != null ? value.toString() : null);
    }

    // --- TryCatchFinally support ---

    @Override
    public void doCatch(Throwable error) throws Throwable {
        throw error;
    }

    @Override
    public void doFinally() {
        setHref(null);
        attributes.clear();
    }
}
