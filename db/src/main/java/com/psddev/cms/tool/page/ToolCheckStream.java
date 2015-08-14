package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.ToolCheck;
import com.psddev.cms.tool.ToolCheckResponse;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.CodeUtils;
import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.TypeDefinition;

/**
 * @deprecated Use {@link RtcFilter} instead.
 */
@Deprecated
@RoutingFilter.Path(application = "cms", value = "toolCheckStream")
public class ToolCheckStream extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolCheckStream.class);

    private static final Lazy<Map<String, ToolCheck>> CHECKS = new Lazy<Map<String, ToolCheck>>() {

        {
            CodeUtils.addRedefineClassesListener(new CodeUtils.RedefineClassesListener() {
                @Override
                public void redefined(Set<Class<?>> classes) {
                    for (Class<?> c : classes) {
                        if (ToolCheck.class.isAssignableFrom(c)) {
                            reset();
                            break;
                        }
                    }
                }
            });
        }

        @Override
        protected Map<String, ToolCheck> create() {
            Map<String, ToolCheck> checks = new HashMap<String, ToolCheck>();

            for (Class<? extends ToolCheck> c : ClassFinder.Static.findClasses(ToolCheck.class)) {
                ToolCheck check = TypeDefinition.getInstance(c).newInstance();

                checks.put(check.getName(), check);
            }

            return checks;
        }
    };

    @Override
    protected void service(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {

        @SuppressWarnings("resource")
        ToolPageContext page = new ToolPageContext(getServletContext(), request, response);
        String url = page.param(String.class, "url");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checkRequests = (List<Map<String, Object>>) ObjectUtils.fromJson(page.param(String.class, "r"));
        ToolUserReference userReference = new ToolUserReference(page.getUser());
        long longPollTimeout = System.currentTimeMillis() + 30000;

        while (true) {
            List<ToolCheckResponse> checkResponses = new ArrayList<ToolCheckResponse>();
            boolean hasNonNullResponses = false;

            for (Map<String, Object> checkRequest : checkRequests) {
                String checkName = (String) checkRequest.get("check");

                try {
                    ToolCheck check = CHECKS.get().get(checkName);
                    ToolUser user = userReference.get();
                    ToolCheckResponse checkResponse = check.check(user, url, checkRequest);

                    checkResponses.add(checkResponse);

                    if (checkResponse != null) {
                        hasNonNullResponses = true;
                    }

                } catch (Exception error) {
                    LOGGER.debug(String.format("Can't run [%s] tool check!", checkName), error);
                }
            }

            if (hasNonNullResponses
                    || System.currentTimeMillis() > longPollTimeout) {
                response.setContentType("application/json");
                page.writeRaw(ObjectUtils.toJson(checkResponses));
                break;
            }

            try {
                Thread.sleep(1000);

            } catch (InterruptedException error) {
                break;
            }
        }
    }

    private static class ToolUserReference {

        private ToolUser user;
        private Long lastUpdate;

        /**
         * @param user May be {@code null}.
         */
        public ToolUserReference(ToolUser user) {
            this.user = user;
        }

        /**
         * @return May be {@code null}.
         */
        public ToolUser get() {
            if (user != null) {
                long newLastUpdate = Query
                        .from(ToolUser.class)
                        .where("_id = ?", user.getId())
                        .noCache()
                        .lastUpdate()
                        .getTime();

                if (lastUpdate == null) {
                    lastUpdate = newLastUpdate;

                } else if (lastUpdate != newLastUpdate) {
                    user = Query
                            .from(ToolUser.class)
                            .where("_id = ?", user.getId())
                            .noCache()
                            .first();
                }
            }

            return user;
        }
    }
}
