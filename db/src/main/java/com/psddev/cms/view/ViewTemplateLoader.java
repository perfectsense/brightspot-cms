package com.psddev.cms.view;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads templates and metadata about templates based on a named path.
 */
public interface ViewTemplateLoader {

    /**
     * Returns the template located at the named path as an
     * {@link java.io.InputStream} object.
     *
     * @param path the path to the template.
     * @return the template as a stream. Never {@code null}.
     * @throws IOException if a problem occurred fetching the template at the given path.
     */
    InputStream getTemplate(String path) throws IOException;

    /**
     * Returns the last modified timestamp for the template located at the
     * named path.
     *
     * @param path the path to the template.
     * @return the last modified timestamp.
     * @throws IOException if a problem occurred reading the last modified timestamp.
     */
    long getLastModified(String path) throws IOException;
}
