package com.psddev.cms.db;

import java.util.Map;

import com.psddev.dari.db.Modification;
import com.psddev.dari.util.CompactMap;

@AbVariationObject.FieldInternalNamePrefix("cms.ab.")
public class AbVariationObject extends Modification<Object> {

    @ToolUi.Hidden
    private Map<String, AbVariationField> fields;

    public Map<String, AbVariationField> getFields() {
        if (fields == null) {
            fields = new CompactMap<String, AbVariationField>();
        }
        return fields;
    }

    public void setFields(Map<String, AbVariationField> fields) {
        this.fields = fields;
    }

    @Override
    protected void beforeSave() {

        if (getFields().isEmpty()) {
            setFields(null);
        }
    }
}
