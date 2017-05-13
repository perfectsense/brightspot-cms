package com.psddev.cms.tool;

import com.psddev.cms.db.Site;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.AbstractFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter that sets cross-origin resource sharing (CORS) header for
 * instantiating classes if cross domain inline editing is enabled and origin
 * matches a site url.
 */
public abstract class CrossDomainFilter extends AbstractFilter {

    /**
     * Continue with {@link AbstractFilter#doRequest}.
     */
    protected abstract void doCrossDomainRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws Exception;

    /**
     * Sets additional CORS headers. Does nothing by default.
     */
    protected void setAdditionalCrossDomainHeaders(HttpServletResponse response) {
    }

    @Override
    protected void doRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws Exception {
        if (Application.Static.getInstance(CmsTool.class).isEnableCrossDomainInlineEditing()) {
            String origin = request.getHeader("origin");

            if (origin != null) {
                if (origin.endsWith("/")) {
                    origin = origin.substring(0, origin.length() - 1);
                }

                if (Query.from(Site.class).where("urls startsWith ?", origin).hasMoreThan(0)) {
                    response.setHeader("Access-Control-Allow-Origin", origin);
                    setAdditionalCrossDomainHeaders(response);
                }
            }
        }

        doCrossDomainRequest(request, response, chain);
    }
}
