package com.psddev.cms.tool;

import com.google.common.base.Preconditions;
import com.psddev.cms.db.ContentTemplate;
import com.psddev.cms.db.Localization;
import com.psddev.dari.db.ObjectType;

import java.util.UUID;

public class ObjectTypeOrContentTemplate implements Comparable<ObjectTypeOrContentTemplate> {

    private final ObjectType type;
    private final ContentTemplate template;

    public ObjectTypeOrContentTemplate(ObjectType type) {
        Preconditions.checkNotNull(type);

        this.type = type;
        this.template = null;
    }

    public ObjectTypeOrContentTemplate(ContentTemplate template) {
        Preconditions.checkNotNull(template);

        this.type = template.getTemplateType();
        this.template = template;
    }

    public ObjectType getType() {
        return type;
    }

    public ContentTemplate getTemplate() {
        return template;
    }

    public UUID getId() {
        return template != null ? template.getId() : type.getId();
    }

    public String getLabel() {
        return template != null
                ? template.getName()
                : Localization.currentUserText(type, "displayName");
    }

    @Override
    public int compareTo(ObjectTypeOrContentTemplate other) {
        return getLabel().compareTo(other.getLabel());
    }
}
