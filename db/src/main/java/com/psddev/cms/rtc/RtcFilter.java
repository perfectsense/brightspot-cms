package com.psddev.cms.rtc;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Filter that handles the real-time communication between the server
 * and the clients.
 */
public class RtcFilter extends AbstractFilter implements AbstractFilter.Auto {

    public static final String PATH = "/_rtc";

    public static final String ASYNC_CONTEXT_TIMEOUT_SETTING = "brightspot/rtc/asyncContextTimeout";

    private static final String ATTRIBUTE_PREFIX = RtcFilter.class.getName() + ".";
    private static final String USER_ID_ATTRIBUTE = ATTRIBUTE_PREFIX + "userId";

    private final ConcurrentMap<UUID, RtcAsyncContext> contexts = new ConcurrentHashMap<>();
    private volatile RtcAsyncContextPingRunnable pingRunnable;
    private volatile ScheduledExecutorService pingExecutor;
    private volatile RtcSessionUpdateNotifier sessionUpdateNotifier;
    private volatile RtcEventUpdateNotifier eventUpdateNotifier;

    public static UUID getUserId(HttpServletRequest request) {
        return (UUID) request.getAttribute(USER_ID_ATTRIBUTE);
    }

    public static void setUserId(HttpServletRequest request, UUID userId) {
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
    }

    @Override
    public void updateDependencies(Class<? extends AbstractFilter> filterClass, List<Class<? extends Filter>> dependencies) {
        dependencies.add(getClass());
    }

    @Override
    protected void doInit() throws ServletException {

        // Ping all clients every 5 seconds to detect disconnects.
        pingRunnable = new RtcAsyncContextPingRunnable(contexts);
        pingExecutor = Executors.newSingleThreadScheduledExecutor();
        pingExecutor.scheduleWithFixedDelay(pingRunnable, 0, 5, TimeUnit.SECONDS);

        Database database = Database.Static.getDefault();

        sessionUpdateNotifier = new RtcSessionUpdateNotifier(contexts);
        database.addUpdateNotifier(sessionUpdateNotifier);

        eventUpdateNotifier = new RtcEventUpdateNotifier(contexts);
        database.addUpdateNotifier(eventUpdateNotifier);
    }

    @Override
    protected void doDestroy() {
        contexts.values().forEach(RtcAsyncContext::disconnect);
        contexts.clear();

        pingRunnable.stop();
        pingRunnable = null;

        pingExecutor.shutdownNow();
        pingExecutor = null;

        Database database = Database.Static.getDefault();

        if (sessionUpdateNotifier != null) {
            database.removeUpdateNotifier(sessionUpdateNotifier);
            sessionUpdateNotifier = null;
        }

        if (eventUpdateNotifier != null) {
            database.removeUpdateNotifier(eventUpdateNotifier);
            eventUpdateNotifier = null;
        }
    }

    @Override
    protected void doRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!request.getServletPath().startsWith(PATH)) {
            chain.doFilter(request, response);
            return;
        }

        // RTC disabled?
        CmsTool cms = Query.from(CmsTool.class).first();

        if (cms != null && cms.isDisableRtc()) {
            chain.doFilter(request, response);
            return;
        }

        // Make sure that the user is available.
        UUID userId = getUserId(request);

        if (userId == null) {
            ToolUser user = AuthenticationFilter.Static.getUser(request);

            if (user != null) {
                userId = user.getId();
            }
        }

        if (userId == null) {
            return;
        }

        // On GET, start the RTC connection.
        String method = request.getMethod();

        if ("get".equalsIgnoreCase(method)) {
            new RtcAsyncContext(contexts, request, userId);
            return;
        }

        // On POST...
        if (!"post".equalsIgnoreCase(method)) {
            throw new UnsupportedOperationException(String.format(
                    "[%s] method isn't supported!",
                    method));
        }

        // Make sure that the session is available.
        RtcSession session = Query
                .from(RtcSession.class)
                .where("_id = ?", ObjectUtils.to(UUID.class, request.getParameter("sessionId")))
                .first();

        if (session == null) {
            throw new IllegalArgumentException("Can't process RTC request without a session!");
        }

        String message = request.getParameter("message");
        @SuppressWarnings("unchecked")
        Map<String, Object> messageJson = (Map<String, Object>) ObjectUtils.fromJson(message);
        String messageType = (String) messageJson.get("type");

        // Ping from the client to prevent RtcSessionTask from deleting the
        // session.
        if ("ping".equals(messageType)) {
            session.setLastPing(Database.Static.getDefault().now());
            session.save();
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> messageData = (Map<String, Object>) messageJson.get("data");

        if ("restore".equals(messageType)) {
            Iterable<?> restores = createInstance(RtcState.class, messageJson).create(messageData);

            if (restores != null) {
                UUID currentUserId = userId;
                List<Map<String, Object>> items = new ArrayList<>();

                restores.forEach(event ->
                        RtcBroadcast.forEachBroadcast(event, (broadcast, data) -> {
                            if (broadcast.shouldBroadcast(data, currentUserId)) {
                                items.add(ImmutableMap.of(
                                        "broadcast", broadcast.getClass().getName(),
                                        "data", data));
                            }
                        }));

                if (!items.isEmpty()) {
                    response.setContentType("application/json");
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(ObjectUtils.toJson(items));
                    response.flushBuffer();
                }
            }

            return;
        }

        if ("execute".equals(messageType)) {
            createInstance(RtcAction.class, messageJson).execute(messageData, userId, session.getId());
            return;
        }

        if ("disconnect".equals(messageType)) {
            Iterable<?> disconnects = createInstance(RtcState.class, messageJson).close(messageData, userId);

            if (disconnects != null) {
                Database database = Database.Static.getDefault();

                database.beginWrites();

                try {
                    disconnects.forEach(event -> {
                        if (event instanceof RtcEvent) {
                            ((RtcEvent) event).onDisconnect();
                        }
                    });

                    database.commitWrites();

                } finally {
                    database.endWrites();
                }
            }

            return;
        }

        throw new UnsupportedOperationException(String.format(
                "[%s] type isn't supported!",
                messageType));
    }

    private <T> T createInstance(Class<T> returnClass, Map<String, Object> messageJson) {
        String className = (String) messageJson.get("className");
        Class<?> c = ObjectUtils.getClassByName(className);

        if (c == null) {
            throw new IllegalArgumentException(String.format(
                    "[%s] isn't a valid class name!",
                    className));

        } else if (!returnClass.isAssignableFrom(c)) {
            throw new IllegalArgumentException(String.format(
                    "[%s] isn't assignable from [%s]!",
                    returnClass.getName(),
                    c.getName()));
        }

        return (T) TypeDefinition.getInstance(c).newInstance();
    }
}
