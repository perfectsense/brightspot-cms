package com.psddev.cms.tool.content;

import com.psddev.cms.db.Template;
import com.psddev.cms.tool.ContentEditSection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.UpdatingContentEditWidget;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.State;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * @deprecated No replacement
 */
@Deprecated
@SuppressWarnings("deprecation")
public class TemplateWidget extends UpdatingContentEditWidget {

    @Override
    public ContentEditSection getSection(ToolPageContext page, Object content) {
        return ContentEditSection.RIGHT;
    }

    @Override
    public String getHeading(ToolPageContext page, Object content) {
        return "Template";
    }

    @Override
    public void displayOrUpdate(ToolPageContext page, Object content, ContentEditSection section) throws IOException {
        Object original = page.getRequest().getAttribute("original");

        if (original == null) {
            original = content;
        }

        State objectState = State.getInstance(original);
        Template.ObjectModification objectTemplateMod = objectState.as(Template.ObjectModification.class);

        if (!Template.Static.findUsedTypes(page.getSite()).contains(objectState.getType())) {
            return;
        }

        UUID objectId = objectState.getId();
        String namePrefix = objectId + "/template/";
        String defaultName = objectId + "default";

        if (section == null) {
            objectTemplateMod.setDefault(Database.Static.findById(objectState.getDatabase(), Template.class, page.param(UUID.class, defaultName)));
            return;
        }

        List<Template> usableTemplates = Template.Static.findUsable(original);

        if (usableTemplates.isEmpty()) {
            return;
        }

        Template objectTemplate = objectTemplateMod.getDefault();

        if (objectTemplate == null && usableTemplates.size() == 1) {
            objectTemplate = usableTemplates.get(0);
        }

        page.writeStart("select", "name", defaultName, "style", "width: 100%;");
        {
            page.writeStart("option");
            page.writeHtml("- AUTOMATIC -");
            page.writeEnd();

            for (Template template : usableTemplates) {
                page.writeStart("option",
                        "selected", template.equals(objectTemplate) ? "selected" : null,
                        "value", template.getId());
                page.writeObjectLabel(template);
                page.writeEnd();
            }
        }
        page.writeEnd();
    }
}
