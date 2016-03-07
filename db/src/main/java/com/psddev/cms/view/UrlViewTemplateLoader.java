package com.psddev.cms.view;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

import com.psddev.dari.util.IoUtils;

/**
 * Skeletal {@link ViewTemplateLoader} implementation that implements
 * {@link #getTemplate(String)} and {@link #getLastModified(String)} based on
 * the URL returned from {@link #getTemplateUrl(String)} implemented by a
 * sub-class.
 */
public abstract class UrlViewTemplateLoader implements ViewTemplateLoader {

    /**
     * Returns the template located at the named path as a URL.
     *
     * @param path the path to the template.
     * @return the template URL. Never {@code null}.
     * @throws IOException if a problem occurred fetching the URL for the given path.
     */
    protected abstract URL getTemplateUrl(String path) throws IOException;

    @Override
    public InputStream getTemplate(String path) throws IOException {
        return getTemplateUrl(path).openConnection().getInputStream();
    }

    @Override
    public long getLastModified(String path) throws IOException {

        URL templateUrl = getTemplateUrl(path);

        URLConnection urlConnection = null;
        try {
            urlConnection = templateUrl.openConnection();

            if (urlConnection instanceof JarURLConnection) {
                URL jarURL = ((JarURLConnection) urlConnection).getJarFileURL();
                if ("file".equals(jarURL.getProtocol())) {
                    urlConnection = null;
                    String file = jarURL.getFile();
                    return new File(file).lastModified();
                }
            }

            return urlConnection.getLastModified();

        } finally {
            if (urlConnection != null) {
                IoUtils.closeQuietly(urlConnection.getInputStream());
            }
        }
    }
}
