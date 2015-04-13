package com.psddev.cms.db;

import javax.servlet.http.HttpServletRequest;

import com.psddev.dari.util.CacheableResponse;
import com.psddev.dari.util.ResponseCacheFilter;

/** If the mainObject implements CacheableResponse, run it. */
public final class MainObjectGlobalCacheableResponse implements CacheableResponse.Global {
    @Override
    public void normalizeRequest(NormalizingRequest request) {
        Object mainObject = PageFilter.Static.getMainObject(request);
        if (mainObject instanceof CacheableResponse && !(mainObject instanceof Global)) {
            if (((CacheableResponse) mainObject).shouldCacheResponse(request)) {
                request.setAttribute(ResponseCacheFilter.TIMEOUT_SECONDS_REQUEST_ATTRIBUTE_NAME, 0);
            }
            ((CacheableResponse) mainObject).normalizeRequest(request);
        }
    }

    @Override
    public boolean shouldCacheResponse(HttpServletRequest request) {
        Object mainObject = PageFilter.Static.getMainObject(request);
        if (mainObject instanceof CacheableResponse && !(mainObject instanceof Global)) {
            return ((CacheableResponse) mainObject).shouldCacheResponse(request);
        }
        return true;
    }
}
