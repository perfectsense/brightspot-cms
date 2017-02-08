package com.psddev.cms.rtc;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

class RtcAsyncContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtcAsyncContext.class);

    private final ConcurrentMap<UUID, RtcAsyncContext> contexts;
    private final AsyncContext context;
    private final UUID userId;
    private final UUID sessionId;
    private final AtomicBoolean disconnected = new AtomicBoolean();

    public RtcAsyncContext(
            ConcurrentMap<UUID, RtcAsyncContext> contexts,
            HttpServletRequest request,
            UUID userId)
            throws IOException {

        // Remove all request attributes to minimize memory usage.
        for (Enumeration<String> names = request.getAttributeNames(); names.hasMoreElements();) {
            request.removeAttribute(names.nextElement());
        }

        // Create the session first so that if there's a database error,
        // the underlying context isn't started.
        RtcSession session = new RtcSession();

        session.setUserId(userId);
        session.setLastPing(Database.Static.getDefault().now());
        session.save();

        this.contexts = contexts;
        this.context = request.startAsync();

        // Forcibly close the underlying context after some time to prevent
        // potential connection leaks.
        context.setTimeout(Settings.getOrDefault(long.class, RtcFilter.ASYNC_CONTEXT_TIMEOUT_SETTING, 15L * 60 * 1000));

        // Make sure everything's cleaned up when the underlying context
        // goes away for any reason.
        context.addListener(new RtcAsyncContextListener(this));

        this.userId = userId;
        this.sessionId = session.getId();
        contexts.put(sessionId, this);

        // Start the event stream and send the session ID to the client.
        ServletResponse response = context.getResponse();

        response.setContentType("text/event-stream");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        writeEvent(ImmutableMap.of(
                "_first", true,
                "sessionId", sessionId.toString()));
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void writeEvent(Map<String, Object> data) {
        try {
            ServletResponse response = context.getResponse();
            PrintWriter writer = response.getWriter();

            writer.write("data:");
            writer.write(ObjectUtils.toJson(data));
            writer.write("\n\n");

            // This is important to force an exception when the client
            // disconnects.
            response.flushBuffer();

        } catch (IOException | RuntimeException error) {
            disconnect();

            LOGGER.debug(
                    String.format("Can't write [%s] to [%s]!", data, this),
                    error);
        }
    }

    public void disconnect() {
        if (disconnected.compareAndSet(false, true)) {
            UUID sessionId = getSessionId();

            contexts.remove(sessionId);

            try {
                context.complete();

            } catch (RuntimeException error) {
                LOGGER.debug(
                        String.format("Can't complete [%s]!", this),
                        error);
            }

            RtcSession session = Query
                    .from(RtcSession.class)
                    .where("_id = ?", sessionId)
                    .first();

            if (session != null) {
                session.disconnect();
            }
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("userId", getUserId())
                .add("sessionId", getSessionId())
                .toString();
    }
}
