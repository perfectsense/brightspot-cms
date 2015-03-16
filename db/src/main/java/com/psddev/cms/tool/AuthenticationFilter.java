package com.psddev.cms.tool;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.psddev.cms.db.Preview;
import com.psddev.cms.db.ToolUser;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ForwardingDatabase;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.DebugFilter;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PageContextFilter;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.UrlBuilder;

public class AuthenticationFilter extends AbstractFilter {

    /**
     * Settings key for tool user session timeout (in milliseconds).
     */
    public static final String TOOL_USER_SESSION_TIMEOUT_SETTING = "brightspot/toolUserSessionTimeout";

    private static final String ATTRIBUTE_PREFIX = AuthenticationFilter.class.getName() + ".";

    public static final String AUTHENTICATED_ATTRIBUTE = ATTRIBUTE_PREFIX + "authenticated";
    public static final String DATABASE_OVERRIDDEN_ATTRIBUTE = ATTRIBUTE_PREFIX + "databaseOverridden";

    /**
     * @deprecated Don't use this directly.
     */
    @Deprecated
    public static final String USER_ATTRIBUTE = ATTRIBUTE_PREFIX + "user";

    public static final String USER_TOKEN = ATTRIBUTE_PREFIX + "token";

    /**
     * @deprecated Don't use this directly.
     */
    @Deprecated
    public static final String USER_CHECKED_ATTRIBUTE = ATTRIBUTE_PREFIX + "userChecked";

    public static final String USER_SETTINGS_CHANGED_ATTRIBUTE = ATTRIBUTE_PREFIX + "userSettingsChanged";

    private static final String INSECURE_TOOL_USER_ATTRIBUTE = ATTRIBUTE_PREFIX + "insecureToolUser";
    private static final String INSECURE_TOOL_USER_CHECKED_ATTRIBUTE = ATTRIBUTE_PREFIX + "insecureToolUserChecked";
    private static final String PREVIEW_ATTRIBUTE = ATTRIBUTE_PREFIX + "preview";
    private static final String PREVIEW_CHECKED_ATTRIBUTE = ATTRIBUTE_PREFIX + "previewChecked";
    private static final String TOOL_USER_ATTRIBUTE = ATTRIBUTE_PREFIX + "toolUser";
    private static final String TOOL_USER_CHECKED_ATTRIBUTE = ATTRIBUTE_PREFIX + "toolUserChecked";

    public static final String LOG_IN_PATH = "/logIn.jsp";
    public static final String RETURN_PATH_PARAMETER = "returnPath";

    /**
     * @deprecated Don't use this directly.
     */
    @Deprecated
    public static final String USER_COOKIE = "cmsToolUser";

    private static final String INSECURE_TOOL_USER_COOKIE = "bsp.itu";
    private static final String PREVIEW_COOKIE = "bsp.p";
    private static final String TOOL_USER_COOKIE = "bsp.tu";

    // --- AbstractFilter support ---

    @Override
    protected void doRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws Exception {

        if (ObjectUtils.to(boolean.class, request.getParameter("_clearPreview"))) {
            Static.removeCurrentPreview(request, response);
            response.sendRedirect(new UrlBuilder(request).
                    currentPath().
                    currentParameters().
                    parameter("_clearPreview", null).
                    toString());
            return;
        }

        try {
            chain.doFilter(request, response);

        } catch (Exception error) {
            if (Static.isAuthenticated(request)) {
                DebugFilter.Static.writeError(request, response, error);

            } else {
                throw error;
            }

        } finally {
            if (Boolean.TRUE.equals(request.getAttribute(DATABASE_OVERRIDDEN_ATTRIBUTE))) {
                Database.Static.setIgnoreReadConnection(false);
                Database.Static.restoreDefault();
            }

            ToolUser user = Static.getUser(request);

            if (user != null && Boolean.TRUE.equals(request.getAttribute(USER_SETTINGS_CHANGED_ATTRIBUTE))) {
                user.save();
            }
        }
    }

    /**
     * {@link AuthenticationFilter} utility methods.
     */
    public static final class Static {

        private static void setSignedCookie(
                HttpServletRequest request,
                HttpServletResponse response,
                String name,
                String value,
                int maxAge,
                boolean secure) {

            Cookie c = new Cookie(name, name + value);

            c.setHttpOnly(true);
            c.setMaxAge(maxAge);
            c.setSecure(secure && JspUtils.isSecure(request));

            String siteUrl = Query.from(CmsTool.class).first().getDefaultSiteUrl();

            if (!ObjectUtils.isBlank(siteUrl)) {
                siteUrl = siteUrl.replaceFirst("^(?i)(?:https?://)?(?:www\\.)?", "");
                int slashAt = siteUrl.indexOf('/');

                if (slashAt > -1) {
                    siteUrl = siteUrl.substring(0, slashAt);
                }

                int colonAt = siteUrl.indexOf(':');

                if (colonAt > -1) {
                    siteUrl = siteUrl.substring(0, colonAt);
                }

                c.setDomain(siteUrl);
            }

            c.setPath("/");
            JspUtils.setSignedCookie(response, c);

            Cookie dc = new Cookie(name, name + value);

            dc.setHttpOnly(true);
            dc.setMaxAge(maxAge);
            dc.setSecure(secure && JspUtils.isSecure(request));
            dc.setPath("/");
            JspUtils.setSignedCookie(response, dc);
        }

        /**
         * Logs in the given tool {@code user}.
         *
         * @param request Can't be {@code null}.
         * @param response Can't be {@code null}.
         * @param user Can't be {@code null}.
         */
        public static void logIn(HttpServletRequest request, HttpServletResponse response, ToolUser user) {
            String token = (String) request.getAttribute(USER_TOKEN);

            if (token == null || (user != null && user.getId().toString().equals(token))) {
                token = user.generateLoginToken();

            } else {
                boolean matched = false;
                boolean removed = false;
                boolean refreshed = false;

                for (java.util.Iterator<ToolUser.LoginToken> i = user.getLoginTokens().iterator(); i.hasNext();) {
                    ToolUser.LoginToken loginToken = i.next();

                    if (loginToken.getToken().equals(token)) {
                        if (loginToken.refreshTokenIfNecessary()) {
                            refreshed = true;
                        }
                        matched = true;

                    } else if (!loginToken.isValid()) {
                        i.remove();
                        removed = true;
                    }
                }

                if (!matched) {
                    token = user.generateLoginToken();
                }

                if (refreshed || removed) {
                    user.save();
                }
            }

            setSignedCookie(request, response, TOOL_USER_COOKIE, token, -1, true);
            setSignedCookie(request, response, INSECURE_TOOL_USER_COOKIE, token, -1, false);
            request.setAttribute(USER_ATTRIBUTE, user);
            request.setAttribute(USER_TOKEN, token);
            request.setAttribute(USER_CHECKED_ATTRIBUTE, Boolean.TRUE);
        }

        /**
         * Logs out the current tool user.
         *
         * @param request Can't be {@code null}.
         * @param response Can't be {@code null}.
         */
        public static void logOut(HttpServletRequest request, HttpServletResponse response) {
            if (request != null) {
                ToolUser user = getUser(request);
                String token = (String) request.getAttribute(USER_TOKEN);

                if (user != null) {
                    user.removeLoginToken(token);
                }
            }

            setSignedCookie(request, response, TOOL_USER_COOKIE, "", 0, true);
            setSignedCookie(request, response, INSECURE_TOOL_USER_COOKIE, "", 0, false);
        }

        /**
         * Logs out the current tool user.
         *
         * @param request Can't be {@code null}.
         * @param response Can't be {@code null}.
         * @deprecated Use {@link #logOut(HttpServletRequest, HttpServletResponse)} instead.
         */
        @Deprecated
        public static void logOut(HttpServletResponse response) {
            logOut(PageContextFilter.Static.getRequest(), response);
        }

        /**
         * Returns {@code true} if a tool user is authenticated in the given
         * {@code request}.
         *
         * @param request Can't be {@code null}.
         */
        public static boolean isAuthenticated(HttpServletRequest request) {
            return Boolean.TRUE.equals(request.getAttribute(AUTHENTICATED_ATTRIBUTE));
        }

        public static boolean requireUser(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException {
            String toolUrlPrefix = Settings.get(String.class, ToolPageContext.TOOL_URL_PREFIX_SETTING);

            if (!ObjectUtils.isBlank(toolUrlPrefix) &&
                    !new UrlBuilder(request).
                            currentScheme().
                            currentHost().
                            currentPath().
                            toString().startsWith(toolUrlPrefix)) {

                response.sendRedirect(
                        StringUtils.removeEnd(toolUrlPrefix, "/") +
                        new UrlBuilder(request).
                                currentPath().
                                currentParameters().
                                toString());

                return true;
            }

            ToolUser user = getUser(request);

            if (user != null) {
                request.setAttribute(AUTHENTICATED_ATTRIBUTE, Boolean.TRUE);

                logIn(request, response, user);

                ForwardingDatabase db = new ForwardingDatabase() {
                    @Override
                    protected <T> Query<T> filterQuery(Query<T> query) {
                        return query.clone().master().resolveInvisible();
                    }
                };

                db.setDelegate(Database.Static.getDefault());
                Database.Static.setIgnoreReadConnection(true);
                Database.Static.overrideDefault(db);
                request.setAttribute(DATABASE_OVERRIDDEN_ATTRIBUTE, Boolean.TRUE);

            } else if (!JspUtils.getEmbeddedServletPath(context, request.getServletPath()).equals(LOG_IN_PATH)) {
                @SuppressWarnings("resource")
                ToolPageContext page = new ToolPageContext(context, request, response);
                String loginUrl = page.cmsUrl(LOG_IN_PATH, RETURN_PATH_PARAMETER, JspUtils.getAbsolutePath(request, ""));

                response.sendRedirect(loginUrl);

                return true;
            }

            return false;
        }

        private static ToolUser getToolUserByCookieName(
                HttpServletRequest request,
                String cookieName,
                String toolUserAttribute,
                String toolUserCheckedAttribute) {

            ToolUser toolUser;

            if (Boolean.TRUE.equals(request.getAttribute(toolUserCheckedAttribute))) {
                toolUser = (ToolUser) request.getAttribute(toolUserAttribute);

            } else {
                long sessionTimeout = ObjectUtils.firstNonNull(
                        Settings.get(Long.class, TOOL_USER_SESSION_TIMEOUT_SETTING),
                        Settings.getOrDefault(long.class, "cms/tool/sessionTimeout", 0L));

                String cookieValue = JspUtils.getSignedCookieWithExpiry(request, cookieName, sessionTimeout);

                if (cookieValue == null || cookieValue.length() < cookieName.length()) {
                    toolUser = null;

                } else {
                    String token = cookieValue.substring(cookieName.length());
                    toolUser = ToolUser.Static.getByToken(token);

                    request.setAttribute(USER_TOKEN, token);
                    request.setAttribute(toolUserAttribute, toolUser);
                }

                request.setAttribute(toolUserCheckedAttribute, Boolean.TRUE);
            }

            return toolUser;
        }

        /**
         * Returns the tool user associated with the given {@code request}.
         *
         * @param request Can't be {@code null}.
         */
        public static ToolUser getUser(HttpServletRequest request) {
            return getToolUserByCookieName(
                    request,
                    TOOL_USER_COOKIE,
                    TOOL_USER_ATTRIBUTE,
                    TOOL_USER_CHECKED_ATTRIBUTE);
        }

        /**
         * Returns the possible and probably insecure tool user associated
         * with the given {@code request}.
         *
         * @param request Can't be {@code null}.
         */
        public static ToolUser getInsecureToolUser(HttpServletRequest request) {
            return getToolUserByCookieName(
                    request,
                    INSECURE_TOOL_USER_COOKIE,
                    INSECURE_TOOL_USER_ATTRIBUTE,
                    INSECURE_TOOL_USER_CHECKED_ATTRIBUTE);
        }

        public static Preview getCurrentPreview(HttpServletRequest request) {
            Preview preview;

            if (Boolean.TRUE.equals(request.getAttribute(PREVIEW_CHECKED_ATTRIBUTE))) {
                preview = (Preview) request.getAttribute(PREVIEW_ATTRIBUTE);

            } else {
                String cookieValue = JspUtils.getSignedCookie(request, PREVIEW_COOKIE);

                if (cookieValue == null || cookieValue.length() < PREVIEW_COOKIE.length()) {
                    preview = null;

                } else {
                    preview = Query.
                            from(Preview.class).
                            where("_id = ?", ObjectUtils.to(UUID.class, cookieValue.substring(PREVIEW_COOKIE.length()))).
                            first();

                    request.setAttribute(PREVIEW_ATTRIBUTE, preview);
                }

                request.setAttribute(PREVIEW_CHECKED_ATTRIBUTE, Boolean.TRUE);
            }

            return preview;
        }

        public static void setCurrentPreview(HttpServletRequest request, HttpServletResponse response, Preview preview) {
            setSignedCookie(request, response, PREVIEW_COOKIE, preview.getId().toString(), -1, true);
            request.setAttribute(PREVIEW_ATTRIBUTE, preview);
            request.setAttribute(PREVIEW_CHECKED_ATTRIBUTE, Boolean.TRUE);
        }

        public static void removeCurrentPreview(HttpServletRequest request, HttpServletResponse response) {
            setSignedCookie(request, response, PREVIEW_COOKIE, "", 0, true);
            request.removeAttribute(PREVIEW_ATTRIBUTE);
            request.removeAttribute(PREVIEW_CHECKED_ATTRIBUTE);
        }

        /**
         * Returns the user setting value associated with the given
         * {@code key}.
         */
        public static Object getUserSetting(HttpServletRequest request, String key) {
            ToolUser user = getUser(request);

            return user != null ? user.getSettings().get(key) : null;
        }

        /**
         * Puts the given user setting {@code value} at the given {@code key}.
         * The user, along with the setting values, are saved once at the end
         * of the given {@code request}.
         */
        public static void putUserSetting(HttpServletRequest request, String key, Object value) {
            ToolUser user = getUser(request);

            if (user != null) {
                user.getSettings().put(key, value);
                request.setAttribute(USER_SETTINGS_CHANGED_ATTRIBUTE, Boolean.TRUE);
            }
        }

        // Returns the page setting key for use with the given {@code request}
        // and {@code key}.
        private static String getPageSettingKey(HttpServletRequest request, String key) {
            return "page" + request.getServletPath() + "/" + key;
        }

        /**
         * Returns the page setting value associated with the given
         * {@code request} and {@code key}.
         */
        public static Object getPageSetting(HttpServletRequest request, String key) {
            return getUserSetting(request, getPageSettingKey(request, key));
        }

        /**
         * Puts the given page setting {@code value} at the given
         * {@code request} and {@code key}. The user, along with the setting
         * values, are saved once at the end of the given {@code request}.
         */
        public static void putPageSetting(HttpServletRequest request, String key, Object value) {
            putUserSetting(request, getPageSettingKey(request, key), value);
        }
    }
}
