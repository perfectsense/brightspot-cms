package com.psddev.cms.tool.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.psddev.cms.db.Draft;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultAction;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.SearchResultSelectionItem;
import com.psddev.cms.tool.SearchResultSelectionGeneratable;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.page.CreateDraft;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.UrlBuilder;

public class CreateDraftSearchResultAction implements SearchResultAction {

    @Override
    public void writeHtml(
            ToolPageContext page,
            Search search,
            SearchResultSelection selection)
            throws IOException {

        if (selection == null) {
            return;
        }

        Query<SearchResultSelectionItem> itemsQuery = Query
                .from(SearchResultSelectionItem.class)
                .where("selectionId = ?", selection.getId());

        if (!itemsQuery.hasMoreThan(100)) {

            Set<ObjectType> itemTypes = new HashSet<>();

            for (Object item : selection.createItemsQuery()
                    .selectAll()) {

                itemTypes.add(State.getInstance(item).getType());
            }

            List<TypeAndItemTypes> generates = new ArrayList<>();

            for (ObjectType type : Database.Static.getDefault().getEnvironment().getTypes()) {
                if (!type.isConcrete()
                        || !page.hasPermission("type/" + type.getId() + "/write")
                        || (!page.getCmsTool().isDisplayTypesNotAssociatedWithJavaClasses()
                        && type.getObjectClass() == null)
                        || Draft.class.equals(type.getObjectClass())
                        || (type.isDeprecated()
                        && !Query.fromType(type).hasMoreThan(0))) {

                    continue;
                }

                Set<ObjectType> generatableItemTypes = new HashSet<>();

                for (ObjectType itemType : type.as(SearchResultSelectionGeneratable.TypeData.class).getItemTypes()) {
                    generatableItemTypes.addAll(itemType.findConcreteTypes());
                }

                if (type.getObjectClass() != null
                        && type.getGroups().contains(SearchResultSelectionGeneratable.class.getName())
                        && generatableItemTypes.containsAll(itemTypes)) {

                    generates.add(new TypeAndItemTypes(type, new ArrayList<>(itemTypes)));
                }
            }

            if (!generates.isEmpty()) {
                Collections.sort(generates);

                for (TypeAndItemTypes generate : generates) {

                    page.writeStart("div", "class", "searchResult-action-simple");
                    page.writeStart("a",
                            "class", "button",
                            "target", "_top",
                            "href", new UrlBuilder(page.getRequest())
                                    .absolutePath(page.toolPath(CmsTool.class, CreateDraft.PATH))
                                    .currentParameters()
                                    .parameter("typeIdAndField", generate.type.getId())
                                    .parameter("selectionId", selection.getId()));
                    page.writeHtml("Create New ");
                    page.writeObjectLabel(generate.type);

                    // write out count of objects that will be passed to the SearchResultSelectionGeneratable#fromCollection method.
                    page.writeHtml(" (");

                    Iterator<ObjectType> componentTypesIt = generate.componentTypes.iterator();
                    ObjectType componentType = componentTypesIt.next();
                    page.writeObjectLabel(componentType);
                    while (componentTypesIt.hasNext()) {
                        page.write(",");
                        componentType = componentTypesIt.next();
                        page.writeObjectLabel(componentType);
                    }
                    page.writeHtml(')');

                    page.writeEnd(); // end a.button
                    page.writeEnd(); // end div.searchResult-action-simple
                }
            }
        }
    }

    private static class TypeAndItemTypes implements Comparable<TypeAndItemTypes> {

        public final ObjectType type;
        public final List<ObjectType> componentTypes;

        public TypeAndItemTypes(ObjectType type, List<ObjectType> componentTypes) {
            this.type = type;
            this.componentTypes = componentTypes;
        }

        @Override
        public int compareTo(TypeAndItemTypes other) {

            int typeCompare = type.compareTo(other.type);

            int componentSizeCompare = (componentTypes.size() > other.componentTypes.size()) ? 1 : ((componentTypes.size() < other.componentTypes.size()) ? -1 : 0);

            return typeCompare != 0 ? typeCompare : componentSizeCompare;
        }
    }

    private static class TypeAndField implements Comparable<TypeAndField> {

        public final ObjectType type;
        public final ObjectField field;

        public TypeAndField(ObjectType type, ObjectField field) {
            this.type = type;
            this.field = field;
        }

        @Override
        public int compareTo(TypeAndField other) {
            int typeCompare = type.compareTo(other.type);

            return typeCompare != 0 ? typeCompare : field.compareTo(other.field);
        }
    }
}
