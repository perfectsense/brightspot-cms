package com.psddev.cms.rtc;

import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.TypeDefinition;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * For capturing any updates to the instances of the given {@code <T>} type
 * and sending some data back to the client.
 *
 * <p>The broadcast can be received in JavaScript using:</p>
 *
 * <p><blockquote><pre>
 *     define([ 'v3/rtc' ], function(rtc) {
 *         rtc.receive('full.RtcBroadcastClassName', function(data) {
 *             // Do something with data.
 *         });
 *     });
 * </pre></blockquote></p>
 *
 * @param <T>
 *        The type of instances that the implementation should handle.
 *
 * @since 3.1
 */
public interface RtcBroadcast<T> {

    static <T> void forEachBroadcast(T object, BiConsumer<RtcBroadcast<T>, Map<String, Object>> consumer) {
        Set<RtcBroadcast<T>> broadcasts = new HashSet<>();

        BROADCAST:
        for (Class<?> c : ClassFinder.Static.findClasses(RtcBroadcast.class)) {
            if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
                continue;
            }

            for (Type broadcastInterface : c.getGenericInterfaces()) {
                if (broadcastInterface instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) broadcastInterface;
                    Type rt = pt.getRawType();

                    if (rt instanceof Class
                            && RtcBroadcast.class.isAssignableFrom((Class<?>) rt)) {

                        Type[] args = pt.getActualTypeArguments();

                        if (args.length > 0) {
                            Type arg = args[0];

                            if (arg instanceof Class
                                    && !((Class<?>) arg).isInstance(object)) {
                                continue BROADCAST;

                            } else {
                                break;
                            }
                        }
                    }
                }
            }

            @SuppressWarnings("unchecked")
            Class<RtcBroadcast<T>> broadcastClass = (Class<RtcBroadcast<T>>) c;
            RtcBroadcast<T> broadcast = TypeDefinition.getInstance(broadcastClass).newInstance();
            Map<String, Object> data = broadcast.create(object);

            if (data != null) {
                consumer.accept(broadcast, data);
            }
        }
    }

    /**
     * Returns {@code true} if the given {@code data} should be broadcast
     * to the user with the given {@code currentUserId}.
     *
     * @param data
     *        Can't be {@code null}.
     *
     * @param currentUserId
     *        Can't be {@code null}.
     */
    boolean shouldBroadcast(Map<String, Object> data, UUID currentUserId);

    /**
     * Creates the broadcast data based on the given {@code object}.
     *
     * @param object
     *        Can't be {@code null}.
     *
     * @return If {@code null}, doesn't send anything back to the client.
     */
    Map<String, Object> create(T object);
}
