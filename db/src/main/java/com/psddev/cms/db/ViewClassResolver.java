package com.psddev.cms.db;

import javax.servlet.http.HttpServletRequest;

/**
 * Resolves the View class to be used to when rendering an object for a given
 * HTTP request.
 */
public interface ViewClassResolver {

    /**
     * Gets the view class for this object in context of the {@code request}.
     *
     * @param request the current HTTP request.
     * @return the View class that should be used when rendering this object.
     */
    Class<? extends View> getViewClass(HttpServletRequest request);
}
