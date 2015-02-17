package com.psddev.cms.db;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.ReferentialText;

/**
 * Production Guide class to hold information about layout sections. Instances of this class are generally not stored
 * separately, but embedded in the GuidePage objects
 *
 */
@Record.LabelFields({ "sectionName" })
public class GuideSection extends Record {

    @Required
    @ToolUi.Note("Name of the page section to be described")
    @Indexed
    private String sectionName;

    @ToolUi.Note("Production Guide description for this section")
    private ReferentialText description;

    @ToolUi.Note("Production Guide Tips for this section")
    private ReferentialText tips;

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public ReferentialText getDescription() {
        return description;
    }

    public void setDescription(ReferentialText description) {
        this.description = description;
    }

    public ReferentialText getTips() {
        return tips;
    }

    public void setTips(ReferentialText tips) {
        this.tips = tips;
    }

}
