package com.psddev.cms.tool;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.util.CodeUtils;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.Lazy;
import com.psddev.dari.util.ObjectUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.Set;

final class ObjectTypeResourceBundle extends ListResourceBundle {

    private static final Lazy<ObjectTypeResourceBundle> GLOBAL_BUNDLE = new Lazy<ObjectTypeResourceBundle>() {

        @Override
        protected ObjectTypeResourceBundle create() {
            return new ObjectTypeResourceBundle(null);
        }
    };

    private static final LoadingCache<ObjectType, ObjectTypeResourceBundle> TYPE_BUNDLES = CacheBuilder
            .newBuilder()
            .weakKeys()
            .build(new CacheLoader<ObjectType, ObjectTypeResourceBundle>() {

                @Override
                @ParametersAreNonnullByDefault
                public ObjectTypeResourceBundle load(ObjectType type) {
                    return new ObjectTypeResourceBundle(type);
                }
            });

    static {
        CodeUtils.addRedefineClassesListener(classes -> {
            GLOBAL_BUNDLE.reset();

            classes.stream()
                    .map(ObjectType::getInstance)
                    .filter(type -> type != null)
                    .forEach(TYPE_BUNDLES::invalidate);
        });
    }

    private final Map<String, Object> map;
    private final Object[][] contents;

    public static ObjectTypeResourceBundle getInstance(ObjectType type) {
        return type != null
                ? TYPE_BUNDLES.getUnchecked(type)
                : GLOBAL_BUNDLE.get();
    }

    private ObjectTypeResourceBundle(ObjectType type) {
        Map<String, Object> map = new CompactMap<>();

        if (type != null) {
            map.put("displayName", type.getDisplayName());
        }

        Set<String> tabs = new HashSet<>();

        for (ObjectField field : type != null
                ? type.getFields()
                : Database.Static.getDefault().getEnvironment().getFields()) {
            String tab = field.as(ToolUi.class).getTab();

            if (!ObjectUtils.isBlank(tab)) {
                tabs.add(tab);
            }

            map.put("field." + field.getInternalName(), field.getDisplayName());
        }

        for (String tab : tabs) {
            map.put("tab." + tab, tab);
        }

        map.put("tab.Main", "Main");

        this.map = ImmutableMap.copyOf(map);
        this.contents = map.entrySet().stream()
                .map(entry -> new Object[] { entry.getKey(), entry.getValue() })
                .toArray(Object[][]::new);
    }

    public Map<String, Object> getMap() {
        return map;
    }

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
