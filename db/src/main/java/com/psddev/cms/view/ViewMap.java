package com.psddev.cms.view;

import com.psddev.dari.db.Location;
import com.psddev.dari.db.Metric;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.Once;
import com.psddev.dari.util.StringUtils;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Am unmodifiable Map implementation that uses a view (java bean) as the
 * backing object for the keys and values within the map. This Map uses the
 * bean spec to map the getter methods of the backing view to the keys within
 * the map.
 */
class ViewMap implements Map<String, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewMap.class);

    private Map<String, Supplier<Object>> unresolved;

    private Map<String, Object> resolved;

    private boolean includeClassName;

    private Object view;

    private Once resolver = new Once() {
        @Override
        protected void run() throws Exception {
            ViewMap.this.keySet().forEach(ViewMap.this::get);
        }
    };

    /**
     * Creates a new Map backed by the specified view.
     *
     * @param view the view to wrap.
     */
    public ViewMap(Object view) {
        this(view, false);
    }

    /**
     * Creates a new Map backed by the specified view.
     *
     * @param view the view to wrap.
     */
    public ViewMap(Object view, boolean includeClassName) {
        this.includeClassName = includeClassName;
        this.view = view;
        this.unresolved = new HashMap<>();
        this.resolved = new HashMap<>();

        try {
            Arrays.stream(Introspector.getBeanInfo(view.getClass()).getPropertyDescriptors())
                    .filter((prop) -> includeClassName || !"class".equals(prop.getName()))
                    .forEach(prop -> unresolved.put(prop.getName(), () -> invoke(prop.getReadMethod(), view)));

        } catch (IntrospectionException e) {
            LOGGER.warn("Failed to introspect bean info for view of type ["
                    + view.getClass().getName() + "]. Cause: " + e.getMessage());
        }
    }

    /**
     * @return the backing view object for this map.
     */
    public Object getView() {
        return view;
    }

    @Override
    public int size() {
        return unresolved.size();
    }

    @Override
    public boolean isEmpty() {
        return unresolved.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return unresolved.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        resolver.ensure();
        return resolved.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        if (key instanceof String) {

            if (resolved.containsKey(key)) {
                return resolved.get(key);

            } else {
                Supplier<Object> supplier = unresolved.get(key);

                if (supplier != null) {
                    Object value = convertValue(supplier.get());
                    resolved.put((String) key, value);
                    return value;

                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(unresolved.keySet());
    }

    @Override
    public Collection<Object> values() {
        resolver.ensure();
        return Collections.unmodifiableCollection(resolved.values());
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        resolver.ensure();
        return Collections.unmodifiableSet(resolved.entrySet());
    }

    @Override
    public String toString() {

        return "{" + StringUtils.join(entrySet()
                .stream()
                .map((e) -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.toList()), ", ") + "}";
    }

    // Converts a value to a Json Map friendly value.
    // Types not currently handled... ReferentialText and Region
    private Object convertValue(Object value) {

        if (value instanceof CharSequence) {
            return value;

        } else if (value instanceof Boolean) {
            return value;

        } else if (value instanceof Number) {
            return value;

        } else if (value instanceof Date) {
            return ((Date) value).getTime();

        } else if (value instanceof URI || value instanceof URL) {
            return value.toString();

        } else if (value instanceof Locale) {
            return ((Locale) value).toLanguageTag();

        } else if (value instanceof File) {
            return ((File) value).getName();

        } else if (value instanceof UUID) {
            return value.toString();

        } else if (value instanceof Location) {
            return ImmutableMap.of(
                    "x", ((Location) value).getX(),
                    "y", ((Location) value).getY());

        } else if (value instanceof Metric) {
            return ((Metric) value).getSum();

        } else if (value instanceof State) { // should we handle this?
            return ((State) value).getSimpleValues();

        } else if (value instanceof Recordable) { // should we handle this?
            return ((Recordable) value).getState().getSimpleValues();

        } else if (value instanceof Iterable) {
            List<Object> immutableViewList = new ArrayList<>();

            for (Object item : (Iterable<?>) value) {
                immutableViewList.add(convertValue(item));
            }
            value = immutableViewList;

        } else if (value instanceof ViewMap) { // pass through
            return value;

        } else if (value instanceof Map) {
            return value;

        } else if (value instanceof Class) {
            return ((Class) value).getName();

        } else if (value instanceof Enum) {
            return value.toString();

        } else if (value != null) {
            value = new ViewMap(value, includeClassName);
        }

        return value;
    }

    private static Object invoke(Method method, Object view) {
        try {
            method.setAccessible(true);
            return method.invoke(view);

        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("Failed to invoke method: " + method, e);
            return null;
        }
    }
}
