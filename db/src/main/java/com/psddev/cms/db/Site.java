package com.psddev.cms.db;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PeriodicValue;
import com.psddev.dari.util.PullThroughValue;
import com.psddev.dari.util.StorageItem;

/** Group of pages that's regarded as one entity. */
@ToolUi.IconName("object-site")
@Record.BootstrapPackages("Sites")
public class Site extends Record {

    private static final String FIELD_PREFIX = "cms.site.";
    private static final Logger LOGGER = LoggerFactory.getLogger(Site.class);

    public static final String OWNER_FIELD = FIELD_PREFIX + "owner";
    public static final String IS_GLOBAL_FIELD = FIELD_PREFIX + "isGlobal";
    public static final String BLACKLIST_FIELD = FIELD_PREFIX + "blacklist";
    public static final String CONSUMERS_FIELD = FIELD_PREFIX + "consumers";

    @Indexed(unique = true)
    @Required
    private String name;

    @ToolUi.Tab("Dashboard")
    private List<CmsTool.ResourceItem> resources;

    @ToolUi.Tab("Dashboard")
    private CmsTool.CommonContentSettings commonContentSettings;

    @ToolUi.Tab("Dashboard")
    private CmsTool.BulkUploadSettings bulkUploadSettings;

    @ToolUi.Tab("Advanced")
    private String cmsCssClass;

    @ToolUi.Tab("Advanced")
    private StorageItem cmsLogo;

    @Indexed(unique = true)
    private List<String> urls;

    @ToolUi.Tab("Advanced")
    private Variation defaultVariation;

    @ToolUi.Tab("Advanced")
    private Boolean isAllSitesAccessible;

    @ToolUi.Tab("Advanced")
    private Set<Site> accessibleSites;

    /** @deprecated Use {@link Static#findAll} instead. */
    @Deprecated
    public static List<Site> findAll() {
        return Static.findAll();
    }

    /** @deprecated Use {@link Static#findByUrl} instead. */
    @Deprecated
    public static Map.Entry<String, Site> findByUrl(String url) {
        return Static.findByUrl(url);
    }

    /** Returns the display name. */
    public String getName() {
        return name;
    }

    /** Sets the display name. */
    public void setName(String name) {
        this.name = name;
    }

    public List<CmsTool.ResourceItem> getResources() {
        if (resources == null) {
            resources = new ArrayList<CmsTool.ResourceItem>();
        }
        return resources;
    }

    public void setResources(List<CmsTool.ResourceItem> resources) {
        this.resources = resources;
    }

    public CmsTool.CommonContentSettings getCommonContentSettings() {
        return commonContentSettings;
    }

    public void setCommonContentSettings(CmsTool.CommonContentSettings commonContentSettings) {
        this.commonContentSettings = commonContentSettings;
    }

    public CmsTool.BulkUploadSettings getBulkUploadSettings() {
        return bulkUploadSettings;
    }

    public void setBulkUploadSettings(CmsTool.BulkUploadSettings bulkUploadSettings) {
        this.bulkUploadSettings = bulkUploadSettings;
    }

    public String getCmsCssClass() {
        return cmsCssClass;
    }

    public void setCmsCssClass(String cmsCssClass) {
        this.cmsCssClass = cmsCssClass;
    }

    public StorageItem getCmsLogo() {
        return cmsLogo;
    }

    public void setCmsLogo(StorageItem cmsLogo) {
        this.cmsLogo = cmsLogo;
    }

    /** Returns the list of URLs that prefix all pages in this site. */
    public List<String> getUrls() {
        if (urls == null) {
            urls = new ArrayList<String>();
        }
        return urls;
    }

    /** Sets the list of URLs that prefix all pages in this site. */
    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public Variation getDefaultVariation() {
        return defaultVariation;
    }

    public void setDefaultVariation(Variation defaultVariation) {
        this.defaultVariation = defaultVariation;
    }

    /** Returns {@code true} if this site can access all other sites. */
    public boolean isAllSitesAccessible() {
        return Boolean.TRUE.equals(isAllSitesAccessible);
    }

    /** Sets whether this site can access all other sites. */
    public void setAllSitesAccessible(boolean isAllSitesAccessible) {
        this.isAllSitesAccessible = isAllSitesAccessible ? Boolean.TRUE : null;
    }

    /** Returns the list of other sites that this one can access. */
    public Set<Site> getAccessibleSites() {
        if (accessibleSites == null) {
            accessibleSites = new LinkedHashSet<Site>();
        }
        return accessibleSites;
    }

    /** Sets the list of other sites that this one can access. */
    public void setAccessibleSites(Set<Site> accessibleSites) {
        this.accessibleSites = accessibleSites;
    }

    public String getPermissionId() {
        return "site/" + getId();
    }

    public String getPrimaryUrl() {
        if (getUrls().size() > 0) {
            String url = getUrls().get(0);
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            return url;
        }

        return null;
    }

    /** Returns the raw path that is stored within other objects. */
    public String getRawPath() {
        return getId() + ":";
    }

    /**
     * Returns a predicate that filters out any objects not accessible
     * by this site.
     */
    public Predicate itemsPredicate() {
        if (isAllSitesAccessible()) {
            return null;
        } else {
            Set<Site> consumers = new LinkedHashSet<Site>();
            consumers.add(this);
            consumers.addAll(getAccessibleSites());
            return PredicateParser.Static.parse(
                    Site.OWNER_FIELD + " = ? or ("
                            + Site.IS_GLOBAL_FIELD + " = ? and "
                            + Site.BLACKLIST_FIELD + " != ?) or "
                            + Site.CONSUMERS_FIELD + " = ?",
                    consumers, Boolean.TRUE, this, consumers);
        }
    }

    /** Static utility methods. */
    public static final class Static {

        private static final Comparator<Map.Entry<String, ?>> LONGEST_KEY_FIRST = new Comparator<Map.Entry<String, ?>>() {
            @Override
            public int compare(Map.Entry<String, ?> x, Map.Entry<String, ?> y) {
                int xLength = x.getKey().length();
                int yLength = y.getKey().length();
                return xLength > yLength ? -1 : xLength < yLength ? 1 : 0;
            }
        };

        private static final PullThroughValue<PeriodicValue<List<Site>>>
                INSTANCES = new PullThroughValue<PeriodicValue<List<Site>>>() {

            @Override
            protected PeriodicValue<List<Site>> produce() {
                return new PeriodicValue<List<Site>>() {

                    @Override
                    protected List<Site> update() {

                        Query<Site> query = Query.from(Site.class).sortAscending("name");
                        Date cacheUpdate = getUpdateDate();
                        Date databaseUpdate = query.lastUpdate();
                        if (databaseUpdate == null || (cacheUpdate != null && !databaseUpdate.after(cacheUpdate))) {
                            List<Site> sites = get();
                            return sites != null ? sites : Collections.<Site>emptyList();
                        }

                        LOGGER.info("Loading sites");
                        return query.selectAll();
                    }
                };
            }
        };

        private Static() {
        }

        /** Returns a cached list of all sites. */
        public static List<Site> findAll() {
            return new ArrayList<Site>(INSTANCES.get().get());
        }

        /** Finds a cached site associated with the given {@code url}. */
        public static Map.Entry<String, Site> findByUrl(String url) {
            if (url == null) {
                return null;
            }

            URI requestUri;
            try {
                requestUri = new URI(url);
            } catch (URISyntaxException ex) {
                try {
                    URL urlObject = new URL(url);
                    requestUri = new URI(urlObject.getProtocol(), urlObject.getAuthority(), urlObject.getHost(), urlObject.getPort(), urlObject.getPath(), urlObject.getQuery(), urlObject.getRef());
                } catch (MalformedURLException error2) {
                    return null;
                } catch (URISyntaxException error2) {
                    return null;
                }
            }

            Map<String, Site> checkUrlsMap = new HashMap<String, Site>();
            for (Site site : INSTANCES.get().get()) {
                for (String siteUrl : site.getUrls()) {
                    try {
                        String checkUrl = requestUri.resolve(siteUrl).toString();
                        if (!checkUrl.endsWith("/")) {
                            checkUrl += "/";
                        }
                        checkUrlsMap.put(checkUrl, site);
                    } catch (IllegalArgumentException e) {
                        // the url is malformed, just skip it
                    }
                }
            }

            List<Map.Entry<String, Site>> checkUrls = new ArrayList<Map.Entry<String, Site>>(checkUrlsMap.entrySet());
            Collections.sort(checkUrls, LONGEST_KEY_FIRST);

            if (!url.endsWith("/")) {
                url += "/";
            }
            for (Map.Entry<String, Site> entry : checkUrls) {
                if (url.startsWith(entry.getKey())) {
                    return entry;
                }
            }

            return null;
        }

        /**
         * Returns {@code true} if the given {@code object} is accessible
         * by the given {@code site}.
         */
        public static boolean isObjectAccessible(Site site, Object object) {
            State objectState = State.getInstance(object);
            Site.ObjectModification objectSiteMod = objectState.as(Site.ObjectModification.class);
            Site objectOwner = objectSiteMod.getOwner();

            if (ObjectUtils.equals(site, objectOwner)) {
                return true;

            } else if (site == null) {
                return false;

            } else if (site.isAllSitesAccessible()) {
                return true;

            } else if (site.getAccessibleSites().contains(objectOwner)) {
                return true;

            } else {
                return (objectSiteMod.isGlobal()
                        && !objectSiteMod.getBlacklist().contains(site))
                        || objectSiteMod.getConsumers().contains(site);
            }
        }
    }

    /** Modification that adds object accessibility information per site. */
    public static class ObjectModification extends Modification<Object> {

        @Indexed
        @InternalName(OWNER_FIELD)
        @ToolUi.Hidden
        private Site owner;

        @Indexed
        @InternalName(IS_GLOBAL_FIELD)
        @ToolUi.Hidden
        private Boolean isGlobal;

        @Indexed
        @InternalName(BLACKLIST_FIELD)
        @ToolUi.Hidden
        private Set<Site> blacklist;

        @Indexed
        @InternalName(CONSUMERS_FIELD)
        @ToolUi.Hidden
        private Set<Site> consumers;

        /** Returns the owner that controls this object. */
        public Site getOwner() {
            return owner;
        }

        /** Sets the owner that controls this object. */
        public void setOwner(Site owner) {
            this.owner = owner;
        }

        /**
         * Returns {@code true} if this object can be accessed globally
         * by any site.
         */
        public boolean isGlobal() {
            return Boolean.TRUE.equals(isGlobal);
        }

        /** Sets whether this object can be accessed globally by any site. */
        public void setGlobal(boolean isGlobal) {
            this.isGlobal = isGlobal ? Boolean.TRUE : null;
        }

        /**
         * Returns the set of blacklisted sites that aren't allowed to
         * access this object.
         */
        public Set<Site> getBlacklist() {
            if (blacklist == null) {
                blacklist = new LinkedHashSet<Site>();
            }
            return blacklist;
        }

        /**
         * Sets the set of blacklisted sites that aren't allowed to
         * access this object.
         */
        public void setBlacklist(Set<Site> blacklist) {
            this.blacklist = blacklist;
        }

        /**
         * Returns the set of consumer sites that are allowed to access
         * the object.
         */
        public Set<Site> getConsumers() {
            if (consumers == null) {
                consumers = new LinkedHashSet<Site>();
            }
            return consumers;
        }

        /**
         * Sets the set of consumer sites that are allowed to access
         * this object.
         */
        public void setConsumers(Set<Site> consumers) {
            this.consumers = consumers;
        }
    }
}
