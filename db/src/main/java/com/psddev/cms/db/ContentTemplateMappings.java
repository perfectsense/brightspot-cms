package com.psddev.cms.db;

import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Recordable;

import java.util.ArrayList;
import java.util.List;

@Recordable.FieldInternalNamePrefix("cms.contentTemplate.")
@Modification.Classes({ CmsTool.class, ToolRole.class, ToolUser.class })
public class ContentTemplateMappings extends Modification<Object> {

    private static final String TAB_NAME = "Content Templates";

    @ToolUi.Tab(TAB_NAME)
    private List<ContentTemplate> globalDefaults;

    @ToolUi.Tab(TAB_NAME)
    private List<ContentTemplate> globalExtras;

    @ToolUi.Tab(TAB_NAME)
    private List<SiteSpecificContentTemplates> siteSpecificDefaults;

    @ToolUi.Tab(TAB_NAME)
    private List<SiteSpecificContentTemplates> siteSpecificExtras;

    public List<ContentTemplate> getGlobalDefaults() {
        if (globalDefaults == null) {
            globalDefaults = new ArrayList<>();
        }
        return globalDefaults;
    }

    public void setGlobalDefaults(List<ContentTemplate> globalDefaults) {
        this.globalDefaults = globalDefaults;
    }

    public List<ContentTemplate> getGlobalExtras() {
        if (globalExtras == null) {
            globalExtras = new ArrayList<>();
        }
        return globalExtras;
    }

    public void setGlobalExtras(List<ContentTemplate> globalExtras) {
        this.globalExtras = globalExtras;
    }

    public List<SiteSpecificContentTemplates> getSiteSpecificDefaults() {
        if (siteSpecificDefaults == null) {
            siteSpecificDefaults = new ArrayList<>();
        }
        return siteSpecificDefaults;
    }

    public void setSiteSpecificDefaults(List<SiteSpecificContentTemplates> siteSpecificDefaults) {
        this.siteSpecificDefaults = siteSpecificDefaults;
    }

    public List<SiteSpecificContentTemplates> getSiteSpecificExtras() {
        if (siteSpecificExtras == null) {
            siteSpecificExtras = new ArrayList<>();
        }
        return siteSpecificExtras;
    }

    public void setSiteSpecificExtras(List<SiteSpecificContentTemplates> siteSpecificExtras) {
        this.siteSpecificExtras = siteSpecificExtras;
    }
}
