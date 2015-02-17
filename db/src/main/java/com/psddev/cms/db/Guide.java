package com.psddev.cms.db;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.ReferentialText;

/**
 * A Production Guide is Editor-oriented Help documentation providing guidance on how to create and organize content for a site. It's content
 * is associated with objects in the CMS so that helpful information is available contextually and can be easily kept up to date
 * as the site evolves.
 *
 * A Guide object consists of some overall descriptive content about programming, and then associations to templates/pages that
 * make up the site. Those templates and their associated content fields also have editorial guidance associated with them via the GuidePage and
 * GuideType classes
 */
@ToolUi.IconName("object-guide")
@Record.BootstrapPackages("Production Guides")
public class Guide extends Record {

    private static final Logger LOGGER = LoggerFactory.getLogger(Guide.class);

    @Required
    @Indexed(unique = true)
    @ToolUi.Note("Production Guide Title")
    private String title;

    @ToolUi.Note("Select the template/page guides to be included in this Production Guide in the order they should appear")
    @BootstrapFollowReferences
    private List<GuidePage> templatesToIncludeInGuide;

    @ToolUi.Note("Production Guide Overview Section")
    private ReferentialText overview;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ReferentialText getOverview() {
        return overview;
    }

    public void setOverview(ReferentialText overview) {
        this.overview = overview;
    }

    public List<GuidePage> getTemplatesToIncludeInGuide() {
        return templatesToIncludeInGuide;
    }

    public void setTemplatesToIncludeInGuide(
            List<GuidePage> templatesToIncludeInGuide) {
        this.templatesToIncludeInGuide = templatesToIncludeInGuide;
    }

    public boolean isIncomplete() {
        if (this.getOverview() == null || this.getOverview().isEmpty()) {
            return true;
        }
        return false;
    }

    public static class GuideSettings extends Modification<com.psddev.cms.tool.CmsTool> {
        @ToolUi.Note("If true, automatically generate production guide entries for all templates")
        private boolean autoGenerateTemplateGuides = true;

        @ToolUi.Note("If true, automatically generate production guide entries for all content types referenced in a template")
        private boolean autoGenerateContentTypeGuides = true;

        public boolean isAutoGenerateTemplateGuides() {
            return autoGenerateTemplateGuides;
        }

        public void setAutoGenerateTemplateGuides(boolean autoGenerateTemplateGuides) {
            this.autoGenerateTemplateGuides = autoGenerateTemplateGuides;
        }

        public boolean isAutoGenerateContentTypeGuides() {
            return autoGenerateContentTypeGuides;
        }

        public void setAutoGenerateContentTypeGuides(
                boolean autoGenerateContentTypeGuides) {
            this.autoGenerateContentTypeGuides = autoGenerateContentTypeGuides;
        }

    }

    /** Static utility methods. */
    public static final class Static {

        private Static() {
        }

        /**
         * Get the Production Guide for the given content type {@code content}
         */
        public static GuidePage getPageProductionGuide(ObjectType contentType) {
            if (contentType != null) {
                GuidePage guide = Query.from(GuidePage.class)
                        .where("pageTypes = ?", contentType).first();
                if (guide != null) {
                    return guide;
                }
            }
            return null;

        }


        /**
         * Return all pages/templates that the given section appears on,
         * excluding the provided (usually current) page.
         */
        public static List<GuidePage> getSectionReferences(GuideSection section, GuidePage page) {
            // TODO: New way to identify related templates
            return null;

        }


        /**
         * Return the related templates (other than the one provided) that have
         * a sample page defined
         */
        public static List<GuidePage> getRelatedPages(Object content,
                GuidePage ignorePage) {
            // TODO: New way to identify related templates
            return null;
        }

        /**
         * Return a map of section ids to names where names are a concatenation
         * of the section name along with it's parental lineage in the layout
         * TODO: New way to establish parental lineage of sections
         */
        public static HashMap<UUID, String> getSectionNameMap(
                Iterable<GuideSection> sections) {
            HashMap<UUID, String> nameMap = new HashMap<UUID, String>();

            if (sections == null) {
                return nameMap;
            }
            for (GuideSection section : sections) {
                String sectionName = "";
                if (!nameMap.containsKey(section.getId())) {
                    if (section.getSectionName() != null && !section.getSectionName().isEmpty()) {
                        sectionName = section.getSectionName();
                    } else {
                        sectionName = "Unnamed";
                    }
                    nameMap.put(section.getId(), sectionName);
                } else {
                    sectionName = nameMap.get(section.getId());
                }

            }
            return nameMap;
        }

    }
}
