package com.psddev.cms.db;

import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.State;
import com.psddev.dari.util.RequestNormalizer;
import com.psddev.dari.util.ResponseCacheFilter;

/** If the mainObject implements RequestNormalizer, run it. */
public final class ResponseCacheMainObjectRequestNormalizer implements RequestNormalizer.Global {
    @Override
    public void normalizeRequest(NormalizingRequest request) {
        Object mainObject = PageFilter.Static.getMainObject(request);
        if (mainObject != null) {
            State mainState = State.getInstance(mainObject);
            if (mainState != null) {
                ObjectType type = mainState.getType();
                Integer timeout = type.as(ResponseCacheTypeModification.class).getTimeout();
                if (timeout != null) {
                    request.setAttribute(ResponseCacheFilter.TIMEOUT_SECONDS_REQUEST_ATTRIBUTE_NAME, timeout);
                }
            }
        }
        if (mainObject instanceof RequestNormalizer && !(mainObject instanceof Global)) {
            ((RequestNormalizer) mainObject).normalizeRequest(request);
        }
    }
}
