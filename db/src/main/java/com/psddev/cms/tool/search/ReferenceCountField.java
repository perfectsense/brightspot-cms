package com.psddev.cms.tool.search;

import com.psddev.cms.db.Content;
import com.psddev.cms.tool.SearchResultField;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;

import java.io.IOException;

public class ReferenceCountField implements SearchResultField {

    @Override
    public String getDisplayName() {
        return "# Of References";
    }

    @Override
    public boolean isSupported(ObjectType type) {
        return Content.Static.isSearchableType(type);
    }

    @Override
    public String createDataCellText(Object item) {
        return String.valueOf(getReferencesCount(item));
    }

    @Override
    public void writeTableDataCellHtml(ToolPageContext page, Object item) throws IOException {
        page.writeStart("td");
        page.writeHtml(String.format("%,d", getReferencesCount(item)));
        page.writeEnd();
    }

    private long getReferencesCount(Object item) {

        return Query
                .fromAll()
                .where("* matches ?", State.getInstance(item).getId())
                .count();
    }
}
