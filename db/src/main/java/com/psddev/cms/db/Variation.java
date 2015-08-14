package com.psddev.cms.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.psddev.dari.db.Database;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.PeriodicValue;
import com.psddev.dari.util.PullThroughValue;

// CHECKSTYLE:OFF
/**
 * Changes objects based on rules.
 *
 * <p>Some of the uses of this class are: A/B testing, internationalization,
 * and delivery of a different layout for mobile.
 *
 * @deprecated No replacement.
 */
@Deprecated
public class Variation extends Record {

    public static final String APPLIED_EXTRA = "cms.variation.applied";

    private static final Logger LOGGER = LoggerFactory.getLogger(Variation.class);

    @Indexed(unique = true)
    @Required
    private String name;

    @Indexed(unique = true)
    @Required
    @ToolUi.Hidden
    private double position;

    @ToolUi.Note("Leave blank to allow on all content types.")
    private Set<ObjectType> contentTypes;

    @Required
    private Rule rule;

    @Required
    private Operation operation;

    /** Returns the name. Displayed in the tool UI. */
    public String getName() {
        return this.name;
    }

    /** Sets the name. */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the position. Determines the order during {@link #applyAll}. */
    public double getPosition() {
        return this.position;
    }

    /** Sets the position. */
    public void setPosition(double position) {
        this.position = position;
    }

    public Set<ObjectType> getContentTypes() {
        if (contentTypes == null) {
            contentTypes = new HashSet<ObjectType>();
        }
        return contentTypes;
    }

    public void setContentTypes(Set<ObjectType> contentTypes) {
        this.contentTypes = contentTypes;
    }

    /** Returns the rule. */
    public Rule getRule() {
        return this.rule;
    }

    /** Sets the rule. */
    public void setRule(Rule rule) {
        this.rule = rule;
    }

    /** Returns the operation. */
    public Operation getOperation() {
        return this.operation;
    }

    /** Sets the operation. */
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    /**
     * @deprecated No replacement.
     */
    @Deprecated
    public static class Data extends Modification<Object> {

        private Map<String, Object> variations;

        @InternalName("cms.variation.initialVariation")
        private Variation initialVariation;

        public Map<String, Object> getVariations() {
            if (variations == null) {
                variations = new HashMap<String, Object>();
            }
            return variations;
        }

        public void setVariations(Map<String, Object> variations) {
            this.variations = variations;
        }

        public Variation getInitialVariation() {
            return initialVariation;
        }

        public void setInitialVariation(Variation initialVariation) {
            this.initialVariation = initialVariation;
        }
    }

    /**
     * {@link Variation} utility methods.
     *
     * @deprecated No replacement.
     */
    @Deprecated
    public static final class Static {

        private Static() {
        }

        private static final PullThroughValue<PeriodicValue<List<Variation>>>
                ALL = new PullThroughValue<PeriodicValue<List<Variation>>>() {

            @Override
            protected PeriodicValue<List<Variation>> produce() {
                return new PeriodicValue<List<Variation>>() {

                    @Override
                    protected List<Variation> update() {
                        Query<Variation> query = Query.from(Variation.class).sortAscending("position").using(Database.Static.getDefaultOriginal());
                        Date cacheUpdate = getUpdateDate();
                        Date databaseUpdate = query.lastUpdate();

                        if (databaseUpdate == null || (cacheUpdate != null && !databaseUpdate.after(cacheUpdate))) {
                            List<Variation> variations = get();
                            return variations != null ? variations : Collections.<Variation>emptyList();
                        }

                        LOGGER.info("Loading variations");
                        return query.selectAll();
                    }
                };
            }
        };

        /**
         * Applies all variations to the given {@code object} using the
         * given {@code profile}.
         *
         * @throws IllegalArgumentException If the given {@code object}
         *         or {@code profile} is {@code null}.
         */
        public static void applyAll(Object object, Profile profile) {
            ErrorUtils.errorIfNull(object, "object");
            ErrorUtils.errorIfNull(profile, "profile");

            List<Variation> applied = getApplied(object);

            for (Variation variation : ALL.get().get()) {
                try {
                    if (!applied.contains(variation) &&
                            variation.getRule().evaluate(variation, profile, object)) {
                        applied.add(variation);
                        variation.getOperation().evaluate(variation, profile, object);
                    }

                } catch (Throwable error) {
                    LOGGER.warn(String.format(
                            "Can't apply variation [%s] to [%s]!",
                            variation.getId(),
                            State.getInstance(object).getId()),
                            error);
                }
            }
        }

        /**
         * Returns the list of variations that have been applied to
         * the given {@code object} so far.
         *
         * @throws IllegalArgumentException If the given {@code object}
         *         is {@code null}.
         */
        public static List<Variation> getApplied(Object object) {
            ErrorUtils.errorIfNull(object, "object");

            Map<String, Object> extras = State.getInstance(object).getExtras();
            @SuppressWarnings("unchecked")
            List<Variation> applied = (List<Variation>) extras.get(APPLIED_EXTRA);

            if (applied == null) {
                applied = new ArrayList<Variation>();
                extras.put(APPLIED_EXTRA, applied);
            }

            return applied;
        }

        /**
         * Returns a list of variations that can be applied to the instances
         * of the given {@code type}.
         *
         * @return Never {@code null}.
         */
        public static List<Variation> getApplicable(ObjectType type) {
            List<Variation> applicable = new ArrayList<Variation>();

            for (Variation variation : ALL.get().get()) {
                Set<ObjectType> types = variation.getContentTypes();
                if (types.isEmpty() || types.contains(type)) {
                    applicable.add(variation);
                }
            }

            return applicable;
        }
    }

    // --- Deprecated ---

    /** @deprecated Use {@link Static#applyAll} instead. */
    @Deprecated
    public static void applyAll(Object object, Profile profile) {
        Static.applyAll(object, profile);
    }

    /** @deprecated Use {@link Static#getApplied} instead. */
    @Deprecated
    public static List<Variation> getApplied(Object object) {
        return Static.getApplied(object);
    }
}
