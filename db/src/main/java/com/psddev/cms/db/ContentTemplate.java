package com.psddev.cms.db;

import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;

import java.util.Map;

@Recordable.BootstrapPackages("Content Templates")
public class ContentTemplate extends Record {

    @Indexed(unique = true)
    @Required
    private String name;

    @Embedded
    @Required
    private Recordable template;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Recordable getTemplate() {
        return template;
    }

    public void setTemplate(Recordable template) {
        this.template = template;
    }

    @Indexed
    @Ignored(false)
    @ToolUi.Hidden
    public ObjectType getTemplateType() {
        Recordable template = getTemplate();
        return template != null ? template.getState().getType() : null;
    }

    @Override
    public String getLabel() {
        String name = getName();
        ObjectType templateType = getTemplateType();

        if (templateType != null) {
            return name + " \u2192 " + templateType.getDisplayName();

        } else {
            return name;
        }
    }

    @Override
    protected void onValidate() {
        Recordable template = getTemplate();

        if (template != null) {
            clearAllErrors(template, true);
        }
    }

    @Override
    protected void beforeSave() {
        Recordable template = getTemplate();

        if (template != null) {
            template.as(ContentTemplateSource.class).setSource(this);
        }
    }

    private void clearAllErrors(Object object, boolean embedded) {
        if (object instanceof Recordable) {
            State state = ((Recordable) object).getState();

            if (embedded || state.isEmbedded()) {
                state.clearAllErrors();

                ObjectType type = state.getType();

                if (type != null) {
                    for (ObjectField field : type.getFields()) {
                        Object value = state.get(field.getInternalName());
                        boolean fieldEmbedded = field.isEmbedded();

                        if (value instanceof Iterable) {
                            for (Object item : (Iterable<?>) value) {
                                clearAllErrors(item, fieldEmbedded);
                            }

                        } else if (value instanceof Map) {
                            for (Object item : ((Map<?, ?>) value).values()) {
                                clearAllErrors(item, fieldEmbedded);
                            }

                        } else {
                            clearAllErrors(value, fieldEmbedded);
                        }
                    }
                }
            }
        }
    }
}
