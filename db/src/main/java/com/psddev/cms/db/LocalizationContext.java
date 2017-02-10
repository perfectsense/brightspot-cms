package com.psddev.cms.db;

import com.ibm.icu.text.MessageFormat;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.MachineTranslations;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CascadingMap;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.CollectionUtils;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LocalizationContext {

    private final String baseName;
    private final State state;
    private final Map<String, Object> overrides;

    public LocalizationContext(Object context, Map<String, Object> overrides) {
        String baseName = null;

        if (context instanceof ObjectField) {
            context = ((ObjectField) context).getParent();
        }

        State state = null;

        if (context != null) {
            if (context instanceof String) {
                baseName = (String) context;

            } else if (context instanceof ObjectType) {
                baseName = ((ObjectType) context).getInternalName();

            } else if (context instanceof DatabaseEnvironment) {
                baseName = null;

            } else if (context instanceof Class) {
                ObjectType type = ObjectType.getInstance((Class<?>) context);

                if (type != null) {
                    baseName = type.getInternalName();

                } else {
                    baseName = ((Class<?>) context).getName();
                }

            } else if (context instanceof Recordable) {
                state = ((Recordable) context).getState();
                ObjectType type = state.getType();

                if (type != null) {
                    baseName = type.getInternalName();
                }

            } else {
                baseName = context.getClass().getName();
            }
        }

        this.baseName = baseName;
        this.state = state;
        this.overrides = overrides;
    }

    public String text(Locale source, Locale target, String key) {
        CascadingMap<String, Object> arguments = new CascadingMap<>();
        List<Map<String, Object>> argumentsSources = arguments.getSources();

        if (overrides != null) {
            argumentsSources.add(overrides);
        }

        List<ClassLoader> customLoaders = ClassFinder
                .findConcreteClasses(LocalizationClassLoaderProvider.class)
                .stream()
                .map(c -> TypeDefinition.getInstance(c).newInstance().getClassLoader())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        ClassLoader defaultLoader = getClass().getClassLoader();
        String pattern = null;

        if (baseName != null) {
            for (ClassLoader customLoader : customLoaders) {
                ResourceBundle customOverride = findBundle(baseName + "Override", source, customLoader);

                if (customOverride != null) {
                    argumentsSources.add(createBundleMap(customOverride));

                    if (pattern == null) {
                        pattern = findBundleString(customOverride, key);
                    }
                }
            }

            ResourceBundle baseOverride = findBundle(baseName + "Override", source, defaultLoader);
            ResourceBundle baseDefault = findBundle(baseName + "Default", source, defaultLoader);

            if (baseOverride != null) {
                argumentsSources.add(createBundleMap(baseOverride));

                if (pattern == null) {
                    pattern = findBundleString(baseOverride, key);
                }
            }

            if (baseDefault != null) {
                argumentsSources.add(createBundleMap(baseDefault));

                if (pattern == null) {
                    pattern = findBundleString(baseDefault, key);
                }
            }
        }

        if (state != null) {
            argumentsSources.add(state);
        }

        if (pattern == null) {
            ResourceBundle fallbackOverride = findBundle("FallbackOverride", source, defaultLoader);

            if (fallbackOverride != null) {
                pattern = findBundleString(fallbackOverride, key);
            }

            if (pattern == null) {
                ResourceBundle fallbackDefault = findBundle("FallbackDefault", source, defaultLoader);

                if (fallbackDefault != null) {
                    pattern = findBundleString(fallbackDefault, key);
                }
            }
        }

        ObjectType type = ObjectType.getInstance(baseName);
        ObjectTypeResourceBundle bundle = ObjectTypeResourceBundle.getInstance(type);
        Map<String, Object> bundleMap = bundle.getMap();

        argumentsSources.add(bundleMap);

        if (pattern == null && Locale.getDefault().equals(source)) {
            pattern = findBundleString(bundle, key);
        }

        if (pattern == null) {
            return null;

        } else if (source.equals(target)) {
            return new MessageFormat(pattern, target).format(arguments);

        } else {
            String googleServerApiKey = Query
                    .from(CmsTool.class)
                    .first()
                    .getGoogleServerApiKey();

            if (ObjectUtils.isBlank(googleServerApiKey)) {
                return new MessageFormat(pattern, source).format(arguments);
            }

            // Already translated?
            UUID translationsId = MachineTranslations.createId(baseName, target);
            State translations = State.getInstance(Query
                    .from(MachineTranslations.class)
                    .where("_id = ?", translationsId)
                    .first());

            if (translations != null) {
                String translation = (String) translations.get(key);

                if (translation != null) {
                    argumentsSources.remove(bundleMap);
                    argumentsSources.add(new AbstractMap<String, Object>() {

                        private final Set<Entry<String, Object>> entries = bundleMap.keySet().stream()
                                .map(k -> new Entry<String, Object>() {

                                    @Override
                                    public String getKey() {
                                        return k;
                                    }

                                    @Override
                                    public Object getValue() {
                                        return text(source, target, k);
                                    }

                                    @Override
                                    public Object setValue(Object value) {
                                        throw new UnsupportedOperationException();
                                    }
                                })
                                .collect(Collectors.toSet());

                        @Nonnull
                        @Override
                        public Set<Entry<String, Object>> entrySet() {
                            return entries;
                        }
                    });

                    return new MessageFormat(translation, target).format(arguments);
                }
            }

            // Convert named arguments to be numbered to prevent the names
            // from being translated.
            MessageFormat numberedFormat = new MessageFormat(pattern, target);
            Map<String, String> numberedToNamed = null;

            if (numberedFormat.usesNamedArguments()) {

                // e.g. Hi, {name} -> Hi, {0}
                Map<String, Object> numberedArgumentByName = new CompactMap<>();
                int index = 0;

                for (String name : numberedFormat.getArgumentNames()) {
                    numberedArgumentByName.put(name, "{" + index + "}");
                    numberedFormat.setFormatByArgumentName(name, null);

                    ++ index;
                }

                // Use the numbered pattern to create a regex that can find
                // the named arguments from the original pattern.
                String numberedPattern = numberedFormat.format(numberedArgumentByName);
                Matcher numberedArgumentMatcher = Pattern.compile("\\{\\d+\\}").matcher(numberedPattern);
                StringBuilder namedArgumentPattern = new StringBuilder();
                List<String> numberedArguments = new ArrayList<>();
                int lastEnd = 0;

                while (numberedArgumentMatcher.find()) {
                    namedArgumentPattern.append(Pattern.quote(numberedPattern.substring(lastEnd, numberedArgumentMatcher.start())));
                    namedArgumentPattern.append("(\\{.+?\\})");
                    numberedArguments.add(numberedArgumentMatcher.group(0));

                    lastEnd = numberedArgumentMatcher.end();
                }

                namedArgumentPattern.append(Pattern.quote(numberedPattern.substring(lastEnd)));

                // Map all numbered argument to the named ones.
                // e.g. {0} = {name}
                Matcher namedArgumentMatcher = Pattern.compile(namedArgumentPattern.toString()).matcher(pattern);
                pattern = numberedPattern;

                if (namedArgumentMatcher.matches()) {
                    numberedToNamed = new CompactMap<>();

                    for (int i = 1; i <= namedArgumentMatcher.groupCount(); ++ i) {
                        numberedToNamed.put(numberedArguments.get(i - 1), namedArgumentMatcher.group(1));
                    }

                } else {
                    throw new IllegalArgumentException();
                }
            }

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpUriRequest request = RequestBuilder.get()
                        .setUri("https://www.googleapis.com/language/translate/v2")
                        .addParameter("format", "text")
                        .addParameter("key", googleServerApiKey)
                        .addParameter("q", pattern)
                        .addParameter("source", source.getLanguage())
                        .addParameter("target", target.getLanguage())
                        .build();

                try (CloseableHttpResponse response = client.execute(request)) {
                    String responseText = EntityUtils.toString(response.getEntity());
                    String translation = (String) CollectionUtils.getByPath(ObjectUtils.fromJson(responseText), "data/translations/0/translatedText");

                    // Restore named arguments.
                    if (numberedToNamed != null) {
                        for (Map.Entry<String, String> entry : numberedToNamed.entrySet()) {
                            translation = translation.replace(entry.getKey(), entry.getValue());
                        }
                    }

                    final String finalTranslation = translation;
                    Thread saveTranslations = new Thread() {

                        @Override
                        public void run() {
                            com.psddev.dari.db.State translations = new MachineTranslations().getState();

                            translations.setId(translationsId);
                            translations.putAtomically(key, finalTranslation);
                            translations.save();
                        }
                    };

                    saveTranslations.start();

                    return new MessageFormat(translation, target).format(arguments);
                }

            } catch (IOException error) {
                return new MessageFormat(pattern, source).format(arguments);
            }
        }
    }

    private ResourceBundle findBundle(String baseName, Locale locale, ClassLoader loader) {
        try {
            return ResourceBundle.getBundle(
                    baseName,
                    locale,
                    loader,
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));

        } catch (MissingResourceException error) {
            return null;
        }
    }

    private Map<String, Object> createBundleMap(ResourceBundle bundle) {
        Map<String, Object> map = new CompactMap<>();

        for (Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements();) {
            String key = keys.nextElement();

            map.put(key, findBundleString(bundle, key));
        }

        return map;
    }

    private String findBundleString(ResourceBundle bundle, String key) {
        try {
            String pattern = bundle.getString(key);

            if (bundle instanceof ObjectTypeResourceBundle) {
                return pattern;

            } else {
                return new String(pattern.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            }

        } catch (MissingResourceException error) {
            return null;
        }
    }

    public String missingText(String key) {
        return "{" + baseName + "/" + key + "}";
    }
}
