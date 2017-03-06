package com.psddev.cms.image;

import com.psddev.cms.db.PageFilter;
import com.psddev.dari.util.AbstractFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Filter that automatically clears the {@linkplain ImageSize#clearContexts()
 * image size contexts} on every request.
 */
public class ImageSizeFilter extends AbstractFilter implements AbstractFilter.Auto {

    @Override
    public void updateDependencies(Class<? extends AbstractFilter> filterClass, List<Class<? extends Filter>> dependencies) {
        if (PageFilter.class.isAssignableFrom(filterClass)) {
            dependencies.add(getClass());
        }
    }

    @Override
    protected void doRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws Exception {
        ImageSize.clearContexts();

        try {
            chain.doFilter(request, response);

        } finally {
            ImageSize.clearContexts();
        }
    }
}
