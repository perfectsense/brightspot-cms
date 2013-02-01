package com.psddev.cms.tool;

import com.psddev.cms.db.ToolUser;

import com.psddev.dari.db.Database;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.Settings;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthenticationFilter extends AbstractFilter {

    private static final String ATTRIBUTE_PREFIX = AuthenticationFilter.class.getName() + ".";
    public static final String AUTHENTICATED_ATTRIBUTE = ATTRIBUTE_PREFIX + "authenticated";
    public static final String USER_ATTRIBUTE = ATTRIBUTE_PREFIX + "user";
    public static final String USER_SETTINGS_CHANGED_ATTRIBUTE = ATTRIBUTE_PREFIX + "userSettingsChanged";

    public static final String LOG_IN_PATH = "/logIn.jsp";
    public static final String RETURN_PATH_PARAMETER = "returnPath";
    public static final String USER_COOKIE = "cmsToolUser";

    // --- AbstractFilter support ---

    @Override
    protected void doRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        try {
            chain.doFilter(request, response);

        } finally {
            Database.Static.setIgnoreReadConnection(false);

            ToolUser user = Static.getUser(request);

            if (user != null && Boolean.TRUE.equals(request.getAttribute(USER_SETTINGS_CHANGED_ATTRIBUTE))) {
                user.save();
            }
        }
    }

    /** {@link AuthenticationFilter} utility methods. */
    public static final class Static {

        private Static() {
        }

        /** Logs in the given tool {@code user}. */
        public static void logIn(HttpServletRequest request, HttpServletResponse response, ToolUser user) {
            Cookie cookie = new Cookie(USER_COOKIE, user.getId().toString());

            cookie.setPath("/");
            cookie.setSecure(JspUtils.isSecureRequest(request));
            JspUtils.setSignedCookie(response, cookie);

            request.setAttribute(USER_ATTRIBUTE, user);
        }

        /** Logs out the current tool user. */
        public static void logOut(HttpServletResponse response) {
            Cookie cookie = new Cookie(USER_COOKIE, null);

            cookie.setMaxAge(0);
            cookie.setPath("/");

            response.addCookie(cookie);
        }

        public static boolean requireUser(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException {
            long sessionTimeout = Settings.getOrDefault(long.class, "cms/tool/sessionTimeout", 0L);
            UUID userId = ObjectUtils.to(UUID.class, JspUtils.getSignedCookieWithExpiry(request, USER_COOKIE, sessionTimeout));
            ToolUser user = Query.findById(ToolUser.class, userId);

            if (user != null) {
                logIn(request, response, user);
                Database.Static.setIgnoreReadConnection(true);

            } else if (!JspUtils.getEmbeddedServletPath(context, request.getServletPath()).equals(LOG_IN_PATH)) {
                ToolPageContext page = new ToolPageContext(context, request, response);
                String loginUrl = page.cmsUrl(LOG_IN_PATH, RETURN_PATH_PARAMETER, JspUtils.getAbsolutePath(request, ""));

                response.sendRedirect(loginUrl);

                return true;
            }

            return false;
        }

        /** Returns the tool user associated with the given {@code request}. */
        public static ToolUser getUser(HttpServletRequest request) {
            return (ToolUser) request.getAttribute(USER_ATTRIBUTE);
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
