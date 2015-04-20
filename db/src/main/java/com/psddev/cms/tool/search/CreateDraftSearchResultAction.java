package com.psddev.cms.tool.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.psddev.cms.db.Draft;
import com.psddev.cms.db.ToolUi;
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

        Query<SearchResultSelectionItem> itemsQuery = Query.
                from(SearchResultSelectionItem.class).
                where("selectionId = ?", selection.getId());

        if (!itemsQuery.hasMoreThan(100)) {
            Set<UUID> itemIds = new HashSet<>();

            for (SearchResultSelectionItem item : itemsQuery.selectAll()) {
                itemIds.add(item.getItemId());
            }

            Set<ObjectType> itemTypes = new HashSet<>();

            for (Object item : Query.
                    fromAll().
                    where("_id = ?", itemIds).
                    referenceOnly().
                    selectAll()) {

                itemTypes.add(State.getInstance(item).getType());
            }

            boolean multiple = itemIds.size() != 1;
            List<TypeAndField> creates = new ArrayList<>();
            List<TypeAndItemTypes> generates = new ArrayList<>();

            for (ObjectType type : Database.Static.getDefault().getEnvironment().getTypes()) {
                if (!type.isConcrete() ||
                        !page.hasPermission("type/" + type.getId() + "/write") ||
                        (!page.getCmsTool().isDisplayTypesNotAssociatedWithJavaClasses() &&
                        type.getObjectClass() == null) ||
                        Draft.class.equals(type.getObjectClass()) ||
                        (type.isDeprecated() &&
                        !Query.fromType(type).hasMoreThan(0))) {

                    continue;
                }

                if (type.getObjectClass() != null &&
                        type.getGroups().contains(SearchResultSelectionGeneratable.class.getName()) &&
                        type.as(SearchResultSelectionGeneratable.TypeData.class).getItemTypes().containsAll(itemTypes)) {

                    generates.add(new TypeAndItemTypes(type, new ArrayList<ObjectType>(itemTypes)));
                }

                for (ObjectField field : type.getFields()) {
                    if (field.as(ToolUi.class).isHidden() ||
                            multiple ^ field.isInternalCollectionType()) {

                        continue;
                    }

                    Set<ObjectType> fieldTypes = new HashSet<>();

                    for (ObjectType fieldType : field.getTypes()) {
                        fieldTypes.addAll(fieldType.findConcreteTypes());
                    }

                    if (fieldTypes.containsAll(itemTypes)) {
                        creates.add(new TypeAndField(type, field));
                    }
                }
            }

            if (!creates.isEmpty() || !generates.isEmpty()) {
                Collections.sort(creates);
                Collections.sort(generates);

                page.writeStart("div", "class", "searchResult-action-createContent");
                    page.writeStart("h2");
                        page.writeHtml("Create");
                    page.writeEnd();

                    page.writeStart("form",
                            "method", "post",
                            "target", "_top",
                            "action", page.toolUrl(CmsTool.class, CreateDraft.PATH));

                        page.writeElement("input",
                                "type", "hidden",
                                "name", "selectionId",
                                "value", selection.getId());

                        page.writeStart("select", "name", "typeIdAndField");
                            for (TypeAndField create : creates) {
                                page.writeStart("option",
                                        "value", create.type.getId() + "," + create.field.getInternalName());

                                    page.writeObjectLabel(create.type);
                                    page.writeHtml(" (");
                                    page.writeObjectLabel(create.field);
                                    page.writeHtml(')');
                                page.writeEnd();
                            }

                            for (TypeAndItemTypes generate : generates) {
                                page.writeStart("option",
                                        "value", generate.type.getId());

                                page.writeObjectLabel(generate.type);
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
                                page.writeEnd();
                            }
                        page.writeEnd();

                        page.writeStart("button");
                            page.writeHtml("New");
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();
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
