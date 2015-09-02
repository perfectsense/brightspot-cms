package com.psddev.cms.rtc;

import java.util.Map;

/**
 * For sending the current state of something back to the client when it
 * first connects to the server.
 *
 * <p>The data can be requested in JavaScript using:</p>
 *
 * <p><blockquote><pre>
 *     define([ 'v3/rtc' ], function(rtc) {
 *         rtc.restore('full.RtcStateClassName', {
 *             parameters: 'for looking up the state'
 *         });
 *     });
 * </pre></blockquote></p>
 *
 * @since 3.1
 */
public interface RtcState {

    /**
     * Creates the broadcast data based on the given {@code data}.
     *
     * @param data
     *        Can't be {@code null}.
     *
     * @return Never {@code null}.
     */
    Iterable<?> create(Map<String, Object> data);
}
