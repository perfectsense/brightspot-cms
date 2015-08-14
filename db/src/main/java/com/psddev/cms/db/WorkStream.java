package com.psddev.cms.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ErrorUtils;

@SuppressWarnings("rawtypes")
@Record.BootstrapPackages(value = "Work Streams", depends = { com.psddev.cms.tool.Search.class, Query.class })
public class WorkStream extends Record {

    @Required
    private String name;

    private String instructions;

    @Embedded
    @ToolUi.Hidden
    private com.psddev.cms.tool.Search search;

    @ToolUi.Hidden
    private Query query;

    @ToolUi.Hidden
    private boolean incompleteIfMatching;

    @ToolUi.Hidden
    private Map<String, UUID> currentItems;

    @ToolUi.Hidden
    private Map<String, List<UUID>> skippedItems;

    /** Returns the name. */
    public String getName() {
        return name;
    }

    /** Sets the name. */
    public void setName(String name) {
        this.name = name;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    /** Returns the tool search that can return all items to be worked on. */
    public com.psddev.cms.tool.Search getSearch() {
        return search;
    }

    /** Sets the tool search that can return all items to be worked on. */
    public void setSearch(com.psddev.cms.tool.Search search) {
        this.search = search;
    }

    /** Returns the query that can return all items to be worked on. */
    public Query getQuery() {
        return query == null && search != null ? search.toQuery() : query;
    }

    /** Sets the query that can return all items to be worked on. */
    public void setQuery(Query query) {
        this.query = query;
    }

    /**
     * Returns {@code true} if the item should be considered incomplete as
     * long as the query matches.
     */
    public boolean isIncompleteIfMatching() {
        return incompleteIfMatching;
    }

    /**
     * Sets whether the item should be considered incomplete as long as the
     * query matches.
     */
    public void setIncompleteIfMatching(boolean incompleteIfMatching) {
        this.incompleteIfMatching = incompleteIfMatching;
    }

    /**
     * Returns all users that are currently working.
     *
     * @return Never {@code null} but may be empty.
     */
    public List<ToolUser> getUsers() {
        return currentItems != null
                ? Query.from(ToolUser.class).where("_id = ?", currentItems.keySet()).selectAll()
                : new ArrayList<ToolUser>();
    }

    /**
     * Returns the item that the given {@code user} is currently working
     * on.
     *
     * @param user Can't be {@code null}.
     * @return May be {@code null}.
     */
    public Object getCurrentItem(ToolUser user) {
        ErrorUtils.errorIfNull(user, "user");

        return currentItems != null
                ? Query.from(Object.class).where("_id = ?", currentItems.get(user.getId().toString())).first()
                : null;
    }

    /** Returns the number of remaining items to be worked on. */
    public long countIncomplete() {
        return getQuery().clone()
                .not("cms.workstream.completeIds ^= ?", getId().toString() + ",")
                .count();
    }

    /** Returns the number of items completed. */
    public long countComplete() {
        return Query.fromAll()
                .where("cms.workstream.completeIds ^= ?", getId().toString() + ",")
                .count();
    }

    /** Returns the total number of items, complete and incomplete */
    public long countTotal() {
        return countIncomplete() + countComplete();
    }

    /**
     * Returns the number of items completed by the given {@code user}.
     *
     * @param user Can't be {@code null}.
     */
    public long countComplete(ToolUser user) {
        ErrorUtils.errorIfNull(user, "user");

        return Query
                .from(Object.class)
                .where("cms.workstream.completeIds = ?", getId().toString() + "," + user.getId().toString())
                .count();
    }

    /**
     * Returns the number of items skipped by the given {@code user}.
     *
     * @param user Can't be {@code null}.
     */
    public long countSkipped(ToolUser user) {
        ErrorUtils.errorIfNull(user, "user");
        String userId = user.getId().toString();

        if (skippedItems != null && skippedItems.get(userId) != null) {
            return skippedItems.get(userId).size();
        } else {
            return 0L;
        }
    }

    /**
     * Returns {@code true} if the given {@code user} is currently working.
     *
     * @param user Can't be {@code null}.
     */
    public boolean isWorking(ToolUser user) {
        ErrorUtils.errorIfNull(user, "user");

        return currentItems != null
                ? currentItems.get(user.getId().toString()) != null
                : false;
    }

    /**
     * Returns the next item that the given {@code user} can work on.
     *
     * @param user Can't be {@code null}.
     */
    public Object next(ToolUser user) {
        ErrorUtils.errorIfNull(user, "user");

        String userId = user.getId().toString();
        Site site = user.getCurrentSite();
        Predicate siteItemsPredicate = null;

        if (site != null) {
            siteItemsPredicate = site.itemsPredicate();
        }

        State next = null;

        if (currentItems != null) {
            Query nextQuery = Query.from(Object.class)
                    .where("_id = ?", currentItems.get(userId));

            if (siteItemsPredicate != null) {
                nextQuery.and(siteItemsPredicate);
            }

            next = State.getInstance(nextQuery.first());
        }

        if (next != null
                && (next.as(Data.class).isComplete(this)
                || (skippedItems != null && skippedItems.get(userId) != null && skippedItems.get(userId).contains(next.getId())))) {
            next = null;
        }

        if (next == null) {
            Query<?> query = getQuery().clone()
                    .not("cms.workstream.completeIds ^= ?", getId().toString() + ",");

            if (siteItemsPredicate != null) {
                query.and(siteItemsPredicate);
            }

            if (currentItems != null) {
                query.and("_id != ?", currentItems.values());
            }

            if (skippedItems != null) {
                query.and("_id != ?", skippedItems.get(userId));
            }

            next = State.getInstance(query.first());

            if (next != null) {
                getState().putAtomically("currentItems/" + userId, next.getId());
                save();
            }
        }

        return next != null ? next.getOriginalObject() : null;
    }

    /**
     * Lets the given {@code user} skip working on the given {@code item}.
     *
     * @param user Can't be {@code null}.
     */
    public void skip(ToolUser user, Object item) {
        ErrorUtils.errorIfNull(user, "user");

        getState().addAtomically("skippedItems/" + user.getId().toString(), State.getInstance(item).getId());
        save();
    }

    /**
     * Lets the given {@code user} stop working.
     *
     * @param user Can't be {@code null}.
     */
    public void stop(ToolUser user) {
        ErrorUtils.errorIfNull(user, "user");

        String userId = user.getId().toString();

        getState().putAtomically("currentItems/" + userId, null);
        getState().putAtomically("skippedItems/" + userId, null);
        save();
    }

    @FieldInternalNamePrefix("cms.workstream.")
    public static class Data extends Modification<Object> {

        @Indexed
        @ToolUi.Hidden
        private Set<String> completeIds;

        /**
         * Marks this object complete in the given {@code workStream} by the
         * given {@code user}.
         *
         * @param workStream Can't be {@code null}.
         * @param user Can't be {@code null}.
         */
        public void complete(WorkStream workStream, ToolUser user) {
            ErrorUtils.errorIfNull(workStream, "workStream");
            ErrorUtils.errorIfNull(user, "user");

            if (completeIds == null) {
                completeIds = new HashSet<String>();
            }

            completeIds.add(workStream.getId().toString() + "," + user.getId().toString());
        }

        /**
         * Returns {@code true} if this object is marked complete in the given
         * {@code workStream}.
         *
         * @param workStream Can't be {@code null}.
         */
        public boolean isComplete(WorkStream workStream) {
            ErrorUtils.errorIfNull(workStream, "workStream");

            if (completeIds != null) {
                String workStreamId = workStream.getId().toString() + ",";

                for (String completeId : completeIds) {
                    if (completeId.startsWith(workStreamId)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
