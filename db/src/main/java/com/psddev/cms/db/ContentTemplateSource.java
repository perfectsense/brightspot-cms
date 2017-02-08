package com.psddev.cms.db;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Recordable;

@Recordable.FieldInternalNamePrefix("cms.contentTemplate.")
public class ContentTemplateSource extends Modification<Object> {

    private ContentTemplate source;

    public ContentTemplate getSource() {
        return source;
    }

    public void setSource(ContentTemplate source) {
        this.source = source;
    }
}
