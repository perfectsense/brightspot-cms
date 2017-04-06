package com.psddev.cms.tool.content;

import com.google.common.base.Throwables;
import com.psddev.cms.db.Localization;
import com.psddev.cms.db.Page;
import com.psddev.cms.db.Template;
import com.psddev.cms.tool.ContentEditWidgetPlacement;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.UpdatingContentEditWidget;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.State;

import java.io.IOException;

public class SeoWidget extends UpdatingContentEditWidget {

    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldDisplay(ToolPageContext page, Object content) {
        if (!Page.class.isInstance(content)
                && Template.class.isInstance(content)
                && !Template.Static.findUsedTypes(page.getSite()).contains(State.getInstance(content).getType())) {

            return false;

        } else {
            return super.shouldDisplay(page, content);
        }
    }

    @Override
    public ContentEditWidgetPlacement getPlacement(ToolPageContext page, Object content) {
        return ContentEditWidgetPlacement.TAB;
    }

    @Override
    public String getHeading(ToolPageContext page, Object content) {
        return Localization.currentUserText(getClass(), "title");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void displayOrUpdate(ToolPageContext page, Object content, ContentEditWidgetPlacement placement) throws IOException {
        State state = State.getInstance(content);
        DatabaseEnvironment environment = state.getDatabase().getEnvironment();
        ObjectField[] seoFields = {
                environment.getField("cms.seo.title"),
                environment.getField("cms.seo.description"),
                environment.getField("cms.seo.keywords"),
                environment.getField("cms.seo.robots") };

        if (placement == null) {
            try {
                for (ObjectField seoField : seoFields) {
                    page.processField(content, seoField);
                }

                return;

            } catch (Throwable error) {
                throw Throwables.propagate(error);
            }
        }

        for (ObjectField seoField : seoFields) {
            page.renderField(content, seoField);
        }
    }
}
