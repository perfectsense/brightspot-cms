package com.psddev.cms.rtc;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.AbstractFilter;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereFrameworkInitializer;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.util.VoidAnnotationProcessor;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Filter that handles the real-time communication between the server
 * and the clients.
 *
 * @since 3.1
 */
public class RtcFilter extends AbstractFilter implements AbstractFilter.Auto {

    public static final String PATH = "/_rtc";

    private static final Map<String, String> DEFAULT_INIT_PARAMETERS = ImmutableMap.<String, String>builder()
            .put(ApplicationConfig.ANALYTICS, Boolean.FALSE.toString())
            .put(ApplicationConfig.ANNOTATION_PROCESSOR, VoidAnnotationProcessor.class.getName())
            .put(ApplicationConfig.HEARTBEAT_INTERVAL_IN_SECONDS, String.valueOf(5))
            .build();

    private static final String ATTRIBUTE_PREFIX = RtcFilter.class.getName() + ".";
    private static final String USER_ID_ATTRIBUTE = ATTRIBUTE_PREFIX + "userId";

    private AtmosphereFrameworkInitializer initializer;
    private RtcObjectUpdateNotifier notifier;

    public static void setUserId(HttpServletRequest request, UUID uuid) {
        request.setAttribute(USER_ID_ATTRIBUTE, uuid);
    }

    public static UUID getUserId(HttpServletRequest request) {
        return (UUID) request.getAttribute(USER_ID_ATTRIBUTE);
    }

    @Override
    public void updateDependencies(Class<? extends AbstractFilter> filterClass, List<Class<? extends Filter>> dependencies) {
        dependencies.add(getClass());
    }

    @Override
    protected void doInit() throws ServletException {
        initializer = new AtmosphereFrameworkInitializer(false, true);
        FilterConfig filterConfig = getFilterConfig();

        initializer.configureFramework(new ServletConfig() {

            @Override
            public String getServletName() {
                return filterConfig.getFilterName();
            }

            @Override
            public ServletContext getServletContext() {
                return filterConfig.getServletContext();
            }

            @Override
            public String getInitParameter(String name) {
                String value = filterConfig.getInitParameter(name);

                return value != null ? value : DEFAULT_INIT_PARAMETERS.get(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                Set<String> names = new HashSet<>(Collections.list(filterConfig.getInitParameterNames()));

                names.addAll(DEFAULT_INIT_PARAMETERS.keySet());

                return Collections.enumeration(names);
            }
        });

        AtmosphereFramework framework = initializer.framework();

        framework.addAtmosphereHandler(
                PATH,
                new RtcHandler(),
                Arrays.asList(
                        new AtmosphereResourceLifecycleInterceptor(),
                        new TrackMessageSizeInterceptor()
                )
        );

        notifier = new RtcObjectUpdateNotifier(framework.getBroadcasterFactory().lookup(PATH));

        Database.Static.getDefault().addUpdateNotifier(notifier);
    }

    @Override
    protected void doDestroy() {
        try {
            if (notifier != null) {
                try {
                    Database.Static.getDefault().removeUpdateNotifier(notifier);

                } finally {
                    notifier = null;
                }
            }

        } finally {
            try {
                initializer.destroy();

            } finally {
                initializer = null;
            }
        }
    }

    @Override
    protected void doRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request.getServletPath().startsWith(PATH)) {
            CmsTool cms = Query.from(CmsTool.class).first();

            if (cms == null || !cms.isDisableRtc()) {
                initializer.framework().doCometSupport(
                        AtmosphereRequest.wrap(request),
                        AtmosphereResponse.wrap(response));

                return;
            }
        }

        chain.doFilter(request, response);
    }
}
