package com.psddev.cms.tool.page;

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
     * Initializes this action with the given {@code currentUserId}.
     *
     * <p>This method will only be called once per client connection.</p>
     *
     * @param currentUserId
     *        Can't be {@code null}.
     */
    void initialize(UUID currentUserId);

    /**
     * Executes this action with the given {@code data}.
     *
     * <p>This method will be called as many times as client requests per
     * connection.</p>
     *
     * @param data
     *        Can't be {@code null}.
     */
    void execute(Map<String, Object> data);

    /**
     * Destroys this action.
     *
     * <p>This method will only be called once per client connection.</p>
     */
    void destroy();
}
