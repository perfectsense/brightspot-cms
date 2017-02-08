package com.psddev.cms.db;

import java.util.ArrayList;
import java.util.List;

import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.StringUtils;

public class ContentType extends Record implements Global, Managed {

    @Indexed
    @Required
    private String displayName;

    @Indexed(unique = true)
    @Required
    private String internalName;

    private List<ContentField> fields;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    /**
     * @return Never {@code null}.
     */
    public List<ContentField> getFields() {
        if (fields == null) {
            fields = new ArrayList<ContentField>();
        }
        return fields;
    }

    /**
     * @param fields {@code null} to reset the list.
     */
    public void setFields(List<ContentField> fields) {
        this.fields = fields;
    }

    @Override
    public String createManagedEditUrl(ToolPageContext page) {
        String internalName = getInternalName();

        if (!StringUtils.isBlank(internalName)) {
            ObjectType type = Database.Static.getDefault().getEnvironment().getTypes().stream()
                    .filter(t -> internalName.equals(t.getInternalName()))
                    .findFirst()
                    .orElse(null);

            if (type != null) {
                return page.cmsUrl(
                        "/adminContentTypes",
                        "typeId", type.getId(),
                        "id", getId());
            }
        }

        return null;
    }
}
