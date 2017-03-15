<%@ page session="false" import="

com.psddev.cms.db.Content,
com.psddev.cms.db.Template,
com.psddev.cms.db.ToolUi,
com.psddev.cms.db.ToolUser,
com.psddev.cms.db.ToolUserSearch,
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
com.psddev.dari.util.StringUtils,
com.psddev.dari.util.Utf8Filter,
com.psddev.dari.util.UrlBuilder,
com.psddev.dari.util.UuidUtils,

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
, java.util.Arrays, java.util.stream.Collectors, com.google.common.collect.ImmutableSet" %><%

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

            addFieldFilters(ff, "", t, selectedType);

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

    if (singleType && selectedType != null) {
        writer.writeHtml(' ');
        writer.writeHtml(wp.getObjectLabel(selectedType));
    }
writer.writeEnd();

wp.writeStart("div", "class", "searchHistory");
    ToolUser user = wp.getUser();
    Map<String, Object> searchValues = search.getState().getSimpleValues();

    searchValues.remove("name");
    searchValues.remove("parentId");

    for (Iterator<Map.Entry<String, Object>> i = searchValues.entrySet().iterator(); i.hasNext();) {
        Map.Entry<String, Object> entry = i.next();

        if (entry.getKey().startsWith("_")) {
            i.remove();
        }
    }

    String context = wp.paramOrDefault(String.class, Search.CONTEXT_PARAMETER, StringUtils.hex(StringUtils.md5(ObjectUtils.toJson(searchValues))));
    String sessionId = wp.paramOrDefault(String.class, Search.SESSION_ID_PARAMETER, UuidUtils.createSequentialUuid().toString());
    String recentSearchesKeyPrefix = user.getId().toString() + context;
    List<ToolUserSearch> recentSearches = Query.from(ToolUserSearch.class)
                .where("key startsWith ?", recentSearchesKeyPrefix)
                .and("key != ?", recentSearchesKeyPrefix + sessionId)
                .sortDescending("key")
                .selectAll();

    if (!recentSearches.isEmpty()) {
        wp.writeStart("div", "class", "searchRecent");
        wp.writeStart("h2");
        wp.writeHtml(wp.localize(Search.class, "label.recentSearches"));
        wp.writeEnd();

        wp.writeStart("ul", "class", "links");
            for (ToolUserSearch recentSearch : recentSearches) {
                String localizedLabel = recentSearch.toLocalizedLabel(wp);

                if (localizedLabel != null) {
                    wp.writeStart("li");
                        wp.writeStart("a", "href", StringUtils.addQueryParameters(
                                wp.url(null) + "?" + recentSearch.getSearch(),
                                Search.PARENT_PARAMETER, search.getParentId(),
                                Search.SESSION_ID_PARAMETER, UuidUtils.createSequentialUuid()));
                            wp.writeHtml(localizedLabel);
                        wp.writeEnd();
                    wp.writeEnd();
                }
            }
        wp.writeEnd();

        wp.writeStart("a", "href", wp.cmsUrl("/misc/savedSearches.jsp"));
        wp.writeEnd();
        wp.writeEnd();
    }

    wp.writeStart("div", "class", "searchSaved");
        wp.writeStart("h2");
            wp.writeHtml(wp.localize(Search.class, "label.savedSearches"));
        wp.writeEnd();

        wp.writeStart("div", "class", "frame savedSearches", "name", "savedSearches");
            wp.writeStart("a", "href", wp.cmsUrl("/misc/savedSearches.jsp", Search.CONTEXT_PARAMETER, context));
            wp.writeEnd();
        wp.writeEnd();
    wp.writeEnd();
wp.writeEnd();

writer.start("div", "class", "searchForm");
    writer.start("div", "class", "searchControls");

        writer.start("div", "class", "searchFilters");
            if ((!singleType && !validTypes.isEmpty()) ||
                    !globalFilters.isEmpty() ||
                    !fieldFilters.isEmpty()) {
                writer.writeStart("h2");
                writer.writeHtml(wp.localize(null, "search.filters"));
                writer.writeEnd();
            }

            writer.start("form",
                    "class", "searchFiltersType",
                    "method", "get",
                    "action", wp.url(null));

                writer.writeElement("input", "type", "hidden", "name", Utf8Filter.CHECK_PARAMETER, "value", Utf8Filter.CHECK_VALUE);
                writer.writeElement("input", "type", "hidden", "name", "reset", "value", "true");
                writer.writeElement("input", "type", "hidden", "name", Search.NAME_PARAMETER, "value", search.getName());
                writer.writeElement("input", "type", "hidden", "name", Search.CONTEXT_PARAMETER, "value", context, "disabled", "disabled");
                writer.writeElement("input", "type", "hidden", "name", Search.SESSION_ID_PARAMETER, "value", sessionId, "disabled", "disabled");

                for (ObjectType type : search.getTypes()) {
                    writer.writeElement("input", "type", "hidden", "name", Search.TYPES_PARAMETER, "value", type.getId());
                }

                writer.writeElement("input", "type", "hidden", "name", Search.IS_ONLY_PATHED, "value", search.isOnlyPathed());
                writer.writeElement("input", "type", "hidden", "name", Search.ADDITIONAL_QUERY_PARAMETER, "value", search.getAdditionalPredicate());
                writer.writeElement("input", "type", "hidden", "name", Search.PARENT_PARAMETER, "value", search.getParentId());
                writer.writeElement("input", "type", "hidden", "name", Search.PARENT_TYPE_PARAMETER, "value", search.getParentTypeId());
                writer.writeElement("input", "type", "hidden", "name", Search.SUGGESTIONS_PARAMETER, "value", search.isSuggestions());

                for (UUID newItemId : search.getNewItemIds()) {
                    writer.writeElement("input", "type", "hidden", "name", Search.NEW_ITEM_IDS_PARAMETER, "value", newItemId);
                }

                writer.writeElement("input",
                        "type", "hidden",
                        "name", Search.QUERY_STRING_PARAMETER,
                        "value", search.getQueryString());

                if (!singleType && !validTypes.isEmpty()) {
                    wp.writeTypeSelect(
                            validTypes.stream()
                                .filter(wp.createTypeDisplayPredicate(ImmutableSet.of("read")))
                                .collect(Collectors.<ObjectType>toSet()),
                            selectedType,
                            wp.localize(Search.class, "label.allTypes"),
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
                writer.writeElement("input", "type", "hidden", "name", Search.CONTEXT_PARAMETER, "value", context, "disabled", "disabled");
                writer.writeElement("input", "type", "hidden", "name", Search.SESSION_ID_PARAMETER, "value", sessionId, "disabled", "disabled");
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

                for (UUID newItemId : search.getNewItemIds()) {
                    writer.writeElement("input", "type", "hidden", "name", Search.NEW_ITEM_IDS_PARAMETER, "value", newItemId);
                }

                writer.writeElement("input", "type", "hidden", "name", Search.IGNORE_SITE_PARAMETER, "value", search.isIgnoreSite());

                writer.start("div", "class", "searchInput");
                    writer.start("label", "for", wp.createId()).html(wp.localize(Search.class, "action.search")).end();
                    writer.writeElement("input",
                            "type", "text",
                            "class", "autoFocus",
                            "id", wp.getId(),
                            "name", Search.QUERY_STRING_PARAMETER,
                            "value", search.getQueryString());
                    writer.start("button").html("Go").end();
                writer.end();

                if (selectedType == null || selectedType.as(ToolUi.class).isDisplayGlobalFilters()) {
                    writer.start("div", "class", "searchFiltersGlobal");
                        for (ObjectType filter : globalFilters) {
                            String filterId = filter.getId().toString();

                            if (search.getGlobalFilters().containsKey(filterId + "#")) {
                                writer.start("div",
                                        "class", "searchFilter searchFilter-multiple",
                                        "data-type-name", filter.getInternalName());

                                    for (int i = 0; i < Integer.parseInt(search.getGlobalFilters().get(filterId + "#")); i++) {
                                        State filterState = State.getInstance(Query.from(Object.class).where("_id = ?", search.getGlobalFilters().get(filterId + i)).first());
                                        writer.writeStart("div", "class", "searchFilterItem");
                                        writer.writeElement("input",
                                                "type", "text",
                                                "class", "objectId",
                                                "name", "gf." + filterId,
                                                "placeholder", wp.localize(filter, "label.globalFilter"),
                                                "data-editable", false,
                                                "data-label", filterState != null ? filterState.getLabel() : null,
                                                "data-restorable", false,
                                                "data-typeIds", filterId,
                                                "value", filterState != null ? filterState.getId() : null);
                                        writer.writeEnd();
                                    }
                                writer.end();

                            } else {
                            State filterState = State.getInstance(Query.from(Object.class).where("_id = ?", search.getGlobalFilters().get(filterId)).first());

                            writer.start("div",
                                    "class", "searchFilter",
                                    "data-type-name", filter.getInternalName());
                                writer.writeStart("div", "class", "searchFilterItem");
                                writer.writeElement("input",
                                        "type", "text",
                                        "class", "objectId",
                                        "name", "gf." + filterId,
                                        "placeholder", wp.localize(filter, "label.globalFilter"),
                                        "data-editable", false,
                                        "data-label", filterState != null ? filterState.getLabel() : null,
                                        "data-restorable", false,
                                        "data-typeIds", filterId,
                                        "value", filterState != null ? filterState.getId() : null);
                                writer.writeEnd();
                            writer.end();
                            }
                        }
                    writer.end();
                }

                writer.start("div", "class", "searchFiltersLocal");
                    if (!fieldFilters.isEmpty()) {
                        writer.start("div", "class", "searchMissing");
                            writer.html(wp.localize(Search.class, "label.missing"));
                        writer.end();
                    }

                    if (selectedType != null) {
                        if (selectedType.getGroups().contains(ColorImage.class.getName())) {
                            writer.writeStart("div", "class", "searchFilter searchFilter-color");
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
                        String displayName = wp.localize(field, "field." + field.getInternalName());
                        String displayPrefix = (displayName.endsWith("?") ? displayName : displayName + ":") + " ";
                        Map<String, String> filterValue = search.getFieldFilters().get(fieldName);
                        String fieldValue = filterValue != null ? filterValue.get("") : null;
                        String fieldInternalItemType = field.getInternalItemType();

                        boolean searchFilterMultiple = filterValue != null && filterValue.containsKey("#");
                        ObjectType fieldParentType = field.getParentType();

                        writer.start("div",
                                "class", "searchFilter searchFilter-" + fieldInternalItemType + (searchFilterMultiple ? " searchFilter-multiple" : ""),
                                "data-type-name", fieldParentType != null ? fieldParentType.getInternalName() : null,
                                "data-field-name", field.getInternalName());
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
                                        "placeholder", wp.localize(Search.class, "label.end"),
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
                                    writer.writeStart("select", "name", inputName, "data-searchable", "true", "placeholder", displayName);
                                        writer.writeStart("option", "value", "").writeEnd();

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

                            } else if (searchFilterMultiple) {
                                for (int i = 0; i < Integer.parseInt(filterValue.get("#")); i++) {
                                    State fieldState = State.getInstance(Query.from(Object.class).where("_id = ?", filterValue.get(Integer.toString(i))).first());

                                    wp.writeStart("div", "class", "searchFilterItem");
                                    wp.writeObjectSelect(field, fieldState,
                                            "name", inputName,
                                            "placeholder", displayName,
                                            "data-dynamic-placeholder", "",
                                            "data-editable", false,
                                            "data-restorable", false);
                                    wp.writeEnd();

                                    writer.writeElement("input",
                                            "type", "checkbox",
                                            "name", inputName + ".m",
                                            "value", true,
                                            "checked", filterValue != null && ObjectUtils.to(boolean.class, filterValue.get("m")) ? "checked" : null);
                                }

                            } else {
                                State fieldState = State.getInstance(Query.from(Object.class).where("_id = ?", fieldValue).first());

                                wp.writeStart("div", "class", "searchFilterItem");
                                wp.writeObjectSelect(field, fieldState,
                                        "name", inputName,
                                        "placeholder", displayName,
                                        "data-dynamic-placeholder", "",
                                        "data-editable", false,
                                        "data-restorable", false);
                                wp.writeEnd();

                                writer.writeElement("input",
                                        "type", "checkbox",
                                        "name", inputName + ".m",
                                        "value", true,
                                        "checked", filterValue != null && ObjectUtils.to(boolean.class, filterValue.get("m")) ? "checked" : null);
                            }
                        writer.end();
                    }
                writer.end();

                writer.writeStart("div", "class", "searchFilter searchFilter-visibilities");
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
                            "placeholder", wp.localize(Search.class, "label.advancedQuery"),
                            "value", search.getAdvancedQuery());

                    String advancedQueryEditParameters = search.getAdvancedQueryEditStringParameters();

                    writer.writeElement("input",
                            "type", "hidden",
                            "name", Search.ADVANCED_QUERY_EDIT_STRING_PARAMETER,
                            "value", advancedQueryEditParameters);

                    writer.writeStart("a",
                            "class", "icon icon-action-edit icon-only",
                            "href", wp.cmsUrl("/searchAdvancedQuery"
                                    + (!StringUtils.isBlank(advancedQueryEditParameters)
                                        ? StringUtils.ensureStart("?", advancedQueryEditParameters)
                                        : "")),
                            "target", "searchAdvancedQuery");
                        writer.writeHtml("Edit");
                    writer.writeEnd();
                writer.writeEnd();
            writer.end();

            if (request.getAttribute("name") != null && request.getAttribute("name").equals("fullScreen")) {
                writer.start("a", "class", "action action-cancel",
                                  "href", new UrlBuilder(request).currentScheme().currentHost().currentPath());
                    writer.html("Reset");
                writer.end();

            } else {
            writer.start("a",
                    "class", "action action-cancel search-reset",
                    "onclick",
                            "var $source = $(this).popup('source');" +
                            "if ($source) {" +
                                "if ($source.is('a')) {" +
                                    "$source.click();" +
                                "} else if ($source.is('form')) {" +
                                    "$source[0].reset();" +
                                    "$source.submit();" +
                                "}" +
                            "}" +
                            "return false;");
                writer.html("Reset");
            writer.end();
            }

        writer.end();

        if (!ObjectUtils.isBlank(newJsp)
                && (!singleType || !selectedType.as(ToolUi.class).isReadOnly())) {
            writer.start("div", "class", "searchCreate");
                writer.start("h2").html(wp.localize(Search.class, "label.create")).end();

                writer.start("form",
                        "class", "objectId-create",
                        "method", "get",
                        "action", wp.url(newJsp),
                        "target", ObjectUtils.isBlank(newTarget) ? null : newTarget);

                    if (singleType) {
                        if (wp.hasPermission("type/" + selectedType.getId() + "/write") &&
                                !selectedType.getGroups().contains(Singleton.class.getName())) {
                            writer.writeElement("input", "type", "hidden", "name", "typeId", "value", selectedType.getId());
                            writer.writeStart("button", "class", "action action-create", "style", "width: auto;");
                                writer.writeHtml(wp.localize(selectedType, "action.newType"));
                            writer.writeEnd();
                        }

                    } else {
                        Set<ObjectType> creatableTypes = new LinkedHashSet<ObjectType>(validTypes.size());

                        for (ObjectType t : validTypes) {
                            if (!t.getGroups().contains(Singleton.class.getName())) {
                                creatableTypes.add(t);
                            }
                        }

                        wp.writeCreateTypeSelect(
                                creatableTypes.stream()
                                    .filter(t -> !t.as(ToolUi.class).isReadOnly())
                                    .filter(wp.createTypeDisplayPredicate(ImmutableSet.of("write", "read")))
                                    .collect(Collectors.<ObjectType>toSet()),
                                selectedType,
                                null,
                                "name", "typeId",
                                "data-searchable", true);
                        writer.writeStart("button", "class", "action action-create");
                            writer.writeHtml(wp.localize(Search.class, "action.new"));
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
        ObjectStruct struct,
        ObjectType selectedType) {

    for (ObjectField field : ObjectStruct.Static.findIndexedFields(struct)) {
        if (selectedType != null && selectedType.getField(field.getInternalName()) != null) {
            continue;
        }

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

private static void addFieldFilters(
        Map<String, ObjectField> fieldFilters,
        String prefix,
        ObjectStruct struct) {

    addFieldFilters(fieldFilters, prefix, struct, null);
}
%>
