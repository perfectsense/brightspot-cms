package com.psddev.cms.tool.search;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import com.psddev.cms.db.Site;
import com.psddev.cms.db.Taxon;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultItem;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ObjectUtils;

public class TaxonomySearchResultView extends AbstractSearchResultView {

    private static final String PARENT_ID_PARAMETER = "taxonomyParentId";

    @Override
    public String getIconName() {
        return "folder-open";
    }

    @Override
    public String getDisplayName() {
        return "Taxonomy";
    }

    @Override
    public boolean isSupported(Search search) {
        ObjectType selectedType = search.getSelectedType();

        if (selectedType != null
                && selectedType.getGroups().contains(Taxon.class.getName())
                && ObjectUtils.isBlank(search.getQueryString())
                && search.getVisibilities().isEmpty()) {

            @SuppressWarnings("unchecked")
            Class<? extends Taxon> taxonClass = (Class<? extends Taxon>) search.getSelectedType().getObjectClass();

            if (!Taxon.Static.getRoots(taxonClass).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isPreferred(Search search) {
        return isSupported(search);
    }

    private Taxon findParent(ToolPageContext page) {
        return Query
                .from(Taxon.class)
                .where("_id = ?", page.param(UUID.class, PARENT_ID_PARAMETER))
                .first();
    }

    @Override
    public boolean isHtmlWrapped(
            Search search,
            ToolPageContext page,
            SearchResultItem itemWriter) {

        return findParent(page) == null;
    }

    @Override
    protected void doWriteHtml() throws IOException {
        search.setSuggestions(false);

        Collection<? extends Taxon> items;
        Site site = page.getSite();
        Predicate predicate = search.toQuery(site).getPredicate();
        Taxon parent = findParent(page);

        if (parent == null) {
            @SuppressWarnings("unchecked")
            Class<? extends Taxon> taxonClass = (Class<? extends Taxon>) search.getSelectedType().getObjectClass();
            items = Taxon.Static.getRoots(taxonClass, site, predicate);

        } else {
            items = Taxon.Static.getChildren(parent, predicate);

            if (site != null && !items.isEmpty()) {
                for (Iterator<? extends Taxon> i = items.iterator(); i.hasNext();) {
                    Taxon taxon = i.next();

                    if (!PredicateParser.Static.evaluate(taxon, site.itemsPredicate())) {
                        i.remove();
                    }
                }
            }
        }

        if (!items.isEmpty()) {
            String target = page.createId();

            page.writeStart("div", "class", "searchResultList");

                if (parent == null) {
                    page.writeStart("div", "class", "taxonomyContainer");
                    page.writeStart("div", "class", "searchTaxonomy");
                }

                        page.writeStart("ul", "class", "taxonomy");
                            for (Taxon item : items) {
                                page.writeStart("li");
                                    if (item.as(Taxon.Data.class).isSelectable()) {
                                        itemWriter.writeBeforeHtml(page, search, item);
                                    }

                                    String altLabel = item.as(Taxon.Data.class).getAltLabel();

                                    if (ObjectUtils.isBlank(altLabel)) {
                                        page.writeObjectLabel(item);

                                    } else {
                                        String visibilityLabel = item.getState().getVisibilityLabel();

                                        if (!ObjectUtils.isBlank(visibilityLabel)) {
                                            page.writeStart("span", "class", "visibilityLabel");
                                                page.writeHtml(visibilityLabel);
                                            page.writeEnd();

                                            page.writeHtml(" ");
                                        }
                                        page.writeHtml(altLabel);
                                    }

                                    if (item.as(Taxon.Data.class).isSelectable()) {
                                        itemWriter.writeAfterHtml(page, search, item);
                                    }

                                    Collection<? extends Taxon> children = Taxon.Static.getChildren(item, predicate);

                                    if (children != null && !children.isEmpty()) {
                                        page.writeStart("a",
                                                "class", "taxonomyExpand",
                                                "target", target,
                                                "href", page.url("", PARENT_ID_PARAMETER, item.getState().getId()));
                                        page.writeEnd();
                                    }
                                page.writeEnd();
                            }
                        page.writeEnd();

                        page.writeStart("div",
                                "class", "frame taxonChildren",
                                "name", target);
                        page.writeEnd();

                if (parent == null) {
                    page.writeEnd();
                    page.writeEnd();
                }

            page.writeEnd();
        }
    }
}
