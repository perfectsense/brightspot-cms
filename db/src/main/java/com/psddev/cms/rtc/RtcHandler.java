package com.psddev.cms.rtc;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;
import com.psddev.dari.util.UuidUtils;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class RtcHandler extends AbstractReflectorAtmosphereHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtcHandler.class);

    private final LoadingCache<UUID, Optional<UUID>> userIds = CacheBuilder
            .newBuilder()
            .maximumSize(1000L)
            .build(new CacheLoader<UUID, Optional<UUID>>() {

                @Override
                @ParametersAreNonnullByDefault
                public Optional<UUID> load(UUID sessionId) throws Exception {
                    RtcSession session = Query
                            .from(RtcSession.class)
                            .where("_id = ?", sessionId)
                            .first();

                    return session != null
                            ? Optional.of(session.getUserId())
                            : Optional.empty();
                }
            });

    private final AtmosphereResourceEventListener disconnectListener = new AtmosphereResourceEventListenerAdapter.OnDisconnect() {

        @Override
        @SuppressWarnings("unchecked")
        public void onDisconnect(AtmosphereResourceEvent event) {
            UUID sessionId = createSessionId(event.getResource());

            userIds.invalidate(sessionId);

            RtcSession session = Query
                    .from(RtcSession.class)
                    .where("_id = ?", sessionId)
                    .first();

            if (session != null) {
                session.delete();

                Query.from(RtcEvent.class)
                        .where("cms.rtc.event.sessionId = ?", sessionId)
                        .selectAll()
                        .forEach(RtcEvent::onDisconnect);
            }
        }
    };

    private UUID createSessionId(AtmosphereResource resource) {
        String resourceUuid = resource.uuid();
        UUID sessionId = ObjectUtils.to(UUID.class, resourceUuid);

        if (sessionId == null) {
            sessionId = UuidUtils.createVersion3Uuid(resourceUuid);
        }

        return sessionId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRequest(AtmosphereResource resource) throws IOException {
        try {
            AtmosphereRequest request = resource.getRequest();
            String method = request.getMethod();

            if ("get".equalsIgnoreCase(method)) {
                ToolUser user = AuthenticationFilter.Static.getUser(resource.getRequest().wrappedRequest());

                if (user == null) {
                    return;
                }

                RtcSession session = new RtcSession();

                session.getState().setId(createSessionId(resource));
                session.setUserId(user.getId());
                session.save();
                resource.addEventListener(disconnectListener);

            } else if ("post".equalsIgnoreCase(method)) {
                try (InputStream requestInput = request.getInputStream()) {
                    String message = IoUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
                    Map<String, Object> messageJson = (Map<String, Object>) ObjectUtils.fromJson(message);
                    String className = (String) messageJson.get("className");
                    Map<String, Object> data = (Map<String, Object>) messageJson.get("data");
                    UUID sessionId = createSessionId(resource);
                    UUID userId = userIds.getUnchecked(sessionId).orElse(null);

                    if (userId == null) {
                        return;
                    }

                    switch ((String) messageJson.get("type")) {
                        case "action" :
                            createInstance(RtcAction.class, className)
                                    .execute(data, userId, sessionId);

                            break;

                        case "state" :
                            RtcState state = createInstance(RtcState.class, className);

                            for (Object object : state.create(data)) {
                                RtcBroadcast.forEachBroadcast(object, (broadcast, broadcastData) ->
                                        writeBroadcast(broadcast, broadcastData, userId, resource));
                            }

                            break;

                        default :
                            throw new UnsupportedOperationException();
                    }
                }
            }

        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    @Override
    public void onStateChange(AtmosphereResourceEvent event) throws IOException {
        try {
            if (event.isSuspended()) {
                Object message = event.getMessage();

                if (message instanceof RtcBroadcastMessage) {
                    RtcBroadcastMessage broadcastMessage = (RtcBroadcastMessage) message;
                    AtmosphereResource resource = event.getResource();
                    UUID userId = userIds.getUnchecked(createSessionId(resource)).orElse(null);

                    if (userId == null) {
                        return;
                    }

                    writeBroadcast(
                            broadcastMessage.getBroadcast(),
                            broadcastMessage.getData(),
                            userId,
                            resource);
                }
            }

            postStateChange(event);

        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T createInstance(Class<T> returnClass, String className) {
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

    private void writeBroadcast(
            RtcBroadcast<Object> broadcast,
            Map<String, Object> data,
            UUID currentUserId,
            AtmosphereResource resource) {

        if (broadcast.shouldBroadcast(data, currentUserId)) {
            resource.write(ObjectUtils.toJson(ImmutableMap.of(
                    "broadcast", broadcast.getClass().getName(),
                    "data", data)));
        }
    }
}
