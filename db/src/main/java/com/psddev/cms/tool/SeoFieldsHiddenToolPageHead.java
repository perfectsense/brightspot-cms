package com.psddev.cms.tool;

import com.psddev.dari.db.ObjectType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * This class writes CSS styles directly into the CMS page to hide the
 * {@link com.psddev.cms.db.Seo.ObjectModification} fields for all types implementing
 * {@link SeoFieldsHidden}.
 */
public class SeoFieldsHiddenToolPageHead implements ToolPageHead {

    private static final List<String> HIDDEN_FIELD_NAMES = Arrays.asList("cms.seo.title", "cms.seo.description", "cms.seo.keywords", "cms.seo.robots");

    @Override
    public void writeHtml(ToolPageContext page) throws IOException {

        ObjectType seoHiddenType = ObjectType.getInstance(SeoFieldsHidden.class);

        page.writeStart("style", "type", "text/css");
        {
            for (ObjectType concreteType : seoHiddenType.findConcreteTypes()) {
                for (String fieldName : HIDDEN_FIELD_NAMES) {
                    page.writeRaw(".objectInputs[data-type=\"" + concreteType.getInternalName() + "\"] [data-field-name=\"" + fieldName + "\"] { display: none; }\n");
                }
            }
        }
        page.writeEnd();
    }
}
