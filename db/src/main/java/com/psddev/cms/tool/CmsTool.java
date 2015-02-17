package com.psddev.cms.tool;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Template;
import com.psddev.cms.db.ToolRole;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.page.ContentRevisions;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.CollectionUtils;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RepeatingTask;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;

/** Brightspot CMS. */
@CmsTool.DisplayName("CMS")
public class CmsTool extends Tool {

    private static final String ATTRIBUTE_PREFIX = CmsTool.class.getName() + ".";
    private static final String CSS_WRITTEN = ATTRIBUTE_PREFIX + ".cssWritten";

    private String companyName;
    private StorageItem companyLogo;
    private StorageItem backgroundImage;
    private String environment;

    @ToolUi.Tab("Defaults")
    private ToolRole defaultRole;

    @ToolUi.Tab("Debug")
    private Set<String> disabledPlugins;
    private String broadcastMessage;
    private Date broadcastExpiration;

    @ToolUi.Note("Check this to force the generated permalink to replace the existing URLs. This is useful before the site is live when the URL structure is still in flux.")
    @ToolUi.Tab("Debug")
    private boolean singleGeneratedPermalink;

    @ToolUi.CodeType("text/css")
    @ToolUi.Tab("Debug")
    private String extraCss;

    @ToolUi.CodeType("text/javascript")
    @ToolUi.Tab("Debug")
    private String extraJavaScript;

    @ToolUi.Tab("Defaults")
    private String defaultSiteUrl;

    @ToolUi.Tab("Defaults")
    private String defaultToolUrl;

    @ToolUi.Tab("Defaults")
    private List<CommonTime> commonTimes;

    @ToolUi.Tab("RTE")
    @ToolUi.CodeType("text/css")
    private String defaultTextOverlayCss;

    @ToolUi.Tab("RTE")
    private List<CssClassGroup> textCssClassGroups;

    @ToolUi.Tab("Dashboard")
    private List<ResourceItem> resources;

    @Deprecated
    @Embedded
    private Template modulePreviewTemplate;

    @ToolUi.Tab("Integrations")
    private String dropboxApplicationKey;

    @ToolUi.Tab("Dashboard")
    private CommonContentSettings commonContentSettings;

    @ToolUi.Tab("Dashboard")
    private BulkUploadSettings bulkUploadSettings;

    @ToolUi.Tab("Debug")
    private boolean useNonMinifiedCss;

    @ToolUi.Tab("Debug")
    private boolean useNonMinifiedJavaScript;

    @ToolUi.Tab("Debug")
    private boolean disableAutomaticallySavingDrafts;

    @ToolUi.Tab("Debug")
    private boolean displayTypesNotAssociatedWithJavaClasses;

    @ToolUi.Tab("RTE")
    private boolean legacyHtml;

    @ToolUi.Tab("RTE")
    private boolean enableAnnotations;

    @ToolUi.Tab("Debug")
    private boolean disableContentLocking;

    @ToolUi.Tab("Debug")
    private boolean removeTrailingSlashes;

    @ToolUi.Tab("Debug")
    private boolean disableToolChecks;

    @ToolUi.Tab("Debug")
    private boolean allowInsecureAuthenticationCookie;

    @ToolUi.Tab("Debug")
    private boolean displaySiteInSearchResult;

    @ToolUi.Tab("Debug")
    private boolean enableAbTesting;

    @ToolUi.Tab("Debug")
    private boolean alwaysGeneratePermalinks;

    @ToolUi.Tab("Debug")
    private List<DariSetting> dariSettings;

    @ToolUi.Placeholder("v2")
    @ToolUi.Tab("Debug")
    @ToolUi.Values({ "v3" })
    private String theme;

    private boolean enableCrossDomainInlineEditing;

    @Embedded
    public static class CommonTime extends Record {

        @Required
        private String displayName;

        @Maximum(23)
        @Minimum(0)
        @Required
        @ToolUi.Note("0 to 23")
        private int hour;

        @Maximum(59)
        @Minimum(0)
        @Required
        @ToolUi.Note("0 to 59")
        private int minute;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public int getHour() {
            return hour;
        }

        public void setHour(int hour) {
            this.hour = hour;
        }

        public int getMinute() {
            return minute;
        }

        public void setMinute(int minute) {
            this.minute = minute;
        }
    }

    @Embedded
    public static class CssClassGroup extends Record {

        @Required
        private String displayName;

        @Required
        private String internalName;

        private boolean dropDown;
        private List<CssClass> cssClasses;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getInternalName() {
            return internalName;
        }

        public void setInternalName(String internalName) {
            this.internalName = internalName;
        }

        public boolean isDropDown() {
            return dropDown;
        }

        public void setDropDown(boolean dropDown) {
            this.dropDown = dropDown;
        }

        public List<CssClass> getCssClasses() {
            if (cssClasses == null) {
                cssClasses = new ArrayList<CssClass>();
            }
            return cssClasses;
        }

        public void setCssClasses(List<CssClass> cssClasses) {
            this.cssClasses = cssClasses;
        }
    }

    @Embedded
    public static class CssClass extends Record {

        @Required
        private String displayName;

        @Required
        private String internalName;

        private String tag;

        @ToolUi.CodeType("text/css")
        private String css;

        @ToolUi.CodeType("text/css")
        private String cmsOnlyCss;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getInternalName() {
            return internalName;
        }

        public void setInternalName(String internalName) {
            this.internalName = internalName;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getCss() {
            return css;
        }

        public void setCss(String css) {
            this.css = css;
        }

        public String getCmsOnlyCss() {
            return cmsOnlyCss;
        }

        public void setCmsOnlyCss(String cmsOnlyCss) {
            this.cmsOnlyCss = cmsOnlyCss;
        }
    }

    /** Returns the company name. */
    public String getCompanyName() {
        return companyName;
    }

    /** Sets the company name. */
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    /** Returns the company logo. */
    public StorageItem getCompanyLogo() {
        return companyLogo;
    }

    /** Sets the company logo. */
    public void setCompanyLogo(StorageItem companyLogo) {
        this.companyLogo = companyLogo;
    }

    public StorageItem getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(StorageItem backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    /** Returns the environment. */
    public String getEnvironment() {
        return environment;
    }

    /** Sets the environment. */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * Returns the default role.
     *
     * @return May be {@code null}.
     */
    public ToolRole getDefaultRole() {
        return defaultRole;
    }

    /**
     * Sets the default role.
     *
     * @param defaultRole May be {@code null}.
     */
    public void setDefaultRole(ToolRole defaultRole) {
        this.defaultRole = defaultRole;
    }

    /**
     * Returns the set of disabled plugin names.
     *
     * @return Never {@code null}.
     */
    public Set<String> getDisabledPlugins() {
        if (disabledPlugins == null) {
            disabledPlugins = new HashSet<String>();
        }
        return disabledPlugins;
    }

    /** Sets the set of disabled plugin names. */
    public void setDisabledPlugins(Set<String> disabledPlugins) {
        this.disabledPlugins = disabledPlugins;
    }

    /**
     * Returns the broadcast message.
     *
     * @return May be {@code null}.
     */
    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    /**
     * Sets the broadcast message.
     *
     * @param broadcastMessage May be {@code null}.
     */
    public void setBroadcastMessage(String broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }

    /**
     * Returns the broadcast message expiration.
     *
     * @return If {@code null}, the message never expires.
     */
    public Date getBroadcastExpiration() {
        return broadcastExpiration;
    }

    /**
     * Sets the broadcast message expiration.
     *
     * @param broadcastExpiration If {@code null}, the message never
     * expires.
     */
    public void setBroadcastExpiration(Date broadcastExpiration) {
        this.broadcastExpiration = broadcastExpiration;
    }

    public boolean isSingleGeneratedPermalink() {
        return singleGeneratedPermalink;
    }

    public void setSingleGeneratedPermalink(boolean singleGeneratedPermalink) {
        this.singleGeneratedPermalink = singleGeneratedPermalink;
    }

    /** Returns the extra CSS. */
    public String getExtraCss() {
        return extraCss;
    }

    /** Sets the extra CSS. */
    public void setExtraCss(String css) {
        this.extraCss = css;
    }

    /** Returns the extra JavaScript. */
    public String getExtraJavaScript() {
        return extraJavaScript;
    }

    /** Sets the extra JavaScript. */
    public void setExtraJavaScript(String script) {
        this.extraJavaScript = script;
    }

    /** Returns the default site URL. */
    public String getDefaultSiteUrl() {
        return defaultSiteUrl;
    }

    /** Sets the default site URL. */
    public void setDefaultSiteUrl(String url) {
        this.defaultSiteUrl = url;
    }

    public String getDefaultToolUrl() {
        return ObjectUtils.isBlank(defaultToolUrl) ? getDefaultSiteUrl() : defaultToolUrl;
    }

    public void setDefaultToolUrl(String defaultToolUrl) {
        this.defaultToolUrl = defaultToolUrl;
    }

    public List<CommonTime> getCommonTimes() {
        if (commonTimes == null) {
            commonTimes = new ArrayList<CommonTime>();
        }
        return commonTimes;
    }

    public void setCommonTimes(List<CommonTime> commonTimes) {
        this.commonTimes = commonTimes;
    }

    public String getDefaultTextOverlayCss() {
        return defaultTextOverlayCss;
    }

    public void setDefaultTextOverlayCss(String defaultTextOverlayCss) {
        this.defaultTextOverlayCss = defaultTextOverlayCss;
    }

    public List<CssClassGroup> getTextCssClassGroups() {
        if (textCssClassGroups == null) {
            textCssClassGroups = new ArrayList<CssClassGroup>();
        }
        return textCssClassGroups;
    }

    public void setTextCssClassGroups(List<CssClassGroup> textCssClassGroups) {
        this.textCssClassGroups = textCssClassGroups;
    }

    public List<ResourceItem> getResources() {
        if (resources == null) {
            resources = new ArrayList<ResourceItem>();
        }
        return resources;
    }

    public void setResources(List<ResourceItem> resources) {
        this.resources = resources;
    }

    public Template getModulePreviewTemplate() {
        return modulePreviewTemplate;
    }

    public void setModulePreviewTemplate(Template modulePreviewTemplate) {
        this.modulePreviewTemplate = modulePreviewTemplate;
    }

    public String getDropboxApplicationKey() {
        return dropboxApplicationKey;
    }

    public void setDropboxApplicationKey(String dropboxApplicationKey) {
        this.dropboxApplicationKey = dropboxApplicationKey;
    }

    public CommonContentSettings getCommonContentSettings() {
        if (commonContentSettings == null) {
            commonContentSettings = new CommonContentSettings();
        }
        return commonContentSettings;
    }

    public void setCommonContentSettings(CommonContentSettings commonContentSettings) {
        this.commonContentSettings = commonContentSettings;
    }

    public BulkUploadSettings getBulkUploadSettings() {
        if (bulkUploadSettings == null) {
            bulkUploadSettings = new BulkUploadSettings();
        }
        return bulkUploadSettings;
    }

    public void setBulkUploadSettings(BulkUploadSettings bulkUploadSettings) {
        this.bulkUploadSettings = bulkUploadSettings;
    }

    public boolean isUseNonMinifiedCss() {
        return useNonMinifiedCss;
    }

    public void setUseNonMinifiedCss(boolean useNonMinifiedCss) {
        this.useNonMinifiedCss = useNonMinifiedCss;
    }

    public boolean isUseNonMinifiedJavaScript() {
        return useNonMinifiedJavaScript;
    }

    public void setUseNonMinifiedJavaScript(boolean useNonMinifiedJavaScript) {
        this.useNonMinifiedJavaScript = useNonMinifiedJavaScript;
    }

    /**
     * @deprecated Use {@link #isUseNonMinifiedCss} or {@link #isUseNonMinifiedJavaScript} instead.
     */
    @Deprecated
    public boolean isUseNonMinified() {
        return isUseNonMinifiedCss() &&
                isUseNonMinifiedJavaScript();
    }

    /**
     * @deprecated Use {@link #setUseNonMinifiedCss} or {@link #setUseNonMinifiedJavaScript} instead.
     */
    @Deprecated
    public void setUseNonMinified(boolean useNonMinified) {
        setUseNonMinifiedCss(useNonMinified);
        setUseNonMinifiedJavaScript(useNonMinified);
    }

    public boolean isDisableAutomaticallySavingDrafts() {
        return true;
        // return disableAutomaticallySavingDrafts;
    }

    public void setDisableAutomaticallySavingDrafts(boolean disableAutomaticallySavingDrafts) {
        this.disableAutomaticallySavingDrafts = disableAutomaticallySavingDrafts;
    }

    public boolean isDisplayTypesNotAssociatedWithJavaClasses() {
        return displayTypesNotAssociatedWithJavaClasses;
    }

    public void setDisplayTypesNotAssociatedWithJavaClasses(boolean displayTypesNotAssociatedWithJavaClasses) {
        this.displayTypesNotAssociatedWithJavaClasses = displayTypesNotAssociatedWithJavaClasses;
    }

    public boolean isLegacyHtml() {
        return legacyHtml;
    }

    public void setLegacyHtml(boolean legacyHtml) {
        this.legacyHtml = legacyHtml;
    }

    public boolean isEnableAnnotations() {
        return enableAnnotations;
    }

    public void setEnableAnnotations(boolean enableAnnotations) {
        this.enableAnnotations = enableAnnotations;
    }

    public boolean isDisableContentLocking() {
        return disableContentLocking;
    }

    public void setDisableContentLocking(boolean disableContentLocking) {
        this.disableContentLocking = disableContentLocking;
    }

    public boolean isRemoveTrailingSlashes() {
        return removeTrailingSlashes;
    }

    public void setRemoveTrailingSlashes(boolean removeTrailingSlashes) {
        this.removeTrailingSlashes = removeTrailingSlashes;
    }

    public boolean isDisableToolChecks() {
        return disableToolChecks;
    }

    public void setDisableToolChecks(boolean disableToolChecks) {
        this.disableToolChecks = disableToolChecks;
    }

    public boolean isAllowInsecureAuthenticationCookie() {
        return allowInsecureAuthenticationCookie;
    }

    public void setAllowInsecureAuthenticationCookie(
            boolean allowInsecureAuthenticationCookie) {
        this.allowInsecureAuthenticationCookie = allowInsecureAuthenticationCookie;
    }

    public boolean isDisplaySiteInSearchResult() {
        return displaySiteInSearchResult;
    }

    public void setDisplaySiteInSearchResult(boolean displaySiteInSearchResult) {
        this.displaySiteInSearchResult = displaySiteInSearchResult;
    }

    public boolean isEnableAbTesting() {
        return enableAbTesting;
    }

    public void setEnableAbTesting(boolean enableAbTesting) {
        this.enableAbTesting = enableAbTesting;
    }

    public boolean isAlwaysGeneratePermalinks() {
        return alwaysGeneratePermalinks;
    }

    public void setAlwaysGeneratePermalinks(boolean alwaysGeneratePermalinks) {
        this.alwaysGeneratePermalinks = alwaysGeneratePermalinks;
    }

    public List<DariSetting> getDariSettings() {
        if (dariSettings == null) {
            dariSettings = new ArrayList<DariSetting>();
        }
        return dariSettings;
    }

    public void setDariSettings(List<DariSetting> dariSettings) {
        this.dariSettings = dariSettings;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isEnableCrossDomainInlineEditing() {
        return enableCrossDomainInlineEditing;
    }

    public void setEnableCrossDomainInlineEditing(boolean enableCrossDomainInlineEditing) {
        this.enableCrossDomainInlineEditing = enableCrossDomainInlineEditing;
    }

    /** Returns the preview URL. */
    public String getPreviewUrl() {
        String url = getDefaultSiteUrl();
        if (!ObjectUtils.isBlank(url)) {
            try {
                return new URI(url + "/").resolve("./_preview").toString();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
        return "/_preview";
    }

    /** Writes the custom CSS to the given {@code out}. */
    public void writeCss(HttpServletRequest request, Writer out) throws IOException {
        if (request.getAttribute(CSS_WRITTEN) != null) {
            return;
        }

        request.setAttribute(CSS_WRITTEN, Boolean.TRUE);

        @SuppressWarnings("all")
        HtmlWriter writer = new HtmlWriter(out);

        writer.start("style", "type", "text/css");
            writer.css(".cms-textAlign-left", "text-align", "left");
            writer.css(".cms-textAlign-center", "text-align", "center");
            writer.css(".cms-textAlign-right", "text-align", "right");

            for (CmsTool.CssClassGroup group : getTextCssClassGroups()) {
                String groupName = group.getInternalName();

                for (CmsTool.CssClass cssClass : group.getCssClasses()) {
                    String css = cssClass.getCss();

                    if (css != null) {
                        writer.write(".cms-");
                        writer.write(groupName);
                        writer.write("-");
                        writer.write(cssClass.getInternalName());
                        writer.write("{");
                        writer.write(cssClass.getCss());
                        writer.write("}");
                    }
                }
            }

        writer.end();
    }

    // --- Tool support ---

    @Override
    public String getApplicationName() {
        return "cms";
    }

    @Override
    public List<Plugin> getPlugins() {
        List<Plugin> plugins = new ArrayList<Plugin>();

        // Areas.
        plugins.add(createArea2("Content", "dashboard", "dashboard", null));
        plugins.add(createArea2("Dashboard", "cms.dashboard", "dashboard/dashboard", "/"));
        plugins.add(createArea2("Admin", "admin", "admin", null));
        plugins.add(createArea2("Content Types", "cms.adminContentTypes", "admin/contentTypes", "/adminContentTypes"));
        plugins.add(createArea2("Production Guides", "adminGuides", "admin/adminGuides", "/admin/guides.jsp"));
        plugins.add(createArea2("Settings", "adminSettings", "admin/adminSettings", "/admin/settings.jsp"));
        plugins.add(createArea2("Sites", "adminSites", "admin/adminSites", "/admin/sites.jsp"));
        plugins.add(createArea2("Users & Roles", "adminUsers", "admin/adminUsers", "/admin/users.jsp"));
        plugins.add(createArea2("Variations & Profiles", "adminVariations", "admin/adminVariations", "/admin/variations.jsp"));
        plugins.add(createArea2("Workflows", "adminWorkflows", "admin/adminWorkflows", "/admin/workflows.jsp"));

        // Dashboard widgets.
        double dashboardColumn = 0.0;
        double dashboardRow = 0.0;

        plugins.add(createJspWidget("Work Streams", "dashboard.workStreams", "/workStreams", DASHBOARD_WIDGET_POSITION, dashboardColumn, dashboardRow ++));
        plugins.add(createJspWidget("Site Map", "dashboard.siteMap", "/misc/siteMap.jsp", DASHBOARD_WIDGET_POSITION, dashboardColumn, dashboardRow ++));
        plugins.add(createJspWidget("Recent Activity", "dashboard.recentActivity", "/misc/recentActivity.jsp", DASHBOARD_WIDGET_POSITION, dashboardColumn, dashboardRow ++));

        dashboardColumn ++;
        dashboardRow = 0.0;

        plugins.add(createJspWidget("Create New", "dashboard.createNew", "/createNew", DASHBOARD_WIDGET_POSITION, dashboardColumn, dashboardRow ++));
        plugins.add(createJspWidget("Bulk Upload", "dashboard.bulkUpload", "/bulkUpload", DASHBOARD_WIDGET_POSITION, dashboardColumn, dashboardRow ++));
        plugins.add(createJspWidget("Schedules", "dashboard.scheduledEvents", "/misc/scheduledEvents.jsp", DASHBOARD_WIDGET_POSITION, dashboardColumn, dashboardRow ++));
        plugins.add(createPageWidget("Drafts", "dashboard.unpublishedDrafts", "/unpublishedDrafts", DASHBOARD_WIDGET_POSITION, dashboardColumn, dashboardRow ++));
        plugins.add(createJspWidget("Resources", "dashboard.resources", "/resources", DASHBOARD_WIDGET_POSITION, dashboardColumn, dashboardRow ++));
        plugins.add(createJspWidget("Production Guides", "dashboard.productionGuides", "/misc/productionGuides", DASHBOARD_WIDGET_POSITION, dashboardColumn, dashboardRow ++));

        // Content right widgets.
        double rightColumn = 0.0;
        double rightRow = 0.0;
        JspWidget template, urls;

        if (isAlwaysGeneratePermalinks()) {
            plugins.add(urls = createJspWidget("URLs", "urls", "/WEB-INF/widget/urls.jsp", CONTENT_RIGHT_WIDGET_POSITION, rightColumn, rightRow ++));

        } else {
            plugins.add(urls = createJspWidget("URLs", "urls", "/WEB-INF/widget/urlsNew.jsp", CONTENT_RIGHT_WIDGET_POSITION, rightColumn, rightRow ++));
        }

        plugins.add(template = createJspWidget("Template", "template", "/WEB-INF/widget/template.jsp", CONTENT_RIGHT_WIDGET_POSITION, rightColumn, rightRow ++));
        plugins.add(createJspWidget("Sites", "sites", "/WEB-INF/widget/sites.jsp", CONTENT_RIGHT_WIDGET_POSITION, rightColumn, rightRow ++));
        plugins.add(new ContentRevisions());
        plugins.add(createPageWidget("References", "references", "/content/references", CONTENT_RIGHT_WIDGET_POSITION, rightColumn, rightRow ++));

        urls.getUpdateDependencies().add(template);

        // Content bottom widgets.
        double bottomColumn = 0.0;
        double bottomRow = 0.0;

        plugins.add(createJspWidget("Search Engine Optimization", "seo", "/WEB-INF/widget/seo.jsp", CONTENT_BOTTOM_WIDGET_POSITION, bottomColumn, bottomRow ++));

        return plugins;
    }

    @Embedded
    public abstract static class ResourceItem extends Record {

        private String name;
        private boolean sameWindow;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isSameWindow() {
            return sameWindow;
        }

        public void setSameWindow(boolean sameWindow) {
            this.sameWindow = sameWindow;
        }

        public abstract String getUrl();
    }

    public static class ResourceLink extends ResourceItem {

        private String url;

        @Override
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class ResourceFile extends ResourceItem {

        private StorageItem file;

        public StorageItem getFile() {
            return file;
        }

        public void setFile(StorageItem file) {
            this.file = file;
        }

        @Override
        public String getUrl() {
            StorageItem file = getFile();
            return file != null ? file.getPublicUrl() : null;
        }
    }

    @Embedded
    public static class CommonContentSettings extends Record {

        private Set<ObjectType> createNewTypes;
        private Set<Content> editExistingContents;

        public Set<ObjectType> getCreateNewTypes() {
            if (createNewTypes == null) {
                createNewTypes = new LinkedHashSet<ObjectType>();
            }
            return createNewTypes;
        }

        public void setCreateNewTypes(Set<ObjectType> createNewTypes) {
            this.createNewTypes = createNewTypes;
        }

        public Set<Content> getEditExistingContents() {
            if (editExistingContents == null) {
                editExistingContents = new LinkedHashSet<Content>();
            }
            return editExistingContents;
        }

        public void setEditExistingContents(Set<Content> editExistingContents) {
            this.editExistingContents = editExistingContents;
        }
    }

    @Embedded
    public static class BulkUploadSettings extends Record {

        private ObjectType defaultType;

        public ObjectType getDefaultType() {
            return defaultType;
        }

        public void setDefaultType(ObjectType defaultType) {
            this.defaultType = defaultType;
        }
    }

    @Embedded
    public static class DariSetting extends Record {

        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class DariSettingsUpdater extends RepeatingTask {

        private Date oldLastUpdate;

        @Override
        protected DateTime calculateRunTime(DateTime currentTime) {
            return every(currentTime, DateTimeFieldType.secondOfDay(), 0, 10);
        }

        @Override
        protected void doRepeatingTask(DateTime runTime) {
            Date newLastUpdate = Query.from(CmsTool.class).lastUpdate();

            if (newLastUpdate != null &&
                    (oldLastUpdate == null ||
                    !newLastUpdate.equals(oldLastUpdate))) {
                oldLastUpdate = newLastUpdate;
                Map<String, Object> settings = new CompactMap<String, Object>();

                for (DariSetting s : Query.from(CmsTool.class).first().getDariSettings()) {
                    CollectionUtils.putByPath(settings, s.getKey(), s.getValue());
                }

                if (!settings.isEmpty()) {
                    Settings.putPermanentOverrides("cms", settings);
                }
            }
        }
    }
}
