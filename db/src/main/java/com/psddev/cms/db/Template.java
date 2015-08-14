package com.psddev.cms.db;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.psddev.dari.db.Database;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

// CHECKSTYLE:OFF
/**
 * @deprecated No direct replacement but {@link Renderer.LayoutPath} and
 * {@link Renderer.ListLayouts} provide similar functionality.
 */
@Deprecated
public class Template extends Page {

    /** @deprecated Use {@link Directory.Item#createPermalink} instead. */
    @Deprecated
    public static final String CSV_NORMALIZE_PATHS_ENGINE = "CSV/Normalize";

    /** @deprecated Use the fields directly. */
    @Deprecated
    public static final String FIELD_PREFIX = "cms.template.";

    /** @deprecated Use the field directly. */
    @Deprecated
    public static final String DEFAULT_FIELD = FIELD_PREFIX + "default";

    private Set<ObjectType> contentTypes;

    @Deprecated
    @ToolUi.NoteHtml("Deprecated. Please use <code>Directory.Item#createPermalink</code> instead.")
    private String pathsEngine;

    @Deprecated
    @ToolUi.NoteHtml("Deprecated. Please use <code>Directory.Item#createPermalink</code> instead.")
    private String pathsScript;

    @Deprecated
    @ToolUi.NoteHtml("Deprecated. Please use <code>Directory.Item#createPermalink</code> instead.")
    private Set<Directory> defaultDirectories;

    @Deprecated
    @ToolUi.NoteHtml("Deprecated. Please use Content Types instead.")
    private ObjectType mainContentType;

    /** Returns the set of types that can be used with this template. */
    public Set<ObjectType> getContentTypes() {
        if (contentTypes == null) {
            contentTypes = new LinkedHashSet<ObjectType>();
        }
        if (contentTypes.isEmpty() && mainContentType != null) {
            contentTypes.add(mainContentType);
        }
        return contentTypes;
    }

    /** Sets the set of types that can be used with this template. */
    public void setContentTypes(Set<ObjectType> contentTypes) {
        this.contentTypes = contentTypes;
    }

    /**
     * Returns the script engine used to create the directory paths.
     *
     * @deprecated Use {@link Directory.Item#createPermalink} instead.
     */
    @Deprecated
    public String getPathsEngine() {
        return pathsEngine;
    }

    /**
     * Sets the script engine used to create the directory paths.
     *
     * @deprecated Use {@link Directory.Item#createPermalink} instead.
     */
    @Deprecated
    public void setPathsEngine(String pathsEngine) {
        this.pathsEngine = pathsEngine;
    }

    /**
     * Returns the script used to create the directory paths.
     *
     * @deprecated Use {@link Directory.Item#createPermalink} instead.
     */
    @Deprecated
    public String getPathsScript() {
        return pathsScript;
    }

    /**
     * Set the script used to create the directory paths.
     *
     * @deprecated Use {@link Directory.Item#createPermalink} instead.
     */
    @Deprecated
    public void setPathsScript(String pathsScript) {
        this.pathsScript = pathsScript;
    }

    /**
     * Makes the directory paths for the given {@code object} using this
     * template's script settings.
     */
    public Set<Directory.Path> makePaths(Site site, Object object) {
        String engine = getPathsEngine();
        if (ObjectUtils.isBlank(engine) &&
                !ObjectUtils.isBlank(defaultDirectories)) {

            engine = CSV_NORMALIZE_PATHS_ENGINE;
            setPathsEngine(engine);

            StringBuilder sb = new StringBuilder();
            for (Directory directory : defaultDirectories) {
                sb.append(directory.getPath());
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            setPathsScript(sb.toString());
        }

        Directory.ObjectModification objectAsDirMod = State.getInstance(object).as(Directory.ObjectModification.class);
        String objectName = objectAsDirMod.getObjectName();
        if (ObjectUtils.isBlank(objectName)) {
            objectName = StringUtils.toNormalized(
                    State.getInstance(object).getLabel());
            objectAsDirMod.setObjectName(objectName);
        }

        Set<Directory.Path> paths = new LinkedHashSet<Directory.Path>();
        if (!ObjectUtils.isBlank(engine)) {

            String script = getPathsScript();
            if (CSV_NORMALIZE_PATHS_ENGINE.equals(engine)) {
                String item = "/" + objectName;
                if (ObjectUtils.isBlank(script)) {
                    addPath(paths, site, item, null);
                } else {
                    for (String prefix : StringUtils.split(script.trim(), "\\s*,\\s*")) {
                        addPath(paths, site, prefix + item, null);
                    }
                }

            } else {
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engineObj = manager.getEngineByName(engine);
                if (engineObj != null) {
                    engineObj.put("object", object);
                    engineObj.put("objectName", objectName);
                    try {
                        addPath(paths, site, engineObj.eval(script), null);
                    } catch (ScriptException ex) {
                        throw new RuntimeException(String.format(
                                "Unable to evaluate [%s] script!", engine),
                                ex);
                    }
                } else {
                    throw new IllegalArgumentException(String.format(
                            "[%s] is not a valid script engine!", engine));
                }
            }
        }

        return paths;
    }

    // For recursively adding the given {@code path} object to the given
    // {@code paths}.
    private void addPath(Set<Directory.Path> paths, Site site, Object path, Directory.PathType pathType) {
        if (path == null) {
            return;

        } else if (path instanceof Directory.Path) {
            paths.add((Directory.Path) path);

        } else if (path instanceof Iterable) {
            for (Object item : (Iterable<?>) path) {
                addPath(paths, site, item, pathType);
            }

        } else if (path.getClass().isArray()) {
            for (int i = 0, length = Array.getLength(path); i < length; ++ i) {
                addPath(paths, site, Array.get(path, i), pathType);
            }

        } else {
            if (pathType == null) {
                pathType = paths.isEmpty() ?
                        Directory.PathType.PERMALINK :
                        Directory.PathType.REDIRECT;
            }

            String normalized = Directory.normalizePath(path.toString());
            normalized = normalized.substring(0, normalized.length() - 1);
            paths.add(new Directory.Path(site, normalized, pathType));
        }
    }

    /** Modification that adds template information. */
    public static final class ObjectModification extends Modification<Object> {

        @Deprecated
        @Indexed
        @InternalName(DEFAULT_FIELD)
        private Template defaultTemplate;

        private ObjectModification() {
        }

        /** Returns the default template for this object. */
        public Template getDefault() {
            return defaultTemplate;
        }

        /** Sets the default template for this object. */
        public void setDefault(Template defaultTemplate) {
            this.defaultTemplate = defaultTemplate;
        }
    }

    /** Static utility methods. */
    public static final class Static {

        private Static() {
        }

        /**
         * Returns a cached list of all templates in the given {@code site}.
         */
        public static List<Template> findAll(Site site) {
            List<Template> templates = new ArrayList<Template>();
            for (Template template : Query.from(Template.class).sortAscending("name").selectAll()) {
                if (Site.Static.isObjectAccessible(site, template)) {
                    templates.add(template);
                }
            }
            return templates;
        }

        /**
         * Finds the template that should be used to render the given
         * {@code object} in the given {@code site}.
         */
        public static Template findRenderable(Object object, Site site) {
            if (object == null) {
                return null;
            }

            State objectState = State.getInstance(object);
            Template template = objectState.as(ObjectModification.class).getDefault();
            if (template != null && Site.Static.isObjectAccessible(site, template)) {
                return template;
            }

            ObjectType objectType = objectState.getType();
            List<Template> usable = new ArrayList<Template>();

            for (Template t : Query.from(Template.class).sortAscending("name").selectAll()) {
                if (Site.Static.isObjectAccessible(site, t) &&
                        t.getContentTypes().contains(objectType)) {
                    usable.add(t);
                }
            }

            return usable.size() == 1 ? usable.get(0) : null;
        }

        /**
         * Finds a list of all templates that are usable with the given
         * {@code object}.
         *
         * @return Never {@code null}. Mutable.
         */
        public static List<Template> findUsable(Object object) {
            List<Template> templates = new ArrayList<Template>();

            if (object != null) {
                State state = State.getInstance(object);
                Site owner = state.as(Site.ObjectModification.class).getOwner();
                ObjectType type = state.getType();

                for (Template template : Query.from(Template.class).sortAscending("name").selectAll()) {
                    if (template.getContentTypes().contains(type) &&
                            (owner == null ||
                            Site.Static.isObjectAccessible(owner, template))) {
                        templates.add(template);
                    }
                }
            }

            return templates;
        }

        /**
         * Finds a cached list of all types that are used by any of
         * the templates in the given {@code site}.
         */
        public static List<ObjectType> findUsedTypes(Site site) {
            Set<ObjectType> typesSet = new LinkedHashSet<ObjectType>();

            for (ObjectType type : Database.Static.getDefault().getEnvironment().getTypes()) {
                if (type.isConcrete() &&
                        type.getGroups().contains(Directory.Item.class.getName())) {
                    typesSet.add(type);
                }
            }

            typesSet.add(ObjectType.getInstance(Page.class));

            for (Template template : findAll(site)) {
                typesSet.addAll(template.getContentTypes());
            }

            List<ObjectType> types = new ArrayList<ObjectType>(typesSet);

            Collections.sort(types);

            return types;
        }
    }

    /** @deprecated Use {@link ObjectModification} or {@link Static} instead. */
    @Deprecated
    public static final class Global {

        private Global() {
        }

        /** @deprecated Use {@link ObjectModification#getDefault} instead. */
        @Deprecated
        public static Template getDefault(Object object) {
            return State.getInstance(object).as(ObjectModification.class).getDefault();
        }

        /** @deprecated Use {@link ObjectModification#setDefault} instead. */
        @Deprecated
        public static void setDefault(Object object, Template template) {
            State.getInstance(object).as(ObjectModification.class).setDefault(template);
        }

        /** @deprecated Use {@link Static#findAll} instead. */
        @Deprecated
        public static List<Template> findAll() {
            return Static.findAll(null);
        }

        /** @deprecated No replacement. */
        @Deprecated
        public static List<Template> findUsableByType(ObjectType type) {
            List<Template> templates = new ArrayList<Template>();
            for (Template template : Static.findAll(null)) {
                if (template.getContentTypes().contains(type)) {
                    templates.add(template);
                }
            }
            return templates;
        }

        /** @deprecated Use {@link Static#findUsable} instead. */
        @Deprecated
        public static List<Template> findUsable(Object object) {
            return Static.findUsable(object);
        }

        /** @deprecated Use {@link Static#findUsedTypes} instead. */
        @Deprecated
        public static List<ObjectType> findUsedTypes() {
            return Static.findUsedTypes(null);
        }
    }

    // --- Deprecated ---

    /** @deprecated Use {@link #makePaths(Site, Object)} instead. */
    @Deprecated
    public Set<String> makePaths(Object object) {
        Set<String> paths = new LinkedHashSet<String>();
        for (Directory.Path path : makePaths(null, object)) {
            paths.add(path.getPath());
        }
        return paths;
    }
}
