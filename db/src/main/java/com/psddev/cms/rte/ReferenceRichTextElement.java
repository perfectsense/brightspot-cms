package com.psddev.cms.rte;

import java.util.Map;
import java.util.UUID;

import com.psddev.cms.db.RichTextElement;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Reference;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;

/**
 * {@link RichTextElement} implementation that represents {@link Reference}
 * objects, also known as RTE enhancements.
 *
 * <p>Note that this class isn't annotated with
 * {@link com.psddev.cms.db.RichTextElement.Tag} on purpose to prevent it from
 * being displayed in the rich text editor UI. Instances of this class are
 * only created at runtime by {@link RichTextViewBuilder} when using any of its
 * {@code build} APIs that accept {@link com.psddev.dari.db.ReferentialText} as
 * a parameter.</p>
 */
public class ReferenceRichTextElement extends RichTextElement {

    public static final String TAG_NAME = "brightspot-cms-reference";
    public static final String VALUES_ATTRIBUTE = "values";

    private Reference reference;

    public Reference getReference() {
        return reference;
    }

    @Override
    public void fromAttributes(Map<String, String> attributes) {
        if (attributes == null) {
            return;
        }

        String valuesString = attributes.get(VALUES_ATTRIBUTE);

        if (valuesString == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> values = (Map<String, Object>) ObjectUtils.fromJson(valuesString);
        ObjectType type = ObjectType.getInstance(ObjectUtils.to(UUID.class, values.get("_type")));

        if (type == null) {
            return;
        }

        Object object = type.createObject(ObjectUtils.to(UUID.class, values.get("_id")));

        if (!(object instanceof Reference)) {
            return;
        }

        State state = State.getInstance(object);
        state.setValues(values);
        reference = (Reference) object;
    }

    @Override
    public Map<String, String> toAttributes() {
        Map<String, String> attributes = new CompactMap<>();

        if (reference != null) {
            attributes.put(VALUES_ATTRIBUTE, ObjectUtils.toJson(reference.getState().getSimpleValues()));
        }

        return attributes;
    }
}
