package com.psddev.cms.tool;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.psddev.cms.db.ToolEntity;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;

public class SearchResultSelection extends Record {

    private String name;

    @Indexed
    @ToolUi.Hidden
    private Set<ToolEntity> entities;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<ToolEntity> getEntities() {
        if (entities == null) {
            entities = new LinkedHashSet<>();
        }
        return entities;
    }

    public void setEntities(Set<ToolEntity> entities) {
        this.entities = entities;
    }

    /**
     * Generates a {@link Query} for the items contained within this {@link SearchResultSelection}.  The returned
     * Query is {@code .fromAll()} and includes visibility-restricted items.
     * @return a {@link Query} for the items contained within this {@link SearchResultSelection}.
     */
    public Query<Object> createItemsQuery() {

        Set<UUID> itemIds = new HashSet<>();

        for (SearchResultSelectionItem item : Query
                .from(SearchResultSelectionItem.class)
                .where("selectionId = ?", getId())
                .selectAll()) {

            itemIds.add(item.getItemId());
        }

        return Query.fromAll().where("_id = ?", itemIds);
    }

    /**
     * Clear the SearchResultSelection by deleting all of the SearchResultSelectionItem that point to it.
     */
    public void clear() {
        Query.from(SearchResultSelectionItem.class)
                .where("selectionId = ?", getId())
                .deleteAll();
    }

    /**
     * Adds the Object with itemId to this SearchResultSelection.
     * @param itemId the id of the Object to be added.  Cannot be {@code null}.
     */
    public boolean addItem(UUID itemId) {

        if (itemId == null) {
            throw new IllegalArgumentException("itemId cannot be null!");
        }

        SearchResultSelectionItem item = Query
                .from(SearchResultSelectionItem.class)
                .where("selectionId = ?", getId())
                .and("itemId = ?", itemId)
                .first();

        if (item == null) {

            item = new SearchResultSelectionItem();

            item.setSelectionId(getId());
            item.setItemId(itemId);
            item.save();
            return true;
        }

        return false;
    }

    /**
     * Removes the Object with itemId from this SearchResultSelection.
     * @param itemId the id of the Object to be removed.  Cannot be {@code null}.
     */
    public boolean removeItem(UUID itemId) {

        if (itemId == null) {
            throw new IllegalArgumentException("itemId cannot be null!");
        }

        SearchResultSelectionItem item = Query
                .from(SearchResultSelectionItem.class)
                .where("selectionId = ?", getId())
                .and("itemId = ?", itemId).first();

        if (item == null) {
            return false;
        }

        item.delete();
        return true;
    }

    /**
     * Calculates the size of the SearchResultSelection by counting its items {@link Query}.
     * @return the size of the SearchResultSelection.
     */
    public int size() {
        return Long.valueOf(createItemsQuery().count()).intValue();
    }

    /**
     * Returns SearchResultSelections that were created by the specified {@link ToolUser}.
     * @param user the {@link ToolUser} for which SearchResultSelections should be returned.
     * @return {@link SearchResultSelection}s that were created by the specified {@link ToolUser}.
     */
    public static List<SearchResultSelection> findOwnSelections(ToolUser user) {

        if (user == null) {
            return null;
        }

        return Query.from(SearchResultSelection.class).where("entities = ?", user).selectAll();
    }

    /**
     * Returns SearchResultSelections that are accessible to the specified {@link ToolUser}, optionally excluding selections
     * that were created by the user.
     * @param user the {@link ToolUser} for which SearchResultSelections should be returned.
     * @param excludeOwn excludes selections created by the specified {@link ToolUser} if true.
     * @return accessible {@link SearchResultSelection}s for the specified {@link ToolUser}.
     */
    public static List<SearchResultSelection> findAccessibleSelections(ToolUser user, boolean excludeOwn) {

        if (user == null) {
            return null;
        }

        Query<SearchResultSelection> query = Query.from(SearchResultSelection.class);

        if (user.getRole() == null) {
            query.where("entities != missing");
        } else {
            query.where("entities = ?", user.getRole());
        }

        if (excludeOwn) {
            query.and("entities != ?", user);
        } else {
            query.or("entities = ?", user);
        }

        return query.selectAll();
    }

    /**
     * Delete all {@link SearchResultSelectionItem}s linking to this {@link SearchResultSelection}.
     */
    @Override
    protected void afterDelete() {

        Query.from(SearchResultSelectionItem.class).where("selectionId = ?", getId()).deleteAll();
    }
}
