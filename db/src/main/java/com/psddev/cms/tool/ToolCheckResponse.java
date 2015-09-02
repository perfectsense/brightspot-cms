package com.psddev.cms.tool;

import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated Use {@link com.psddev.cms.rtc.RtcAction} instead.
 */
@Deprecated
public class ToolCheckResponse extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    /**
     * @param action Can't be {@code null}.
     * @param parameters May be {@code null}.
     */
    public ToolCheckResponse(String action, Object... parameters) {
        put("action", action);

        if (parameters != null) {
            for (int i = 0, length = parameters.length; i < length; ++ i) {
                Object key = parameters[i];

                if (key == null) {
                    ++ i;
                    continue;
                }

                if (key instanceof Map) {
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) key).entrySet()) {
                        Object k = entry.getKey();
                        Object v = entry.getValue();

                        if (k != null && v != null) {
                            put(k.toString(), v);
                        }
                    }

                } else {
                    ++ i;

                    if (i < length) {
                        Object value = parameters[i];

                        if (value != null) {
                            put(key.toString(), value);
                        }
                    }
                }
            }
        }
    }
}
