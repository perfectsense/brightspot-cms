package com.psddev.cms.tool.page;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.handler.OnMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class RtcHandler extends OnMessage<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtcHandler.class);

    private static final String CURRENT_USER_ID_ATTRIBUTE = "currentUserId";
    private static final String ACTIONS_ATTRIBUTE = "actions";

    private static final ConcurrentMap<String, RtcSession> SESSIONS = new ConcurrentHashMap<>();
    private static final AtmosphereResourceEventListener DISCONNECT_LISTENER = new AtmosphereResourceEventListenerAdapter.OnDisconnect() {

        @Override
        @SuppressWarnings("unchecked")
        public void onDisconnect(AtmosphereResourceEvent event) {
            AtmosphereResource resource = event.getResource();
            RtcSession session = SESSIONS.remove(resource.uuid());

            if (session != null) {
                Map<String, RtcAction> actions = session.getActions();

                if (!actions.isEmpty()) {
                    actions.values().forEach(RtcAction::destroy);
                }
            }
        }
    };

    @Override
    public void onOpen(AtmosphereResource resource) throws IOException {
        String uuid = resource.uuid();

        SESSIONS.putIfAbsent(uuid, new RtcSession());
        resource.addEventListener(DISCONNECT_LISTENER);

        ToolUser user = AuthenticationFilter.Static.getUser(resource.getRequest().wrappedRequest());

        if (user != null) {
            SESSIONS.get(uuid).setCurrentUserId(user.getId());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onMessage(AtmosphereResponse response, Object message) throws IOException {
        try {
            AtmosphereResource resource = response.resource();
            RtcSession session = SESSIONS.get(resource.uuid());

            if (session == null) {
                return;
            }

            UUID currentUserId = session.getCurrentUserId();

            if (currentUserId == null) {
                return;
            }

            if (message instanceof RtcBroadcastMessage) {
                RtcBroadcastMessage broadcastMessage = (RtcBroadcastMessage) message;

                writeBroadcast(
                        broadcastMessage.getBroadcast(),
                        broadcastMessage.getData(),
                        currentUserId,
                        resource);

                return;
            }

            Map<String, Object> messageJson = (Map<String, Object>) ObjectUtils.fromJson((String) message);

            if (!resource.uuid().equals(ObjectUtils.to(String.class, messageJson.get("resource")))) {
                return;
            }

            String stateClassName = ObjectUtils.to(String.class, messageJson.get("state"));

            if (!ObjectUtils.isBlank(stateClassName)) {
                RtcState state = createInstance(RtcState.class, stateClassName);

                for (Object object : state.create((Map<String, Object>) messageJson.get("data"))) {
                    RtcBroadcast.forEachBroadcast(object, (broadcast, data) ->
                            writeBroadcast(broadcast, data, currentUserId, resource));
                }

                return;
            }

            Map<String, RtcAction> actions = session.getActions();
            String actionClassName = ObjectUtils.to(String.class, messageJson.get("action"));
            RtcAction action = actions.get(actionClassName);

            if (action == null) {
                action = createInstance(RtcAction.class, actionClassName);

                action.initialize(currentUserId);
                actions.put(actionClassName, action);
            }

            action.execute((Map<String, Object>) messageJson.get("data"));

        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    private void writeBroadcast(RtcBroadcast<Object> broadcast, Map<String, Object> data, UUID currentUserId, AtmosphereResource resource) {
        if (broadcast.shouldBroadcast(data, currentUserId)) {
            resource.write(ObjectUtils.toJson(ImmutableMap.of(
                    "broadcast", broadcast.getClass().getName(),
                    "data", data)));
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
}
