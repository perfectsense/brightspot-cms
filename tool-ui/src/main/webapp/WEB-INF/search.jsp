<%@ page session="false" import="

com.psddev.cms.db.Content,
com.psddev.cms.db.Template,
com.psddev.cms.db.ToolUi,
com.psddev.cms.db.ToolUser,
com.psddev.cms.db.WorkStream,
com.psddev.cms.tool.PageWriter,
com.psddev.cms.tool.Search,
com.psddev.cms.tool.Tool,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ColorImage,
com.psddev.dari.db.Database,
com.psddev.dari.db.DatabaseEnvironment,
com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectStruct,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Query,
com.psddev.dari.db.Singleton,
com.psddev.dari.db.State,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.Utf8Filter,

java.util.ArrayList,
java.util.Collections,
java.util.HashSet,
java.util.Iterator,
java.util.LinkedHashMap,
java.util.LinkedHashSet,
java.util.List,
java.util.Map,
java.util.Set,
java.util.UUID
" %><%

ToolPageContext wp = new ToolPageContext(pageContext);
PageWriter writer = wp.getWriter();
Search search = null;
String searchJson = wp.param(String.class, "search");

if (searchJson != null) {
    search = new Search();
    search.getState().setValues((Map<String, Object>) ObjectUtils.fromJson(searchJson));
}

String name = wp.param(String.class, Search.NAME_PARAMETER);

if (search == null) {
    UUID[] typeIds = (UUID[]) request.getAttribute("validTypeIds");

    if (ObjectUtils.isBlank(typeIds)) {
        Class<?> typeClass = (Class<?>) request.getAttribute("validTypeClass");

        if (typeClass != null) {
            typeIds = new UUID[] { ObjectType.getInstance(typeClass).getId() };
        }
    }

    if (typeIds != null) {
        search = new Search(wp, typeIds);

    } else {
        search = new Search(wp);
    }
}

search.setName(name);

Set<ObjectType> validTypes = search.findValidTypes();
ObjectType selectedType = search.getSelectedType();

if (validTypes.isEmpty()) {
    for (ObjectType t : Database.Static.getDefault().getEnvironment().getTypesByGroup(Content.SEARCHABLE_GROUP)) {
        validTypes.addAll(t.as(ToolUi.class).findDisplayTypes());
    }
}

String resultTarget = wp.createId();
String newJsp = (String) request.getAttribute("newJsp");
String newTarget = (String) request.getAttribute("newTarget");
boolean singleType = validTypes.size() == 1;

if (selectedType == null && singleType) {
    selectedType = validTypes.iterator().next();
}

DatabaseEnvironment environment = Database.Static.getDefault().getEnvironment();
List<ObjectType> globalFilters = new ArrayList<ObjectType>();

for (ObjectType t : environment.getTypes()) {
    if (t.as(ToolUi.class).isGlobalFilter()) {
        globalFilters.add(t);
    }
}

globalFilters.remove(selectedType);
Collections.sort(globalFilters);

Map<String, ObjectField> fieldFilters = new LinkedHashMap<String, ObjectField>();

addFieldFilters(fieldFilters, "", environment);

if (selectedType != null) {
    addFieldFilters(fieldFilters, "", selectedType);

    if (selectedType.isAbstract()) {
        Map<String, List<ObjectField>> commonFieldsByName = new LinkedHashMap<>();

        for (ObjectType t : selectedType.as(ToolUi.class).findDisplayTypes()) {
            Map<String, ObjectField> ff = new LinkedHashMap<>();

            addFieldFilters(ff, "", t);

            for (Map.Entry<String, ObjectField> entry : ff.entrySet()) {
                String n = entry.getKey();
                List<ObjectField> commonFields = commonFieldsByName.get(n);

                if (commonFields == null) {
                    commonFields = new ArrayList<>();
                    commonFieldsByName.put(n, commonFields);
                }

                commonFields.add(entry.getValue());
            }
        }

        for (Map.Entry<String, List<ObjectField>> entry : commonFieldsByName.entrySet()) {
            List<ObjectField> commonFields = entry.getValue();

            if (commonFields.size() > 1) {
                ObjectField first = commonFields.get(0);
                String declaringClassName = first.getJavaDeclaringClassName();

                if (declaringClassName != null) {
                    boolean same = true;

                    for (ObjectField f : commonFields) {
                        if (!declaringClassName.equals(f.getJavaDeclaringClassName())) {
                            same = false;
                            break;
                        }
                    }

                    if (same) {
                        fieldFilters.put(declaringClassName + "/" + entry.getKey(), first);
                    }
                }
            }
        }
    }
}

if (wp.isFormPost()) {
    String workStreamName = wp.param(String.class, "workStreamName");

    if (!ObjectUtils.isBlank(workStreamName)) {
        WorkStream workStream = new WorkStream();
        workStream.setName(workStreamName);
        workStream.setQuery(search.toQuery());
        workStream.save();

        wp.writeStart("script", "type", "text/javascript");
            wp.write("window.location = window.location;");
        wp.writeEnd();

        return;
    }
}

writer.writeStart("h1", "class", "icon icon-action-search");
    writer.writeHtml("Search");
writer.writeEnd();

ToolUser user = wp.getUser();
Map<String, String> savedSearches = user.getSavedSearches();

if (!savedSearches.isEmpty()) {
    wp.writeStart("div", "class", "widget-contentCreate searchSaved");
        List<String> savedSearchNames = new ArrayList<String>(savedSearches.keySet());

        Collections.sort(savedSearchNames, String.CASE_INSENSITIVE_ORDER);

        wp.writeStart("div", "class", "action action-create icon-action-search");
            wp.writeHtml("Saved Searches");
        wp.writeEnd();

        wp.writeStart("ul");
            for (String savedSearchName : savedSearchNames) {
                String savedSearch = savedSearches.get(savedSearchName);

                wp.writeStart("li");
                    wp.writeStart("a",
                            "href", wp.url(null) + "?" + savedSearch);
                        wp.writeHtml(savedSearchName);
                    wp.writeEnd();
                wp.writeEnd();
            }
        wp.writeEnd();
    wp.writeEnd();
}

writer.start("div", "class", "searchForm");
    writer.start("div", "class", "searchControls");

        writer.start("div", "class", "searchFilters");
            if ((!singleType && !validTypes.isEmpty()) ||
                    !globalFilters.isEmpty() ||
                    !fieldFilters.isEmpty()) {
                writer.start("h2").html("Filters").end();
            }

            writer.start("form",
                    "class", "searchFiltersType",
                    "method", "get",
                    "action", wp.url(null));

                writer.writeElement("input", "type", "hidden", "name", Utf8Filter.CHECK_PARAMETER, "value", Utf8Filter.CHECK_VALUE);
                writer.writeElement("input", "type", "hidden", "name", "reset", "value", "true");
                writer.writeElement("input", "type", "hidden", "name", Search.NAME_PARAMETER, "value", search.getName());

                for (ObjectType type : search.getTypes()) {
                    writer.writeElement("input", "type", "hidden", "name", Search.TYPES_PARAMETER, "value", type.getId());
                }

                writer.writeElement("input", "type", "hidden", "name", Search.IS_ONLY_PATHED, "value", search.isOnlyPathed());
                writer.writeElement("input", "type", "hidden", "name", Search.ADDITIONAL_QUERY_PARAMETER, "value", search.getAdditionalPredicate());
                writer.writeElement("input", "type", "hidden", "name", Search.PARENT_PARAMETER, "value", search.getParentId());
                writer.writeElement("input", "type", "hidden", "name", Search.PARENT_TYPE_PARAMETER, "value", search.getParentTypeId());
                writer.writeElement("input", "type", "hidden", "name", Search.SUGGESTIONS_PARAMETER, "value", search.isSuggestions());

                writer.writeElement("input",
                        "type", "hidden",
                        "name", Search.QUERY_STRING_PARAMETER,
                        "value", search.getQueryString());

                if (!singleType && !validTypes.isEmpty()) {
                    wp.writeTypeSelect(
                            validTypes,
                            selectedType,
                            "Any Types",
                            "name", Search.SELECTED_TYPE_PARAMETER,
                            "data-bsp-autosubmit", "",
                            "data-searchable", true);
                }

            writer.end();

            writer.start("form",
                    "class", "searchFiltersRest" + (singleType || validTypes.isEmpty() ? " searchFiltersRest-single" : ""),
                    "data-bsp-autosubmit", "",
                    "method", "get",
                    "action", ObjectUtils.firstNonNull(request.getAttribute("resultPath"), wp.url(request.getAttribute("resultJsp"))),
                    "target", resultTarget);

                writer.writeElement("input", "type", "hidden", "name", Utf8Filter.CHECK_PARAMETER, "value", Utf8Filter.CHECK_VALUE);
                writer.writeElement("input", "type", "hidden", "name", Search.NAME_PARAMETER, "value", search.getName());
                writer.writeElement("input", "type", "hidden", "name", Search.SORT_PARAMETER, "value", search.getSort());

                for (ObjectType type : search.getTypes()) {
                    writer.writeElement("input", "type", "hidden", "name", Search.TYPES_PARAMETER, "value", type.getId());
                }

                writer.writeElement("input", "type", "hidden", "name", Search.IS_ONLY_PATHED, "value", search.isOnlyPathed());
                writer.writeElement("input", "type", "hidden", "name", Search.ADDITIONAL_QUERY_PARAMETER, "value", search.getAdditionalPredicate());
                writer.writeElement("input", "type", "hidden", "name", Search.PARENT_PARAMETER, "value", search.getParentId());
                writer.writeElement("input", "type", "hidden", "name", Search.PARENT_TYPE_PARAMETER, "value", search.getParentTypeId());
                writer.writeElement("input", "type", "hidden", "name", Search.SUGGESTIONS_PARAMETER, "value", search.isSuggestions());
                writer.writeElement("input", "type", "hidden", "name", Search.OFFSET_PARAMETER, "value", search.getOffset());
                writer.writeElement("input", "type", "hidden", "name", Search.LIMIT_PARAMETER, "value", search.getLimit());
                writer.writeElement("input", "type", "hidden", "name", Search.SELECTED_TYPE_PARAMETER, "value", selectedType != null ? selectedType.getId() : null);

                writer.start("div", "class", "searchInput");
                    writer.start("label", "for", wp.createId()).html("Search").end();
                    writer.writeElement("input",
                            "type", "text",
                            "class", "autoFocus",
                            "id", wp.getId(),
                            "name", Search.QUERY_STRING_PARAMETER,
                            "value", search.getQueryString());
                    writer.start("button").html("Go").end();
                writer.end();

                if (selectedType == null) {
                    writer.start("div", "class", "searchFiltersGlobal");
                        for (ObjectType filter : globalFilters) {
                            String filterId = filter.getId().toString();
                            State filterState = State.getInstance(Query.from(Object.class).where("_id = ?", search.getGlobalFilters().get(filterId)).first());

                            writer.start("div", "class", "searchFilter");
                                writer.writeElement("input",
                                        "type", "text",
                                        "class", "objectId",
                                        "name", "gf." + filterId,
                                        "placeholder", "Filter: " + filter.getDisplayName(),
                                        "data-editable", false,
                                        "data-label", filterState != null ? filterState.getLabel() : null,
                                        "data-restorable", false,
                                        "data-typeIds", filterId,
                                        "value", filterState != null ? filterState.getId() : null);
                            writer.end();
                        }
                    writer.end();
                }

                writer.start("div", "class", "searchFiltersLocal");
                    if (!fieldFilters.isEmpty()) {
                        writer.start("div", "class", "searchMissing");
                            writer.html("Missing?");
                        writer.end();
                    }

                    if (selectedType != null) {
                        if (selectedType.getGroups().contains(ColorImage.class.getName())) {
                            writer.writeStart("div", "class", "searchFilter");
                                writer.writeElement("input",
                                        "type", "text",
                                        "class", "color",
                                        "name", Search.COLOR_PARAMETER,
                                        "value", search.getColor());
                            writer.writeEnd();
                        }
                    }

                    for (Map.Entry<String, ObjectField> entry : fieldFilters.entrySet()) {
                        String fieldName = entry.getKey();
                        ObjectField field = entry.getValue();
                        ToolUi fieldUi = field.as(ToolUi.class);
                        Set<ObjectType> fieldTypes = field.getTypes();
                        StringBuilder fieldTypeIds = new StringBuilder();

                        if (!ObjectUtils.isBlank(fieldTypes)) {
                            for (ObjectType fieldType : fieldTypes) {
                                fieldTypeIds.append(fieldType.getId()).append(",");
                            }

                            fieldTypeIds.setLength(fieldTypeIds.length() - 1);
                        }

                        String inputName = "f." + fieldName;
                        String displayName = field.getDisplayName();
                        String displayPrefix = (displayName.endsWith("?") ? displayName : displayName + ":") + " ";
                        Map<String, String> filterValue = search.getFieldFilters().get(fieldName);
                        String fieldValue = filterValue != null ? filterValue.get("") : null;
                        String fieldInternalItemType = field.getInternalItemType();

                        writer.start("div", "class", "searchFilter searchFilter-" + fieldInternalItemType);
                            if (ObjectField.BOOLEAN_TYPE.equals(fieldInternalItemType)) {
                                writer.writeStart("select", "name", inputName);
                                    writer.writeStart("option", "value", "").writeHtml(displayName).writeEnd();
                                    writer.writeStart("option",
                                            "selected", "true".equals(fieldValue) ? "selected" : null,
                                            "value", "true");
                                        writer.writeHtml(displayPrefix);
                                        writer.writeHtml("Yes");
                                    writer.writeEnd();

                                    writer.writeStart("option",
                                            "selected", "false".equals(fieldValue) ? "selected" : null,
                                            "value", "false");
                                        writer.writeHtml(displayPrefix);
                                        writer.writeHtml("No");
                                    writer.writeEnd();
                                writer.writeEnd();

                                writer.writeElement("input",
                                        "type", "hidden",
                                        "name", inputName + ".t",
                                        "value", "b");

                            } else if (ObjectField.DATE_TYPE.equals(fieldInternalItemType)) {
                                writer.writeElement("input",
                                        "type", "text",
                                        "class", "date",
                                        "name", inputName,
                                        "placeholder", displayName,
                                        "value", fieldValue);

                                writer.writeElement("input",
                                        "type", "text",
                                        "class", "date",
                                        "name", inputName + ".x",
                                        "placeholder", "(End)",
                                        "value", filterValue != null ? filterValue.get("x") : null);

                                writer.writeElement("input",
                                        "type", "hidden",
                                        "name", inputName + ".t",
                                        "value", "d");

                                writer.writeElement("input",
                                        "type", "checkbox",
                                        "name", inputName + ".m",
                                        "value", true,
                                        "checked", filterValue != null && ObjectUtils.to(boolean.class, filterValue.get("m")) ? "checked" : null);

                            } else if (ObjectField.NUMBER_TYPE.equals(fieldInternalItemType)) {
                                writer.writeElement("input",
                                        "type", "text",
                                        "name", inputName,
                                        "placeholder", displayName,
                                        "value", fieldValue);

                                writer.writeElement("input",
                                        "type", "text",
                                        "name", inputName + ".x",
                                        "placeholder", "(Maximum)",
                                        "value", filterValue != null ? filterValue.get("x") : null);

                                writer.writeElement("input",
                                        "type", "hidden",
                                        "name", inputName + ".t",
                                        "value", "n");

                            } else if (ObjectField.TEXT_TYPE.equals(fieldInternalItemType)) {
                                if (field.getValues() == null || field.getValues().isEmpty()) {
                                    writer.writeElement("input",
                                            "type", "text",
                                            "name", inputName,
                                            "placeholder", displayName,
                                            "value", fieldValue);

                                    writer.writeElement("input",
                                            "type", "hidden",
                                            "name", inputName + ".t",
                                            "value", "t");

                                } else {
                                    writer.writeStart("select", "name", inputName);
                                        writer.writeStart("option", "value", "").writeHtml(displayName).writeEnd();

                                        for (ObjectField.Value v : field.getValues()) {
                                            writer.writeStart("option",
                                                    "selected", v.getValue().equals(fieldValue) ? "selected" : null,
                                                    "value", v.getValue());
                                                writer.writeHtml(v.getLabel());
                                            writer.writeEnd();
                                        }
                                    writer.end();
                                }

                                writer.writeElement("input",
                                        "type", "checkbox",
                                        "name", inputName + ".m",
                                        "value", true,
                                        "checked", filterValue != null && ObjectUtils.to(boolean.class, filterValue.get("m")) ? "checked" : null);

                            } else {
                                State fieldState = State.getInstance(Query.from(Object.class).where("_id = ?", fieldValue).first());

                                writer.writeElement("input",
                                        "type", "text",
                                        "class", "objectId",
                                        "name", inputName,
                                        "placeholder", displayName,
                                        "data-additional-query", field.getPredicate(),
                                        "data-editable", false,
                                        "data-label", fieldState != null ? fieldState.getLabel() : null,
                                        "data-pathed", ToolUi.isOnlyPathed(field),
                                        "data-restorable", false,
                                        "data-searcher-path", fieldUi.getInputSearcherPath(),
                                        "data-typeIds", fieldTypeIds,
                                        "value", fieldValue);
                                writer.writeElement("input",
                                        "type", "checkbox",
                                        "name", inputName + ".m",
                                        "value", true,
                                        "checked", filterValue != null && ObjectUtils.to(boolean.class, filterValue.get("m")) ? "checked" : null);
                            }
                        writer.end();
                    }
                writer.end();

                writer.writeStart("div", "class", "searchFilter");
                    wp.writeMultipleVisibilitySelect(
                            selectedType,
                            search.getVisibilities(),
                            "name", Search.VISIBILITIES_PARAMETER);
                writer.writeEnd();

                for (Tool tool : Query.from(Tool.class).selectAll()) {
                    tool.writeSearchFilters(search, wp);
                }

                writer.writeStart("div", "class", "searchFilter searchFilter-advancedQuery");
                    writer.writeElement("input",
                            "type", "text",
                            "class", "code",
                            "name", Search.ADVANCED_QUERY_PARAMETER,
                            "placeholder", "Advanced Query",
                            "value", search.getAdvancedQuery());

                    writer.writeStart("a",
                            "class", "icon icon-action-edit icon-only",
                            "href", wp.cmsUrl("/searchAdvancedQuery"),
                            "target", "searchAdvancedQuery");
                        writer.writeHtml("Edit");
                    writer.writeEnd();
                writer.writeEnd();
            writer.end();

            writer.start("a",
                    "class", "action action-cancel search-reset",
                    "onclick",
                            "var $source = $(this).popup('source');" +
                            "if ($source) {" +
                                "if ($source.is('a')) {" +
                                    "console.log($source[0]);" +
                                    "$source.click();" +
                                "} else if ($source.is('form')) {" +
                                    "$source[0].reset();" +
                                    "$source.submit();" +
                                "}" +
                            "}" +
                            "return false;");
                writer.html("Reset");
            writer.end();

        writer.end();

        if (!ObjectUtils.isBlank(newJsp) && (selectedType == null || !selectedType.isAbstract())) {
            writer.start("div", "class", "searchCreate");
                writer.start("h2").html("Create").end();

                writer.start("form",
                        "method", "get",
                        "action", wp.url(newJsp),
                        "target", ObjectUtils.isBlank(newTarget) ? null : newTarget);

                    if (singleType) {
                        if (wp.hasPermission("type/" + selectedType.getId() + "/write") &&
                                !selectedType.getGroups().contains(Singleton.class.getName())) {
                            writer.writeElement("input", "type", "hidden", "name", "typeId", "value", selectedType.getId());
                            writer.writeStart("button", "class", "action action-create", "style", "width: auto;");
                                writer.writeHtml("New " + wp.getObjectLabel(selectedType));
                            writer.writeEnd();
                        }

                    } else {
                        Set<ObjectType> creatableTypes = new LinkedHashSet<ObjectType>(validTypes.size());

                        for (ObjectType t : validTypes) {
                            if (!t.getGroups().contains(Singleton.class.getName())) {
                                creatableTypes.add(t);
                            }
                        }

                        wp.writeTypeSelect(
                                creatableTypes,
                                selectedType,
                                null,
                                "name", "typeId",
                                "data-searchable", true);
                        writer.writeStart("button", "class", "action action-create");
                            writer.writeHtml("New");
                        writer.writeEnd();
                    }

                writer.end();
            writer.end();
        }

    writer.end();

    writer.start("div", "class", "searchResult frame", "name", resultTarget);
    writer.end();

writer.end();
%><%!

private static void addFieldFilters(
        Map<String, ObjectField> fieldFilters,
        String prefix,
        ObjectStruct struct) {
    for (ObjectField field : ObjectStruct.Static.findIndexedFields(struct)) {
        ToolUi fieldUi = field.as(ToolUi.class);

        if (!fieldUi.isEffectivelyFilterable()) {
            continue;
        }

        String fieldName = field.getInternalName();

        if (ObjectField.RECORD_TYPE.equals(field.getInternalItemType())) {
            boolean embedded = field.isEmbedded();

            if (!embedded) {
                embedded = true;

                for (ObjectType t : field.getTypes()) {
                    if (!t.isEmbedded()) {
                        embedded = false;
                        break;
                    }
                }
            }

            if (embedded) {
                for (ObjectType t : field.getTypes()) {
                    addFieldFilters(fieldFilters, fieldName + "/", t);
                }
                continue;
            }
        }

        fieldFilters.put(prefix + fieldName, field);
    }
}
%>
