package com.psddev.cms.tool;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.psddev.cms.db.ToolEntity;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;

public class SearchResultSelection extends Record {

    @Indexed
    private Set<ToolEntity> entities;

    public Set<ToolEntity> getEntities() {
        if (entities == null) {
            entities = new LinkedHashSet<>();
        }
        return entities;
    }

    public void setEntities(Set<ToolEntity> entities) {
        this.entities = entities;
    }

    public Query<Object> createItemsQuery() {

        Set<UUID> itemIds = new HashSet<>();

        for (SearchResultSelectionItem item : Query.
                from(SearchResultSelectionItem.class).
                where("selectionId = ?", getId()).
                selectAll()) {

            itemIds.add(item.getItemId());
        }

        return Query.fromAll().where("_id = ?", itemIds);
    }
}
