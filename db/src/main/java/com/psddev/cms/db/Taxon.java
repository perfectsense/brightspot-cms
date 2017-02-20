package com.psddev.cms.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectFieldComparator;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.ObjectUtils;

public interface Taxon extends Recordable {

    public boolean isRoot();

    public Collection<? extends Taxon> getChildren();

    @FieldInternalNamePrefix("cms.taxon.")
    public static final class Data extends Modification<Taxon> {

        @Indexed
        @ToolUi.Hidden
        private Boolean root;

        @ToolUi.Hidden
        private Boolean childrenEmpty;

        private transient boolean selectable = true;

        @ToolUi.Hidden
        private String altLabel;

        public Boolean isRoot() {
            return Boolean.TRUE.equals(root);
        }

        public void setRoot(Boolean root) {
            this.root = root ? Boolean.TRUE : null;
        }

        public boolean isChildrenEmpty() {
            return Boolean.TRUE.equals(childrenEmpty);
        }

        public void setChildrenEmpty(boolean childrenEmpty) {
            this.childrenEmpty = childrenEmpty ? Boolean.TRUE : null;
        }

        public boolean isSelectable() {
            return selectable;
        }

        public void setSelectable(boolean selectable) {
            this.selectable = selectable;
        }

        public String getAltLabel() {
            return altLabel;
        }

        public void setAltLabel(String altLabel) {
            this.altLabel = altLabel;
        }

        public void beforeSave() {
            Taxon taxon = getOriginalObject();

            setRoot(taxon.isRoot());
            setChildrenEmpty(taxon.getChildren().isEmpty());
        }

    }

    /**
     * {@link Taxon} utility methods.
     */
    public static final class Static {

        public static <T extends Taxon> List<T> getRoots(Class<T> taxonClass) {
            return getRoots(taxonClass, null);
        }

        public static <T extends Taxon> List<T> getRoots(Class<T> taxonClass, Site site) {
            return getRoots(taxonClass, site, null);
        }

        public static <T extends Taxon> List<T> getRoots(Class<T> taxonClass, Site site, Predicate predicate) {
            Query<T> query = Query.from(taxonClass).where("cms.taxon.root = true");

            if (site != null) {
                query.and(site.itemsPredicate());
            }

            List<T> roots = query.selectAll();

            return filter(roots, predicate);
        }

        public static <T extends Taxon> boolean hasChildren(T taxon, Predicate predicate) {
            return taxon.getChildren().stream()
                    .anyMatch(childTaxon -> PredicateParser.Static.evaluate(childTaxon, predicate) || hasChildren(childTaxon, predicate));
        }

        public static <T extends Taxon> List<? extends Taxon> getChildren(T taxon, Predicate predicate) {
            if (taxon == null) {
                return Collections.emptyList();
            }

            List<Taxon> children = new ArrayList<>();
            children.addAll(taxon.getChildren());

            return filter(children, predicate);
        }

        public static <T extends Taxon> Comparator<T> getSorter(List<T> taxons) {
            if (ObjectUtils.isBlank(taxons)) {
                return (o1, o2) -> 0;
            }

            ObjectType taxonType = taxons.get(0).getState().getType();
            ToolUi ui = taxonType.as(ToolUi.class);
            if (ObjectUtils.isBlank(ui.getDefaultSortField())) {
                return (o1, o2) -> 0;
            }

            ObjectFieldComparator comparator = new ObjectFieldComparator(ui.getDefaultSortField(), true);
            return (o1, o2) -> comparator.compare(o1, o2);
        }

        public static <T extends Taxon> List<T> filter(List<T> taxons, Predicate predicate) {
            // If there's no items, we can just return the empty list
            if (taxons.isEmpty()) {
                return taxons;
            }

            // If there's nothing to filter on, just return the list, sorted
            if (predicate == null) {
                return taxons.stream()
                        .sorted(getSorter(taxons))
                        .collect(Collectors.toList());
            }

            // mark the roots that don't match the predicate as not selectable
            taxons.stream()
                    .filter(taxon -> !PredicateParser.Static.evaluate(taxon, predicate))
                    .forEach(child -> child.as(Taxon.Data.class).setSelectable(false));

            // Filter out any roots that are not selectable AND have no children that are selectable
            return taxons.stream()
                    .filter(taxon -> taxon.as(Taxon.Data.class).isSelectable() || hasChildren(taxon, predicate))
                    .sorted(getSorter(taxons))
                    .collect(Collectors.toList());
        }
    }

}
