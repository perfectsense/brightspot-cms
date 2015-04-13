package com.psddev.cms.db;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.ReferentialText;
import com.psddev.dari.util.StorageItem;

/**
 * Production Guide class to hold information about Pages and Templates (objects
 * with layouts)
 *
 */

@Record.LabelFields({ "name" })
@Record.BootstrapPackages("Production Guides")
public class GuidePage extends Record {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GuidePage.class);

    @ToolUi.Note("Name for this template/page guide")
    @Required
    @Indexed(unique = true)
    @DisplayName("Name")
    @BootstrapFollowReferences
    String name;

    @ToolUi.Hidden
    @Deprecated
    // old association to template - retained
    Page pageType;

    @ToolUi.Note("Content Types associated with this page guide (will be displayable from their Edit form)")
    @Required
    @DisplayName("Page Types")
    @BootstrapFollowReferences
    @Indexed
    List<ObjectType> pageTypes;

    @ToolUi.Note("Production Guide summary for this page type")
    @DisplayName("Summary")
    ReferentialText description;

    @ToolUi.Note("Sample (Published) Page as documentation example for this page")
    @BootstrapFollowReferences
    private Content samplePage;

    @ToolUi.Note("Sample Page snapshot image used for Printouts")
    private StorageItem samplePageSnapshot;

    @ToolUi.Note("Production Guide section descriptions for this page")
    @Embedded
    private List<GuideSection> sectionDescriptions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Deprecated
    public Page getPageType() {
        return pageType;
    }

    @Deprecated
    public void setPageType(Page pageType) {
        this.pageType = pageType;
    }

    public List<ObjectType> getPageTypes() {
        return pageTypes;
    }

    public void setPageTypes(List<ObjectType> pageTypes) {
        this.pageTypes = pageTypes;
    }

    public ReferentialText getDescription() {
        return description;
    }

    public void setDescription(ReferentialText description) {
        this.description = description;
    }

    public List<GuideSection> getSectionDescriptions() {
        return sectionDescriptions;
    }

    public void setSectionDescriptions(List<GuideSection> sectionDescriptions) {
        this.sectionDescriptions = sectionDescriptions;
    }

    public Content getSamplePage() {
        return samplePage;
    }

    public void setSamplePage(Content samplePage) {
        this.samplePage = samplePage;
    }

    public StorageItem getSamplePageSnapshot() {
        return samplePageSnapshot;
    }

    public void setSamplePageSnapshot(StorageItem samplePageSnapshot) {
        this.samplePageSnapshot = samplePageSnapshot;
    }

    @Override
    public void beforeSave() {
        // beforeSave() override left in here for backward compatibility
        super.beforeSave();
    }

    /**
     * Return a boolean as to whether the basic information expected for this
     * production guide is available.
     */
    public boolean isIncomplete() {
        if (this.getSamplePage() == null ||
                this.getSamplePage().getPermalink() == null) {
            return true;
        }
        if (this.getDescription() == null || this.getDescription().isEmpty()) {
            return true;
        }
        return false;
    }

    @Deprecated
    public ReferentialText getSectionDescription(Section section) {
        return null;
    }

    @Deprecated
    public ReferentialText getSectionTips(Section section) {
        return null;
    }

    /**
     * Return the GuideSection entry that matches the given {@code section} of
     * the template associated with this guide object. If none exists, create
     * one.
     * @deprecated template section guides no longer automatically created
     */
    @Deprecated
    public GuideSection findOrCreateSectionGuide(Section section) {
        return null;
    }

    /**
     * Create a GuideSection entry in the sectionList for all sections in the
     * object's referenced page/template.
     * @deprecated template section guides no longer automatically created
     */
    @Deprecated
    public void generateSectionDescriptionList() {
    }

    /** Static utility methods. */
    public static final class Static {
        /**
         * - * Generate any missing guides for existing templates -
         * @deprecated template guides no longer automatically created
         */
        @Deprecated
        public static void createDefaultTemplateGuides() {
            return;
        }

    }

}
