package com.psddev.cms.rtc;

import java.util.Map;
import java.util.UUID;

/**
 * For executing something on the server whenever a client requests it.
 *
 * <p>The request can be made in JavaScript using:</p>
 *
 * <p><blockquote><pre>
 *     define([ 'v3/rtc' ], function(rtc) {
 *         rtc.execute('full.RtcActionClassName', {
 *             parameters: 'for executing the action'
 *         });
 *     });
 * </pre></blockquote></p>
 *
 * @since 3.1
 */
public interface RtcAction {

    /**
     * Executes this action with the given {@code data} on behalf of a user
     * in a session.
     *
     * @param data
     *        Can't be {@code null}.
     *
     * @param userId
     *        Can't be {@code null}.
     *
     * @param sessionId
     *        Can't be {@code null}.
     */
    void execute(Map<String, Object> data, UUID userId, UUID sessionId);
}
