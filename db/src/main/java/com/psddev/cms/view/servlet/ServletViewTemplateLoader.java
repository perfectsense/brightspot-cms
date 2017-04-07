package com.psddev.cms.view.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

import javax.servlet.ServletContext;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.psddev.cms.view.UrlViewTemplateLoader;
import com.psddev.dari.util.CodeUtils;

/**
 * Loads templates in the servlet context.
 */
public class ServletViewTemplateLoader extends UrlViewTemplateLoader {

    private static final String TEMPLATE_NOT_FOUND_MESSAGE_FORMAT = "Could not find template at path [%s]!";

    private static final LoadingCache<ServletContext, ServletViewTemplateLoader> INSTANCES = CacheBuilder
            .newBuilder()
            .weakKeys()
            .build(new CacheLoader<ServletContext, ServletViewTemplateLoader>() {

                @Override
                @SuppressWarnings("deprecation")
                public ServletViewTemplateLoader load(ServletContext servletContext) {
                    return new ServletViewTemplateLoader(servletContext);
                }
            });

    private ServletContext servletContext;

    /**
     * Returns an instance that loads templates from the given
     * {@code servletContext}.
     *
     * @param servletContext Nonnull.
     * @return Nonnull.
     */
    public static ServletViewTemplateLoader getInstance(ServletContext servletContext) {
        Preconditions.checkNotNull(servletContext);
        return INSTANCES.getUnchecked(servletContext);
    }

    /**
     * Creates an instance that loads templates from the given
     * {@code servletContext}.
     *
     * @param servletContext Nonnull.
     * @deprecated Use {@link #getInstance(ServletContext)} instead.
     */
    @Deprecated
    public ServletViewTemplateLoader(ServletContext servletContext) {
        Preconditions.checkNotNull(servletContext);
        this.servletContext = servletContext;
    }

    @Override
    public InputStream getTemplate(String path) throws IOException {
        InputStream template = CodeUtils.getResourceAsStream(servletContext, path);
        if (template == null) {
            throw new IOException(String.format(TEMPLATE_NOT_FOUND_MESSAGE_FORMAT, path));
        }
        return template;
    }

    @Override
    protected URL getTemplateUrl(String path) throws IOException {
        URL templateUrl = CodeUtils.getResource(servletContext, path);
        if (templateUrl == null) {
            throw new IOException(String.format(TEMPLATE_NOT_FOUND_MESSAGE_FORMAT, path));
        }
        return templateUrl;
    }

    @Override
    public int hashCode() {
        return servletContext.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof ServletViewTemplateLoader
                && Objects.equals(servletContext, ((ServletViewTemplateLoader) other).servletContext));
    }
}
