package com.psddev.cms.tool;

import java.util.Map;

import com.psddev.cms.db.ToolUser;

/**
 * @deprecated Use {@link com.psddev.cms.rtc.RtcAction} instead.
 */
@Deprecated
public abstract class ToolCheck {

    /**
     * @return Never {@code null}.
     */
    public abstract String getName();

    /**
     * @param user May be {@code null}.
     * @param url May be {@code null}.
     * @param parameters May be {@code null}.
     * @return May be {@code null}.
     */
    public final ToolCheckResponse check(ToolUser user, String url, Map<String, Object> parameters) throws Exception {
        return doCheck(user, url, parameters);
    }

    /**
     * @param user May be {@code null}.
     * @param url May be {@code null}.
     * @param parameters May be {@code null}.
     * @return May be {@code null}.
     */
    protected abstract ToolCheckResponse doCheck(ToolUser user, String url, Map<String, Object> parameters) throws Exception;
}
