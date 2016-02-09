package com.psddev.cms.view;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

/**
 * An abstraction of an HTTP response that contains information on how a real
 * HTTP response should be constructed.
 */
public class ViewResponse extends RuntimeException {

    private Integer status;

    private Map<String, List<String>> headers = new LinkedHashMap<>();

    private List<Cookie> cookies = new ArrayList<>();

    private List<Cookie> signedCookies = new ArrayList<>();

    private String redirectUri;

    private Boolean isRedirectPermanent;

    /**
     * Gets the status for the response.
     *
     * @return the status to be sent with the response.
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * Sets the status for the response.
     *
     * @param status the status to set.
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Gets the headers to be sent with the response.
     *
     * @return the headers to be sent with the response.
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Adds a header to be sent with the response. This will append to any
     * previously added headers by the same {@code name}.
     *
     * @param name the the header name.
     * @param value the header value.
     * @see #setHeader(String, String)
     */
    public void addHeader(String name, String value) {
        if (name != null && value != null) {
            List<String> values = headers.get(name);
            if (values == null) {
                values = new ArrayList<>();
                headers.put(name, values);
            }
            values.add(value);
        }
    }

    /**
     * Sets a header to be sent with the response. Any previously added headers
     * by the same name will be removed. If the {@code value} is null, then the
     * header will be removed.
     *
     * @param name the header name.
     * @param value the header value.
     * @see #addHeader(String, String)
     */
    public void setHeader(String name, String value) {
        if (name != null) {
            List<String> values = headers.get(name);

            if (values == null) {
                values = new ArrayList<>();
                headers.put(name, values);
            } else {
                values.clear();
            }

            if (value != null) {
                values.add(value);
            } else {
                headers.remove(name);
            }
        }
    }

    /**
     * Gets the list of cookies to be sent with the response.
     *
     * @return the list of cookies to be sent with response.
     */
    public List<Cookie> getCookies() {
        return cookies;
    }

    /**
     * Adds a cookie to be sent with the response.
     *
     * @param cookie the cookie to add.
     */
    public void addCookie(Cookie cookie) {
        if (cookie != null) {
            cookies.add(cookie);
        }
    }

    /**
     * Gets the list of cookies in need of signing to be sent with the response.
     *
     * @return the list of cookies in need of signing to be sent with the
     * response.
     */
    public List<Cookie> getSignedCookies() {
        return signedCookies;
    }

    /**
     * Adds a cookie in need of signing to the response.
     *
     * @param cookie the cookie to add that needs signing.
     */
    public void addSignedCookie(Cookie cookie) {
        if (cookie != null) {
            signedCookies.add(cookie);
        }
    }

    /**
     * Gets the redirect URI intended for the response.
     *
     * @return the redirect URI.
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Signals that the response should contain a permanent redirect to the
     * specified {@code uri}. Calls to this method will usually be followed
     * by {@code throw response} to halt execution and signal that writing to
     * the response is complete.
     *
     * @param uri the uri to redirect to.
     */
    public void redirectPermanently(String uri) {
        redirectUri = uri;
        isRedirectPermanent = true;
    }

    /**
     * Signals that the response should contain a temporary redirect to the
     * specified {@code uri}. Calls to this method will usually be followed
     * by {@code throw response} to halt execution and signal that writing to
     * the response is complete.
     *
     * @param uri the uri to redirect to.
     */
    public void redirectTemporarily(String uri) {
        redirectUri = uri;
        isRedirectPermanent = false;
    }

    /**
     * @return true if a permanent redirect has been issued, false otherwise.
     */
    public boolean isRedirectPermanent() {
        return Boolean.TRUE.equals(isRedirectPermanent);
    }

    /**
     * @return true if a permanent redirect has been issued, false otherwise.
     */
    public boolean isRedirectTemporary() {
        return Boolean.FALSE.equals(isRedirectPermanent);
    }

    @Override
    public String getMessage() {
        return "Request is finished.";
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    /**
     * @deprecated Use {@link #redirectTemporarily(String)} instead.
     */
    @Deprecated
    public void redirect(String uri) {
        redirectTemporarily(uri);
    }

    /**
     * Finds the first ViewResponse in an exception stack by traversing the
     * causes of the {@code throwable} argument.
     *
     * @param throwable the exception stack to search through.
     * @return the first ViewResponse found in the exception stack.
     */
    public static ViewResponse findInExceptionChain(Throwable throwable) {
        if (throwable instanceof ViewResponse) {
            return (ViewResponse) throwable;
        } else if (throwable != null) {
            return findInExceptionChain(throwable.getCause());
        } else {
            return null;
        }
    }
}
