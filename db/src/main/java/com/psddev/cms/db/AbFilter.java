package com.psddev.cms.db;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.primitives.Longs;
import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.ApplicationFilter;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

@Deprecated
public class AbFilter extends AbstractFilter implements AbstractFilter.Auto {

    private static final String SEED_COOKIE = "cms.ab";
    private static final String ATTRIBUTE_PREFIX = AbFilter.class.getName() + ".";
    private static final String SEED_ATTRIBUTE = ATTRIBUTE_PREFIX + "seed";

    @Override
    public void updateDependencies(
            Class<? extends AbstractFilter> filterClass,
            List<Class<? extends Filter>> dependencies) {
        if (ApplicationFilter.class.isAssignableFrom(filterClass)) {
            dependencies.add(getClass());
        }
    }

    @Override
    protected void doRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        CmsTool cms = Query.from(CmsTool.class).first();

        if (cms == null || !cms.isEnableAbTesting()) {
            chain.doFilter(request, response);
            return;
        }

        if (AuthenticationFilter.Static.isAuthenticated(request)) {
            chain.doFilter(request, response);
            return;
        }

        String seed = null;

        if (!"reset".equals(request.getParameter("_ab"))) {
            Cookie[] cookies = request.getCookies();

            if (cookies != null) {
                for (Cookie c : cookies) {
                    if (SEED_COOKIE.equals(c.getName())) {
                        seed = c.getValue();
                        break;
                    }
                }
            }
        }

        if (ObjectUtils.isBlank(seed)) {
            seed = UUID.randomUUID().toString();
            Cookie seedCookie = new Cookie(SEED_COOKIE, seed.toString());

            seedCookie.setPath("/");
            response.addCookie(seedCookie);
        }

        request.setAttribute(SEED_ATTRIBUTE, seed);

        AbDatabase ab = new AbDatabase();

        ab.setDelegate(Database.Static.getDefault());
        ab.setRequest(request);

        try {
            Database.Static.overrideDefault(ab);

            chain.doFilter(request, response);

        } finally {
            Database.Static.restoreDefault();
        }
    }

    /**
     * {@link AbFilter} utility methods.
     */
    public static class Static {

        public static double random(HttpServletRequest request, State state, String fieldName) {
            byte[] md5 = StringUtils.md5(String.valueOf(request.getAttribute(SEED_ATTRIBUTE)) + state.getId() + fieldName);
            long seed = Longs.fromByteArray(md5);
            double random = new Random(seed).nextDouble();

            return random;
        }
    }
}
