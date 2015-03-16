package com.psddev.cms.db;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;

@Recordable.FieldInternalNamePrefix("responseCache.")
public class ResponseCacheTypeModification extends Modification<ObjectType> {
    private Integer timeout;

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getTimeout() {
        return timeout;
    }
}
