package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.SearchResultField;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.TypeDefinition;

@RoutingFilter.Path(application = "cms", value = "/searchResultFields")
public class SearchResultFields extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        ObjectType type = ObjectType.getInstance(page.param(UUID.class, "typeId"));
        ToolUser user = page.getUser();
        Map<String, List<String>> fieldNamesByTypeId = user.getSearchResultFieldsByTypeId();
        String typeId = type != null ? type.getId().toString() : "";
        List<String> fieldNames = fieldNamesByTypeId.get(typeId);

        if (page.isFormPost()) {
            List<String> fieldNameParams = page.params(String.class, "fieldNames");
            if (!ObjectUtils.isBlank(fieldNameParams)) {
                fieldNamesByTypeId.put(typeId, fieldNameParams);

            } else {
                fieldNamesByTypeId.remove(typeId);
            }

            user.save();

            page.writeStart("div", "id", page.createId());
            page.writeEnd();

            page.writeStart("script", "type", "text/javascript");
                page.writeRaw("$('#" + page.getId() + "').popup('source').closest('.searchForm').find('.searchFiltersRest').submit();");
                page.writeRaw("$('#" + page.getId() + "').popup('close');");
            page.writeEnd();
            return;
        }

        page.writeStart("div", "class", "widget searchResultFields");
            page.writeStart("h1");
                if (type == null) {
                    page.writeHtml(page.localize(SearchResultFields.class, "title"));
                } else {
                    page.writeHtml(
                            page.localize(
                                    SearchResultFields.class,
                                    ImmutableMap.of("type", page.getObjectLabel(type)),
                                    "typeTitle"));
                }
            page.writeEnd();

            page.writeStart("form",
                    "method", "post",
                    "action", page.url(""));

                Map<Boolean, List<Object>> fieldsMap = getSearchFieldsMap(type, fieldNames);

                page.writeStart("div", "class", "searchResultFields-container");
                    page.writeStart("div", "class", "searchResultFields-hide");
                        page.writeHtml(page.localize(SearchResultFields.class, "label.hiddenFields"));
                        writeFieldItemsHtml(page, fieldsMap.get(false), false);
                    page.writeEnd();

                    page.writeStart("div",
                            "class", "searchResultFields-divider");
                    page.writeEnd();

                    page.writeStart("div", "class", "searchResultFields-display");
                        page.writeHtml(page.localize(SearchResultFields.class, "label.selectedFields"));
                        writeFieldItemsHtml(page, fieldsMap.get(true), true);
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("div", "class", "actions");
                    page.writeStart("button");
                        page.writeHtml(page.localize(SearchResultFields.class, "action.update"));
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeEnd();
    }

    private void writeFieldItemsHtml(ToolPageContext page, List<Object> fieldObjects, boolean checked) throws IOException {

        if (!checked) {
            page.writeTag("input",
                    "type", "text",
                    "placeholder", "Filter by name",
                    "class", "searchResultFields-filter");
        }

        page.writeStart("ul");
            for (Object fieldObject : fieldObjects) {
                String displayName = "";
                String internalName = "";

                if (fieldObject instanceof ObjectField) {
                    ObjectField field = (ObjectField) fieldObject;
                    displayName = field.getDisplayName();
                    internalName = field.getInternalName();
                } else if (fieldObject instanceof SearchResultField) {
                    SearchResultField field = (SearchResultField) fieldObject;
                    displayName = field.getDisplayName();
                    internalName = field.getClass().getName();
                }

                page.writeStart("li");

                    page.writeStart("label", "data-display-name", displayName);
                        page.writeTag("input",
                                "type", "checkbox",
                                "name", "fieldNames",
                                "checked", checked ? "checked" : null,
                                "value", internalName);
                        page.writeStart("span");
                            page.writeHtml(displayName);
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();
            }
        page.writeEnd();
    }

    private Map<Boolean, List<Object>> getSearchFieldsMap(ObjectType type,
                                                          List<String> fieldsNames) {

        List<Object> hiddenFields = new ArrayList<>();
        List<Object> displayFields = new ArrayList<>();

        if (fieldsNames == null) {
            fieldsNames = new ArrayList<>();
        }

        if (type != null) {
            for (ObjectField field : type.getFields()) {
                String fieldName = field.getInternalName();

                if (fieldsNames.contains(fieldName)) {
                    displayFields.add(field);
                } else {
                    hiddenFields.add(field);
                }
            }
        }

        for (Class<? extends SearchResultField> f : ClassFinder.findConcreteClasses(SearchResultField.class)) {
            SearchResultField field = TypeDefinition.getInstance(f).newInstance();

            if (field.isSupported(type)) {
                String fieldName = f.getName();

                if (fieldsNames.contains(fieldName) || field.isDefault(type)) {
                    displayFields.add(field);
                } else {
                    hiddenFields.add(field);
                }
            }
        }

        for (ObjectField field : Database.Static.getDefault().getEnvironment().getFields()) {

            if (fieldsNames.contains(field.getInternalName())) {
                displayFields.add(field);
            } else {
                hiddenFields.add(field);
            }
        }

        // This field is currently required
        hiddenFields.removeIf(o -> o instanceof ObjectField && ((ObjectField) o).getInternalName().equals("cms.content.updateDate"));

        Collections.sort(hiddenFields, (f1, f2) -> {
            String fieldName1 = "";
            String fieldName2 = "";

            if (f1 instanceof ObjectField) {
                fieldName1 = ((ObjectField) f1).getDisplayName();
            } else if (f1 instanceof SearchResultField) {
                fieldName1 = ((SearchResultField) f1).getDisplayName();
            }

            if (f2 instanceof ObjectField) {
                fieldName2 = ((ObjectField) f2).getDisplayName();
            } else if (f2 instanceof SearchResultField) {
                fieldName2 = ((SearchResultField) f2).getDisplayName();
            }

            return fieldName1.compareTo(fieldName2);
        });

        // Boolean key indicates whether field has been selected
        Map<Boolean, List<Object>> mappedFields = new CompactMap<>();
        mappedFields.put(false, hiddenFields);
        mappedFields.put(true, displayFields);

        return mappedFields;
    }
}
