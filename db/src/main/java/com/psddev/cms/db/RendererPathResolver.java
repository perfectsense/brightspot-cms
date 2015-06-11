package com.psddev.cms.db;

import javax.servlet.http.HttpServletRequest;

/**
 * Resolves the path to be used to render an object for a given HTTP
 * request.
 */
public interface RendererPathResolver {

    /**
     * Gets the renderer path for this object in context of the {@code request}.
     *
     * @param request the current HTTP request.
     * @return the path to the script that should be used to render this object.
     */
    String getRendererPath(HttpServletRequest request);
}
