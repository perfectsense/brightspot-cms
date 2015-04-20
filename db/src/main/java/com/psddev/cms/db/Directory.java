package com.psddev.cms.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

@Record.BootstrapTypeMappable(groups = Directory.Item.class, uniqueKey = "path")
public class Directory extends Record {

    public static final String FIELD_PREFIX = "cms.directory.";
    public static final String PATHS_MODE_FIELD = FIELD_PREFIX + "pathsMode";
    public static final String OBJECT_NAME_FIELD = FIELD_PREFIX + "objectName";
    public static final String PATHS_FIELD = FIELD_PREFIX + "paths";
    public static final String PATH_TYPES_FIELD = FIELD_PREFIX + "pathTypes";

    private static final Pattern EXTERNAL_URL_PATTERN = Pattern.compile("(?i)/(https?:)/(.*)");

    @Indexed(unique = true)
    @Required
    private String path;

    /**
     * Cleans up the given {@code path} so that it always looks like
     * {@code /path/to/stuff/}.
     */
    public static String normalizePath(String path) {
        if (path == null || path.length() == 0) {
            return null;
        }
        path = "/" + path + "/";
        path = StringUtils.replaceAll(path, "/(?:/+|\\./)", "/");
        return path;
    }

    /**
     * Extracts a valid external URL from the given {@code path} if possible.
     *
     * @param path If {@code null}, returns {@code null}.
     * @return May be {@code null} if the path doesn't look like an external
     * url.
     */
    public static String extractExternalUrl(String path) {
        if (path != null) {
            Matcher externalUrlMatcher = EXTERNAL_URL_PATTERN.matcher(path);

            if (externalUrlMatcher.matches()) {
                return externalUrlMatcher.group(1) + "//" + externalUrlMatcher.group(2);
            }
        }

        return null;
    }

    /** Returns the path. */
    public String getPath() {
        return path;
    }

    /** Sets the path. */
    public void setPath(String path) {
        this.path = normalizePath(path);
    }

    /** Returns the raw path that is stored within other objects. */
    public String getRawPath() {
        return getId() + "/";
    }

    /**
     * Returns a predicate that filters out any items that's not associated
     * with this directory in the given {@code site}.
     */
    public Predicate itemsPredicate(Site site) {
        String path = getRawPath();
        if (site != null) {
            path = site.getRawPath() + path;
        }
        return PredicateParser.Static.parse(PATHS_FIELD + " ^= ?", path);
    }

    /** @deprecated Use {@link Static#findObject} instead. */
    @Deprecated
    public static Object findObjectByPath(Site site, String path) {
        return Static.findObject(site, path);
    }

    /** @deprecated Use {@link Static#findObject} instead. */
    @Deprecated
    public static Object findObjectByPath(String path) {
        return Static.findObject(null, path);
    }

    /** @deprecated Use {@link Static#hasPathPredicate} instead. */
    @Deprecated
    public static Predicate hasPathPredicate() {
        return Static.hasPathPredicate();
    }

    /** @deprecated Use {@link #itemsPredicate(Site)} instead. */
    @Deprecated
    public Predicate itemsPredicate() {
        return itemsPredicate(null);
    }

    @FieldInternalNamePrefix("cms.directory.")
    public static class Data extends Modification<Object> {

        private static final Pattern RAW_PATH_PATTERN = Pattern.compile("^(?:([^/]+):)?([^/]+)/([^/]+)$");

        @ToolUi.Hidden
        private Set<String> automaticRawPaths;

        public PathsMode getPathsMode() {
            return as(Directory.ObjectModification.class).getPathsMode();
        }

        public void setPathsMode(PathsMode pathsMode) {
            as(Directory.ObjectModification.class).setPathsMode(pathsMode);
        }

        public List<String> getRawPaths() {
            return as(Directory.ObjectModification.class).getRawPaths();
        }

        public void setRawPaths(List<String> rawPaths) {
            as(Directory.ObjectModification.class).setRawPaths(rawPaths);
        }

        public Set<String> getAutomaticRawPaths() {
            if (automaticRawPaths == null) {
                automaticRawPaths = new LinkedHashSet<String>();
            }
            return automaticRawPaths;
        }

        public void setAutomaticRawPaths(Set<String> automaticRawPaths) {
            this.automaticRawPaths = automaticRawPaths;
        }

        /**
         * Adds the given {@code path} in the given {@code site} with the
         * given {@code type} to this object.
         *
         * @param site May be {@code null}.
         * @param path If blank, does nothing.
         * @param type May be {@code null}.
         */
        public void addPath(Site site, String path, PathType type) {
            as(Directory.ObjectModification.class).addSitePath(site, path, type);
        }

        /**
         * Clears all paths associated with this object.
         */
        public void clearPaths() {
            Directory.ObjectModification dirData = as(Directory.ObjectModification.class);

            dirData.getRawPaths().clear();
            dirData.getPathTypes().clear();
        }

        private Set<Path> convertRawPaths(Collection<String> rawPaths) {
            if (ObjectUtils.isBlank(rawPaths)) {
                return Collections.emptySet();
            }

            Directory.ObjectModification dirData = as(Directory.ObjectModification.class);
            Map<String, PathType> pathTypes = dirData.getPathTypes();
            Set<Path> paths = new LinkedHashSet<Path>();

            for (String rawPath : rawPaths) {
                Matcher rawPathMatcher = RAW_PATH_PATTERN.matcher(rawPath);

                if (rawPathMatcher.matches()) {
                    UUID directoryId = ObjectUtils.to(UUID.class, rawPathMatcher.group(2));
                    Directory directory = Query.
                            from(Directory.class).
                            where("_id = ?", directoryId).
                            first();

                    if (directory != null) {
                        String path = directory.getPath() + rawPathMatcher.group(3);
                        Site site = Query.
                                from(Site.class).
                                where("_id = ?", ObjectUtils.to(UUID.class, rawPathMatcher.group(1))).
                                first();

                        if (path.endsWith("/index")) {
                            path = path.substring(0, path.length() - 5);
                        }

                        paths.add(new Path(site, path, pathTypes.get(rawPath)));
                    }
                }
            }

            return paths;
        }

        /**
         * Returns a set of all paths associated with this object.
         *
         * @return Never {@code null}.
         */
        public Set<Path> getPaths() {
            return convertRawPaths(as(Directory.ObjectModification.class).getRawPaths());
        }

        public Set<Path> getAutomaticPaths() {
            return convertRawPaths(getAutomaticRawPaths());
        }

        public Set<Path> getManualPaths() {
            List<String> rawPaths = as(Directory.ObjectModification.class).getRawPaths();

            rawPaths.removeAll(getAutomaticRawPaths());
            return convertRawPaths(rawPaths);
        }
    }

    /** How the paths should be constructed. */
    public enum PathsMode {

        AUTOMATIC("Automatic"),
        MANUAL("Manual");

        private final String displayName;

        private PathsMode(String displayName) {
            this.displayName = displayName;
        }

        // --- Object support ---

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** How the path should be interpreted. */
    public enum PathType {

        PERMALINK("Permalink"),
        REDIRECT("Redirect"),
        ALIAS("Alias");

        private final String displayName;

        private PathType(String displayName) {
            this.displayName = displayName;
        }

        // --- Object support ---

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static class Path {

        private final Site site;
        private final String path;
        private final PathType type;

        public Path(Site site, String path, PathType type) {
            this.site = site;
            this.path = path != null ? path.trim() : null;
            this.type = type != null ? type : PathType.PERMALINK;
        }

        public Site getSite() {
            return site;
        }

        public String getPath() {
            return path != null ? path.trim() : null;
        }

        public PathType getType() {
            return type;
        }

        // --- Object support ---

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;

            } else if (other instanceof Path) {
                Path otherPath = (Path) other;
                return ObjectUtils.equals(getSite(), otherPath.getSite()) &&
                        ObjectUtils.equals(getPath(), otherPath.getPath());

            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hashCode(getSite(), getPath());
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            Site site = getSite();

            s.append("{");

            if (site != null) {
                s.append("site=");
                s.append(site.getLabel());
                s.append(", ");
            }

            s.append("path=");
            s.append(getPath());
            s.append(", type=");
            s.append(getType());
            s.append("}");

            return s.toString();
        }
    }

    public interface Item {

        /** Creates the permalink appropriate for the given {@code site}. */
        public String createPermalink(Site site);
    }

    /** Modification that adds directory information. */
    public static final class ObjectModification extends Modification<Object> {

        @InternalName(PATHS_MODE_FIELD)
        @ToolUi.Hidden
        private PathsMode pathsMode;

        @InternalName(OBJECT_NAME_FIELD)
        @ToolUi.Hidden
        private String objectName;

        @Indexed(unique = true)
        @InternalName(PATHS_FIELD)
        @ToolUi.Hidden
        private List<String> paths;

        @InternalName(PATH_TYPES_FIELD)
        @ToolUi.Hidden
        private Map<String, PathType> pathTypes;

        /** Returns the paths mode. */
        public PathsMode getPathsMode() {
            return pathsMode;
        }

        /** Sets the paths mode. */
        public void setPathsMode(PathsMode pathsMode) {
            this.pathsMode = pathsMode;
        }

        /** Returns the object name. */
        public String getObjectName() {
            return objectName;
        }

        /** Sets the object name. */
        public void setObjectName(String objectName) {
            this.objectName = objectName;
        }

        /** Returns the raw paths. */
        public List<String> getRawPaths() {
            if (paths == null) {
                paths = new ArrayList<String>();
            }
            return paths;
        }

        /** Sets the raw paths. */
        public void setRawPaths(List<String> paths) {
            this.paths = paths;
        }

        /** Returns the path types. */
        public Map<String, PathType> getPathTypes() {
            if (pathTypes == null) {
                pathTypes = new LinkedHashMap<String, PathType>();
            }
            return pathTypes;
        }

        /** Sets the path types. */
        public void setPathTypes(Map<String, PathType> pathTypes) {
            this.pathTypes = pathTypes;
        }

        /**
         * Makes a raw path, like {@code siteId:directoryId/item},
         * using the given {@code site} and {@code path}.
         */
        private String makeRawPath(Site site, String path) {
            Matcher pathMatcher = StringUtils.getMatcher(normalizePath(path), "^(/.*?)([^/]*)/?$");
            pathMatcher.find();
            path = pathMatcher.group(1);

            Directory dir = Query.
                    from(Directory.class).
                    where("path = ?", path).
                    master().
                    noCache().
                    first();

            if (dir == null) {
                dir = new Directory();
                dir.setPath(path);
                dir.saveImmediately();
            }

            String rawPath = dir.getRawPath() + pathMatcher.group(2);
            if (site != null) {
                rawPath = site.getRawPath() + rawPath;
            }

            return rawPath;
        }

        /**
         * Returns all paths in the given {@code site} associated with
         * this object.
         */
        public List<Path> getSitePaths(Site site) {
            List<String> rawPaths = getRawPaths();
            if (ObjectUtils.isBlank(rawPaths)) {
                return Collections.emptyList();
            }

            Map<String, PathType> pathTypes = getPathTypes();
            List<Path> paths = new ArrayList<Path>();
            for (String rawPath : rawPaths) {

                Matcher rawPathMatcher = StringUtils.getMatcher(rawPath, "^(?:([^/]+):)?([^/]+)/([^/]+)$");
                if (rawPathMatcher.matches()) {

                    UUID siteId = ObjectUtils.to(UUID.class, rawPathMatcher.group(1));
                    if ((siteId == null && site == null) ||
                            (site != null && site.getId().equals(siteId))) {

                        UUID directoryId = ObjectUtils.to(UUID.class, rawPathMatcher.group(2));
                        if (directoryId != null) {
                            String directoryPath = null;

                            try {
                                directoryPath = DIRECTORY_PATHS.getUnchecked(directoryId);

                            } catch (UncheckedExecutionException error) {
                                Directory directory = Query.
                                        from(Directory.class).
                                        where("_id = ?", directoryId).
                                        first();

                                if (directory != null) {
                                    directoryPath = directory.getPath();
                                }
                            }

                            if (directoryPath != null) {
                                String path = directoryPath + rawPathMatcher.group(3);
                                if (path.endsWith("/index")) {
                                    path = path.substring(0, path.length() - 5);
                                }
                                paths.add(new Path(site, path, pathTypes.get(rawPath)));
                            }
                        }
                    }
                }
            }

            return paths;
        }

        private static final RuntimeException DIRECTORY_NOT_FOUND = new RuntimeException();

        private static final LoadingCache<UUID, String> DIRECTORY_PATHS = CacheBuilder.
                newBuilder().
                maximumSize(1000).
                build(new CacheLoader<UUID, String>() {

                    @Override
                    public String load(UUID directoryId) {
                        Directory directory = Query.
                                from(Directory.class).
                                where("_id = ?", directoryId).
                                first();

                        if (directory != null) {
                            return directory.getPath();

                        } else {
                            throw DIRECTORY_NOT_FOUND;
                        }
                    }
                });

        /**
         * Adds the given {@code path} in the given {@code site} with
         * the given {@code type} to this object.
         */
        public void addSitePath(Site site, String path, PathType type) {
            if (ObjectUtils.isBlank(path)) {
                return;
            }

            if (path.endsWith("/")) {
                path += "index";
            }

            List<String> rawPaths = getRawPaths();
            String rawPath = makeRawPath(site, path);

            if (!rawPaths.contains(rawPath)) {
                rawPaths.add(rawPath);
            }

            getPathTypes().put(rawPath, type);
        }

        /**
         * Clears all paths in the given {@code site} associated with
         * this object.
         */
        public void clearSitePaths(Site site) {
            String sitePrefix = site != null ? site.getRawPath() : null;
            Map<String, PathType> types = getPathTypes();
            for (Iterator<String> i = getRawPaths().iterator(); i.hasNext();) {
                String rawPath = i.next();
                if ((sitePrefix == null && !rawPath.contains(":")) ||
                        (sitePrefix != null && rawPath.startsWith(sitePrefix))) {
                    i.remove();
                    types.remove(rawPath);
                }
            }
        }

        /**
         * Removes the given {@code path} in the given {@code site} from
         * this object.
         */
        public void removeSitePath(Site site, String path) {
            String rawPath = makeRawPath(site, path);
            getRawPaths().remove(rawPath);
            getPathTypes().remove(rawPath);
        }

        /**
         * Returns the type of the given {@code path} in the given
         * {@code site} associated with this object.
         */
        public PathType getSitePathType(Site site, String path) {
            PathType type = getPathTypes().get(makeRawPath(site, path));
            return type != null ? type : PathType.PERMALINK;
        }

        /**
         * Puts the given {@code type} into the given {@code path} in the
         * given {@code site} stored in this object.
         */
        public void putSitePathType(Site site, String path, PathType type) {
            Map<String, PathType> types = getPathTypes();
            String rawPath = makeRawPath(site, path);
            if (type == null || type == PathType.PERMALINK) {
                types.remove(rawPath);
            } else {
                types.put(rawPath, type);
            }
        }

        /**
         * Returns the permalink in the given {@code site} for
         * this object.
         */
        public String getSitePermalink(Site site) {
            for (Path path : getSitePaths(site)) {
                if (path.getType() == PathType.PERMALINK) {
                    return site != null ? site.getPrimaryUrl() + path.getPath() : path.getPath();
                }
            }
            return null;
        }

        /**
         * Returns the permalink (path only) in the given {@code site} for
         * this object.
         */
        public String getSitePermalinkPath(Site site) {
            for (Path path : getSitePaths(site)) {
                if (path.getType() == PathType.PERMALINK) {
                    return path.getPath();
                }
            }
            return null;
        }

        /** Returns the site that owns this object. */
        private Site getOwner() {
            return as(Site.ObjectModification.class).getOwner();
        }

        /**
         * Returns all paths in the given {@linkplain
         * Site.ObjectModification#getOwner owner site} associated with
         * this object.
         */
        public List<Path> getPaths() {
            return getSitePaths(getOwner());
        }

        /**
         * Adds the given {@code path} in the {@linkplain
         * Site.ObjectModification#getOwner owner site} with the given
         * {@code type} to this object.
         */
        public void addPath(String path, PathType type) {
            addSitePath(getOwner(), path, type);
        }

        /**
         * Clears all paths in the {@linkplain
         * Site.ObjectModification#getOwner owner site} associated with
         * this object.
         */
        public void clearPaths() {
            clearSitePaths(getOwner());
        }

        /**
         * Removes the given {@code path} in the {@linkplain
         * Site.ObjectModification#getOwner owner site} from this object.
         */
        public void removePath(String path) {
            removeSitePath(getOwner(), path);
        }

        /**
         * Returns the type of the given {@code path} in the {@linkplain
         * Site.ObjectModification#getOwner owner site} associated with
         * this object.
         */
        public PathType getPathType(String path) {
            return getSitePathType(getOwner(), path);
        }

        /**
         * Puts the given {@code type} into the given {@code path} in the
         * {@linkplain Site.ObjectModification#getOwner owner site} stored
         * in this object.
         */
        public void putPathType(String path, PathType type) {
            putSitePathType(getOwner(), path, type);
        }

        /** Returns the best permalink associated with this object. */
        public String getPermalink() {
            String permalink = getSitePermalink(getOwner());
            if (ObjectUtils.isBlank(permalink)) {
                permalink = getSitePermalink(null);
            }
            return permalink;
        }

        /**
         * Returns the full permalink associated with this object.
         *
         * @throws IllegalStateException If {@link CmsTool#getDefaultSiteUrl}
         * returns blank.
         */
        public String getFullPermalink() {
            String siteUrl = Application.Static.getInstance(CmsTool.class).getDefaultSiteUrl();

            if (ObjectUtils.isBlank(siteUrl)) {
                throw new IllegalStateException("Default site URL not configured!");
            }

            return StringUtils.removeEnd(siteUrl, "/") + getPermalink();
        }

        /** Creates paths appropriate for the given {@code site}. */
        @SuppressWarnings("deprecation")
        public Set<Path> createPaths(Site site) {
            Object object = getOriginalObject();
            Set<Path> paths = new LinkedHashSet<Path>();
            Template template = as(Template.ObjectModification.class).getDefault();

            if (object instanceof Item) {
                paths.add(new Path(site, ((Item) object).createPermalink(site), PathType.PERMALINK));
            }

            if (template != null) {
                paths.addAll(template.makePaths(site, object));
            }

            return paths;
        }
    }

    /** Static utility methods. */
    public static final class Static {

        private Static() {
        }

        /**
         * Finds the object associated with the given {@code site} and
         * {@code path}.
         *
         * @param site May be {@code null}.
         * @param path If {@code null}, returns {@code null}.
         * @return May be {@code null}.
         */
        public static Object findByPath(Site site, String path) {
            path = normalizePath(path);

            if (path == null) {
                return null;
            }

            path = path.substring(0, path.length() - 1);
            int slashAt = path.lastIndexOf("/");

            if (slashAt > -1) {
                String name = path.substring(slashAt + 1);
                path = path.substring(0, slashAt + 1);
                Directory directory = Query.
                        from(Directory.class).
                        where("path = ?", path).
                        first();

                if (directory != null) {
                    String rawPath = directory.getRawPath() + name;
                    Object item = null;

                    if (site != null) {
                        item = findByRawPath(site.getRawPath() + rawPath);
                    }

                    if (item == null) {
                        item = findByRawPath(rawPath);
                    }

                    return item;
                }
            }

            return null;
        }

        private static Object findByRawPath(String rawPath) {
            Set<Object> invisibles = null;

            while (true) {
                Query<Object> query = Query.
                        fromAll().
                        and("cms.directory.paths = ?", rawPath);

                if (invisibles != null) {
                    query.and("_id != ?", invisibles);
                }

                Object item = query.first();

                if (item != null && !State.getInstance(item).isVisible()) {
                    if (invisibles == null) {
                        invisibles = new LinkedHashSet<Object>();
                    }

                    invisibles.add(item);
                    continue;
                }

                if (item != null) {
                    return item;

                } else if (invisibles != null && !invisibles.isEmpty()) {
                    return invisibles.iterator().next();

                } else {
                    return null;
                }
            }
        }

        /**
         * Finds the object associated with the given {@code site} and
         * {@code path}.
         *
         * @deprecated Use {@link #findByPath} instead. Note that the new
         * version doesn't return a directory object.
         */
        @Deprecated
        public static Object findObject(Site site, String path) {
            path = normalizePath(path);
            if (path == null) {
                return null;
            }

            Directory directory = Query.
                    from(Directory.class).
                    where("path = ?", path).
                    first();

            if (directory != null) {
                return directory;
            }

            path = path.substring(0, path.length() - 1);
            int slashAt = path.lastIndexOf("/");
            if (slashAt > -1) {
                String name = path.substring(slashAt + 1);
                path = path.substring(0, slashAt + 1);
                directory = Query.
                        from(Directory.class).
                        where("path = ?", path).
                        first();

                if (directory != null) {
                    String rawPath = directory.getRawPath() + name;
                    Object item = null;
                    if (site != null) {
                        item = Query.findUnique(Object.class, PATHS_FIELD, site.getRawPath() + rawPath);
                    }
                    if (item == null) {
                        item = Query.findUnique(Object.class, PATHS_FIELD, rawPath);
                    }
                    return item;
                }
            }

            return null;
        }

        /**
         * Returns a predicate that can be used to filter out any objects
         * that doesn't have a path.
         */
        public static Predicate hasPathPredicate() {
            return PredicateParser.Static.parse(PATHS_FIELD + " != missing");
        }
    }

    /** @deprecated Use {@link ObjectModification} instead. */
    @Deprecated
    public static final class Global {

        private Global() {
        }

        private static ObjectModification asMod(Object object) {
            return State.getInstance(object).as(ObjectModification.class);
        }

        /** @deprecated Use {@link ObjectModification#getPathsMode} instead. */
        @Deprecated
        public static PathsMode getPathsMode(Object object) {
            return asMod(object).getPathsMode();
        }

        /** @deprecated Use {@link ObjectModification#setPathsMode} instead. */
        @Deprecated
        public static void setPathsMode(Object object, PathsMode pathsMode) {
            asMod(object).setPathsMode(pathsMode);
        }

        /** @deprecated Use {@link ObjectModification#getObjectName} instead. */
        @Deprecated
        public static String getObjectName(Object object) {
            return asMod(object).getObjectName();
        }

        /** @deprecated Use {@link ObjectModification#setObjectName} instead. */
        @Deprecated
        public static void setObjectName(Object object, String objectName) {
            asMod(object).setObjectName(objectName);
        }

        /** @deprecated Use {@link ObjectModification#getSitePaths} instead. */
        @Deprecated
        public static List<Path> getSitePaths(Object object, Site site) {
            return asMod(object).getSitePaths(site);
        }

        /** @deprecated Use {@link ObjectModification#addSitePath} instead. */
        @Deprecated
        public static void addSitePath(Object object, Site site, String path, PathType type) {
            asMod(object).addSitePath(site, path, type);
        }

        /** @deprecated Use {@link ObjectModification#clearSitePaths} instead. */
        @Deprecated
        public static void clearSitePaths(Object object, Site site) {
            asMod(object).clearSitePaths(site);
        }

        /** @deprecated Use {@link ObjectModification#removeSitePath} instead. */
        @Deprecated
        public static void removeSitePath(Object object, Site site, String path) {
            asMod(object).removeSitePath(site, path);
        }

        /** @deprecated Use {@link ObjectModification#getSitePathType} instead. */
        @Deprecated
        public static PathType getSitePathType(Object object, Site site, String path) {
            return asMod(object).getSitePathType(site, path);
        }

        /** @deprecated Use {@link ObjectModification#putSitePathType} instead. */
        @Deprecated
        public static void putSitePathType(Object object, Site site, String path, PathType type) {
            asMod(object).putSitePathType(site, path, type);
        }

        /** @deprecated Use {@link ObjectModification#getSitePermalink} instead. */
        @Deprecated
        public static String getSitePermalink(Object object, Site site) {
            return asMod(object).getSitePermalink(site);
        }

        /** @deprecated Use {@link ObjectModification#getPaths} instead. */
        @Deprecated
        public static List<Path> getPaths(Object object) {
            return asMod(object).getPaths();
        }

        /** @deprecated Use {@link ObjectModification#addPath} instead. */
        @Deprecated
        public static void addPath(Object object, String path, PathType type) {
            asMod(object).addPath(path, type);
        }

        /** @deprecated Use {@link ObjectModification#clearPaths} instead. */
        @Deprecated
        public static void clearPaths(Object object) {
            asMod(object).clearPaths();
        }

        /** @deprecated Use {@link ObjectModification#removePath} instead. */
        @Deprecated
        public static void removePath(Object object, String path) {
            asMod(object).removePath(path);
        }

        /** @deprecated Use {@link ObjectModification#getPathType} instead. */
        @Deprecated
        public static PathType getPathType(Object object, String path) {
            return asMod(object).getPathType(path);
        }

        /** @deprecated Use {@link ObjectModification#putPathType} instead. */
        @Deprecated
        public static void putPathType(Object object, String path, PathType type) {
            asMod(object).putPathType(path, type);
        }

        /** @deprecated Use {@link ObjectModification#getPermalink} instead. */
        @Deprecated
        public static String getPermalink(Object object) {
            return asMod(object).getPermalink();
        }

        /** @deprecated No replacement. */
        @Deprecated
        public static String getPermalink(Object object, String prefix) {
            String permalink = null;
            for (Path path : getPaths(object)) {
                if (path.getType() == PathType.PERMALINK) {
                    String current = path.getPath();
                    if (permalink == null) {
                        permalink = current;
                    }
                    if (current.startsWith(prefix)) {
                        return current;
                    }
                }
            }
            return permalink;
        }
    }
}
