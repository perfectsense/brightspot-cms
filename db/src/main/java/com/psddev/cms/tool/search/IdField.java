package com.psddev.cms.tool.search;

import com.psddev.cms.tool.SearchResultField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.State;

public class IdField implements SearchResultField {

    @Override
    public String getDisplayName() {
        return "ID";
    }

    @Override
    public boolean isSupported(ObjectType type) {
        return true;
    }

    @Override
    public String createDataCellText(Object item) {
        return State.getInstance(item).getId().toString();
    }
}
