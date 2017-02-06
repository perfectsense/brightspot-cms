package com.psddev.cms.db;

import com.psddev.dari.db.CompoundPredicate;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.TypeDefinition;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This interface enables defining logic around what objects a {@link ToolUser}
 * can access.
 */
public interface UserPermissionsProvider {

    /**
     * Returns a {@link Predicate} that filters out any objects that are not
     * accessible by the provided {@code user}.
     *
     * @param user Nonnull.
     * @return Nullable.
     */
    Predicate itemsPredicate(ToolUser user);

    /**
     * Returns a {@link Predicate} that compounds all predicates from
     * implementations of {@link UserPermissionsProvider}.
     *
     * @param user Nullable.
     * @return Nullable.
     */
    static Predicate allItemsPredicate(ToolUser user) {
        if (user == null) {
            return null;
        }

        List<Predicate> predicates = ClassFinder.findConcreteClasses(UserPermissionsProvider.class).stream()
                .map(clazz -> TypeDefinition.getInstance(clazz).newInstance())
                .map(object -> object.itemsPredicate(user))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return predicates.isEmpty() ? null : new CompoundPredicate(PredicateParser.OR_OPERATOR, predicates);
    }

    /**
     * @return {@code true} if the {@code object} is accessible by the
     * given {@code user}, {@code false}.
     */
    static boolean isObjectAccessible(ToolUser user, Object object) {
        return PredicateParser.Static.evaluate(object, allItemsPredicate(user));
    }
}
