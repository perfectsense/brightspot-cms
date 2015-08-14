package com.psddev.cms.db;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.psddev.dari.db.Reference;
import com.psddev.dari.db.ReferentialText;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CodeUtils;
import com.psddev.dari.util.HtmlGrid;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PageContextFilter;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.UrlBuilder;

public final class ElFunctionUtils {

    private ElFunctionUtils() {
    }

    /**
     * Escapes the given {@code string} so that it's safe to use in HTML.
     *
     * @param string If {@code null}, returns an empty string.
     * @return Never {@code null}.
     */
    public static String html(String string) {
        return string != null ? StringUtils.escapeHtml(string) : "";
    }

    /**
     * Returns {@code true} if the current request is in the given
     * {@code context}.
     *
     * @param context If {@code null}, returns {@code false}.
     * @see ContextTag.Static#isInContext
     */
    public static boolean inContext(String context) {
        return ContextTag.Static.isInContext(PageContextFilter.Static.getRequest(), context);
    }

    /**
     * Returns {@code true} if the given {@code object} is an instance of the
     * class represented by the given {@code className}.
     *
     * @throws IllegalArgumentException If there isn't a class with the given
     * {@code className}.
     */
    public static boolean instanceOf(Object object, String className) {
        Class<?> c = ObjectUtils.getClassByName(className);

        if (c != null) {
            return c.isInstance(object);

        } else {
            throw new IllegalArgumentException(String.format(
                    "[%s] is not a valid class name!", className));
        }
    }

    /**
     * Escapes the given {@code string} so that it's safe to use in
     * JavaScript.
     *
     * @param string If {@code null}, returns an empty string.
     * @return Never {@code null}.
     */
    public static String js(String string) {
        return string != null ? StringUtils.escapeJavaScript(string) : "";
    }

    public static List<String> listLayouts(Object object, String field) {
        return object != null ? State.getInstance(object).as(Renderer.Data.class).getListLayouts().get(field) : null;
    }

    /**
     * Returns the number of markers with the given {@code markerInternalName}
     * in the given {@code text}.
     *
     * @param text If {@code null}, returns {@code 0}.
     * @param markerInternalName If blank, returns {@code 0}.
     */
    public static int markerCount(ReferentialText text, String markerInternalName) {
        int count = 0;

        if (text != null && !ObjectUtils.isBlank(markerInternalName)) {
            for (Object item : text) {
                if (item instanceof Reference) {
                    Object referenced = ((Reference) item).getObject();

                    if (referenced instanceof ReferentialTextMarker
                            && (((ReferentialTextMarker) referenced).getInternalName().equals(markerInternalName))) {
                        ++ count;
                    }
                }
            }
        }

        return count;
    }

    /**
     * Returns the plain {@linkplain StorageItem CDN} URL associated
     * with the given {@code servletPath}.
     */
    public static String plainResource(String servletPath) {
        ServletContext servletContext = null;
        HttpServletRequest request = null;

        try {
            servletContext = PageContextFilter.Static.getServletContext();
            request = PageContextFilter.Static.getRequest();
        } catch (IllegalStateException ex) {
            // Checked for nulls right below.
        }

        if (servletContext != null && request != null) {
            HtmlGrid.Static.addStyleSheet(request, servletPath);

            if (ObjectUtils.coalesce(
                    Settings.get(Boolean.class, "cms/isResourceInStorage"),
                    Settings.isProduction())) {
                StorageItem item = StorageItem.Static.getPlainResource(null, servletContext, servletPath);

                if (item != null) {
                    return JspUtils.isSecure(request)
                            ? item.getSecurePublicUrl()
                            : item.getPublicUrl();
                }
            }
        }

        return pathWithTimestamp(servletContext, servletPath);
    }

    private static String pathWithTimestamp(ServletContext context, String path) {
        if (context != null) {
            try {
                URL resource = CodeUtils.getResource(context, path);

                if (resource != null) {
                    URLConnection resourceConnection = resource.openConnection();
                    InputStream resourceInput = resourceConnection.getInputStream();

                    try {
                        return StringUtils.addQueryParameters(
                                path,
                                "_", resourceConnection.getLastModified());

                    } finally {
                        resourceInput.close();
                    }
                }

            } catch (IOException error) {
                // Ignore any errors and just return the path as is.
            }
        }

        return path;
    }

    /**
     * Returns the plain or gzipped {@linkplain StorageItem CDN} URL
     * associated with the given {@code servletPath}.
     */
    public static String resource(String servletPath) {
        ServletContext servletContext = null;
        HttpServletRequest request = null;

        try {
            servletContext = PageContextFilter.Static.getServletContext();
            request = PageContextFilter.Static.getRequest();
        } catch (IllegalStateException ex) {
            // Checked for nulls right below.
        }

        if (servletContext != null && request != null) {
            HtmlGrid.Static.addStyleSheet(request, servletPath);

            if (ObjectUtils.coalesce(
                    Settings.get(Boolean.class, "cms/isResourceInStorage"),
                    Settings.isProduction())) {

                String encodings = request.getHeader("Accept-Encoding");
                StorageItem item = ObjectUtils.isBlank(encodings) || !encodings.contains("gzip")
                        ? StorageItem.Static.getPlainResource(null, servletContext, servletPath)
                        : StorageItem.Static.getGzippedResource(null, servletContext, servletPath);

                if (item != null) {
                    return JspUtils.isSecure(request)
                            ? item.getSecurePublicUrl()
                            : item.getPublicUrl();
                }
            }
        }

        return pathWithTimestamp(servletContext, servletPath);
    }

    public static UrlBuilder absolutePath(String path) {
        return new UrlBuilder(PageContextFilter.Static.getRequest()).absolutePath(path);
    }

    public static UrlBuilder currentPath() {
        return new UrlBuilder(PageContextFilter.Static.getRequest()).currentPath();
    }

    public static UrlBuilder path(String path) {
        return new UrlBuilder(PageContextFilter.Static.getRequest()).path(path);
    }
}
