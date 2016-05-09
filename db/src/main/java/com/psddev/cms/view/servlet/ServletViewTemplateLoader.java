package com.psddev.cms.view.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.ServletContext;

import com.psddev.cms.view.UrlViewTemplateLoader;
import com.psddev.dari.util.CodeUtils;

/**
 * Loads templates in the servlet context.
 */
public class ServletViewTemplateLoader extends UrlViewTemplateLoader {

    private static final String TEMPLATE_NOT_FOUND_MESSAGE_FORMAT = "Could not find template at path [%s]!";

    private ServletContext servletContext;

    /**
     * @param servletContext the servlet HTTP servlet context .
     */
    public ServletViewTemplateLoader(ServletContext servletContext) {
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
}
