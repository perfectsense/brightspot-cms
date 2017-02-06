package com.psddev.cms.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectFieldComparator;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PageContextFilter;

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

            Optional.ofNullable(PageContextFilter.Static.getRequestOrNull())
                    .ifPresent(request -> query.and(UserPermissionsProvider.allItemsPredicate(AuthenticationFilter.Static.getUser(request))));

            if (site != null) {
                query.and(site.itemsPredicate());
            }

            List<T> roots = query.selectAll();
            filter(roots, predicate);
            sort(roots);

            return roots;
        }

        public static <T extends Taxon> List<? extends Taxon> getChildren(T taxon, Predicate predicate) {
            List<Taxon> children = new ArrayList<Taxon>();

            if (taxon != null) {
                children.addAll(taxon.getChildren());
            }

            filter(children, predicate);
            sort(children);

            return children;
        }

        private static <T extends Taxon> void sort(List<T> taxa) {
            if (taxa != null && !taxa.isEmpty()) {
                ObjectType taxonType = taxa.get(0).getState().getType();
                ToolUi ui = taxonType.as(ToolUi.class);
                if (!ObjectUtils.isBlank(ui.getDefaultSortField())) {
                    Collections.sort(taxa, new ObjectFieldComparator(ui.getDefaultSortField(), true));
                }
            }
        }

        /*
         * This applies the given predicate (usually @Where) to the list.
         * It completely filters out items that do not match the predicate AND do not have any children that do match the predicate.
         * If any items in the list have children that should not be filtered out, they are included with Taxon.Data#isSelectable = false.
         * SearchResultRenderer uses this flag to make the parent navigable but not selectable.
         */
        private static <T extends Taxon> void filter(List<T> taxa, Predicate predicate) {
            if (taxa != null && predicate != null) {
                for (T t : new ArrayList<T>(taxa)) {
                    if (!PredicateParser.Static.evaluate(t, predicate)) {
                        List<? extends Taxon> children = getChildren(t, null);
                        if (children.isEmpty()) {
                            taxa.remove(t);
                        } else {
                            filter(children, predicate);
                            if (children.isEmpty()) {
                                taxa.remove(t);
                            } else {
                                t.as(Taxon.Data.class).setSelectable(false);
                            }
                        }
                    }
                }
            }
        }

    }

}
