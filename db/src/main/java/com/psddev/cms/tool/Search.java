package com.psddev.cms.tool;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.Workflow;
import com.psddev.cms.db.WorkflowState;
import com.psddev.cms.tool.search.ListSearchResultView;
import com.psddev.dari.db.CompoundPredicate;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.db.ObjectStruct;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Singleton;
import com.psddev.dari.db.Sorter;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.HuslColorSpace;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PaginatedResult;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;
import com.psddev.dari.util.UuidUtils;

public class Search extends Record {

    public static final String ADDITIONAL_PREDICATE_PARAMETER = "aq";
    public static final String ADVANCED_QUERY_PARAMETER = "av";
    public static final String GLOBAL_FILTER_PARAMETER_PREFIX = "gf.";
    public static final String FIELD_FILTER_PARAMETER_PREFIX = "f.";
    public static final String LIMIT_PARAMETER = "l";
    public static final String MISSING_FILTER_PARAMETER_SUFFIX = ".m";
    public static final String NAME_PARAMETER = "n";
    public static final String OFFSET_PARAMETER = "o";
    public static final String COLOR_PARAMETER = "c";
    public static final String ONLY_PATHED_PARAMETER = "p";
    public static final String PARENT_PARAMETER = "pt";
    public static final String PARENT_TYPE_PARAMETER = "py";
    public static final String QUERY_STRING_PARAMETER = "q";
    public static final String SELECTED_TYPE_PARAMETER = "st";
    public static final String SHOW_DRAFTS_PARAMETER = "d";
    public static final String VISIBILITIES_PARAMETER = "v";
    public static final String SHOW_MISSING_PARAMETER = "m";
    public static final String SORT_PARAMETER = "s";
    public static final String SUGGESTIONS_PARAMETER = "sg";
    public static final String TYPES_PARAMETER = "rt";

    public static final String NEWEST_SORT_LABEL = "Newest";
    public static final String NEWEST_SORT_VALUE = "_newest";
    public static final String RELEVANT_SORT_LABEL = "Relevant";
    public static final String RELEVANT_SORT_VALUE = "_relevant";

    public static final double RELEVANT_SORT_LABEL_BOOST = 10.0;

    private String name;
    private Set<ObjectType> types;
    private ObjectType selectedType;
    private String queryString;
    private String color;
    private boolean onlyPathed;
    private String additionalPredicate;
    private String advancedQuery;
    private UUID parentId;
    private UUID parentTypeId;
    private Map<String, String> globalFilters;
    private Map<String, Map<String, String>> fieldFilters;
    private String sort;
    private boolean showDrafts;
    private List<String> visibilities;
    private boolean showMissing;
    private boolean suggestions;
    private long offset;
    private int limit;

    public Search() {
    }

    public Search(ObjectField field) {
        getTypes().addAll(field.getTypes());
        setOnlyPathed(ToolUi.isOnlyPathed(field));
        setAdditionalPredicate(field.getPredicate());
    }

    public Search(ToolPageContext page, Iterable<UUID> typeIds) {
        this.page = page;

        setName(page.param(String.class, NAME_PARAMETER));

        if (typeIds != null) {
            for (UUID typeId : typeIds) {
                ObjectType type = ObjectType.getInstance(typeId);

                if (type != null) {
                    getTypes().add(type);
                }
            }
        }

        for (String name : page.paramNamesList()) {
            if (name.startsWith(GLOBAL_FILTER_PARAMETER_PREFIX)) {
                getGlobalFilters().put(name.substring(GLOBAL_FILTER_PARAMETER_PREFIX.length()), page.param(String.class, name));

            } else if (name.startsWith(FIELD_FILTER_PARAMETER_PREFIX)) {
                String filterName = name.substring(FIELD_FILTER_PARAMETER_PREFIX.length());
                int dotAt = filterName.lastIndexOf('.');
                String filterValueKey;

                if (dotAt < 0) {
                    filterValueKey = "";

                } else {
                    filterValueKey = filterName.substring(dotAt + 1);

                    if (filterValueKey.length() > 1) {
                        filterValueKey = "";

                    } else {
                        filterName = filterName.substring(0, dotAt);
                    }
                }

                Map<String, String> filterValue = getFieldFilters().get(filterName);

                if (filterValue == null) {
                    filterValue = new HashMap<String, String>();
                    getFieldFilters().put(filterName, filterValue);
                }

                filterValue.put(filterValueKey, page.param(String.class, name));
            }
        }

        setSelectedType(ObjectType.getInstance(page.param(UUID.class, SELECTED_TYPE_PARAMETER)));
        setQueryString(page.paramOrDefault(String.class, QUERY_STRING_PARAMETER, "").trim());
        setColor(page.param(String.class, COLOR_PARAMETER));
        setOnlyPathed(page.param(boolean.class, IS_ONLY_PATHED));
        setAdditionalPredicate(page.param(String.class, ADDITIONAL_QUERY_PARAMETER));
        setAdvancedQuery(page.param(String.class, ADVANCED_QUERY_PARAMETER));
        setParentId(page.param(UUID.class, PARENT_PARAMETER));
        setParentTypeId(page.param(UUID.class, PARENT_TYPE_PARAMETER));
        setSort(page.param(String.class, SORT_PARAMETER));
        setShowDrafts(page.param(boolean.class, SHOW_DRAFTS_PARAMETER));
        setVisibilities(page.params(String.class, VISIBILITIES_PARAMETER));
        setSuggestions(page.param(boolean.class, SUGGESTIONS_PARAMETER));
        setOffset(page.param(long.class, OFFSET_PARAMETER));
        setLimit(page.paramOrDefault(int.class, LIMIT_PARAMETER, 10));

        for (Tool tool : Query.from(Tool.class).selectAll()) {
            tool.initializeSearch(this, page);
        }
    }

    public Search(ToolPageContext page) {
        this(page, page.params(UUID.class, TYPES_PARAMETER));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<ObjectType> getTypes() {
        if (types == null) {
            types = new HashSet<ObjectType>();
        }
        return types;
    }

    public void setTypes(Set<ObjectType> types) {
        this.types = types;
    }

    public ObjectType getSelectedType() {
        return selectedType;
    }

    public void setSelectedType(ObjectType selectedType) {
        this.selectedType = selectedType;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isOnlyPathed() {
        return onlyPathed;
    }

    public void setOnlyPathed(boolean onlyPathed) {
        this.onlyPathed = onlyPathed;
    }

    public String getAdditionalPredicate() {
        return additionalPredicate;
    }

    public void setAdditionalPredicate(String additionalPredicate) {
        this.additionalPredicate = additionalPredicate;
    }

    public String getAdvancedQuery() {
        return advancedQuery;
    }

    public void setAdvancedQuery(String advancedQuery) {
        this.advancedQuery = advancedQuery;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public UUID getParentTypeId() {
        return parentTypeId;
    }

    public void setParentTypeId(UUID parentTypeId) {
        this.parentTypeId = parentTypeId;
    }

    public Map<String, String> getGlobalFilters() {
        if (globalFilters == null) {
            globalFilters = new HashMap<String, String>();
        }
        return globalFilters;
    }

    public void setGlobalFilters(Map<String, String> globalFilters) {
        this.globalFilters = globalFilters;
    }

    public Map<String, Map<String, String>> getFieldFilters() {
        if (fieldFilters == null) {
            fieldFilters = new HashMap<String, Map<String, String>>();
        }
        return fieldFilters;
    }

    public void setFieldFilters(Map<String, Map<String, String>> fieldFilters) {
        this.fieldFilters = fieldFilters;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public boolean isShowDrafts() {
        return showDrafts;
    }

    public void setShowDrafts(boolean showDrafts) {
        this.showDrafts = showDrafts;
    }

    public List<String> getVisibilities() {
        if (visibilities == null) {
            visibilities = new ArrayList<String>();
        }
        return visibilities;
    }

    public void setVisibilities(List<String> visibilities) {
        this.visibilities = visibilities;
    }

    @Deprecated
    public boolean isShowMissing() {
        return showMissing;
    }

    @Deprecated
    public void setShowMissing(boolean showMissing) {
        this.showMissing = showMissing;
    }

    public boolean isSuggestions() {
        return suggestions;
    }

    public void setSuggestions(boolean suggestions) {
        this.suggestions = suggestions;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public Set<ObjectType> findValidTypes() {
        Set<ObjectType> types = getTypes();
        List<ObjectType> validTypes = new ArrayList<ObjectType>();

        if (types.size() == 1) {
            ObjectType type = types.iterator().next();

            if (type != null && Content.class.equals(type.getObjectClass())) {
                types.clear();
            }
        }

        for (ObjectType type : types) {
            validTypes.addAll(type.as(ToolUi.class).findDisplayTypes());
        }

        Collections.sort(validTypes);
        return new LinkedHashSet<ObjectType>(validTypes);
    }

    /**
     * Finds all sorters that can be applied to this search.
     *
     * @return Never {@code null}. The key is the unique ID, and the value
     * is the label. Sorted by the label.
     */
    public Map<String, String> findSorts() {
        Map<String, String> sorts = new LinkedHashMap<String, String>();
        ObjectType selectedType = getSelectedType();

        if (!ObjectUtils.isBlank(getQueryString())) {
            sorts.put(RELEVANT_SORT_VALUE, RELEVANT_SORT_LABEL);
        }

        addSorts(sorts, selectedType);
        addSorts(sorts, Database.Static.getDefault().getEnvironment());

        List<Map.Entry<String, String>> sortsList = new ArrayList<Map.Entry<String, String>>(sorts.entrySet());

        Collections.sort(sortsList, new Comparator<Map.Entry<String, String>>() {

            @Override
            public int compare(Map.Entry<String, String> x, Map.Entry<String, String> y) {
                return ObjectUtils.compare(x.getValue(), y.getValue(), false);
            }
        });

        sorts.clear();

        for (Map.Entry<String, String> entry : sortsList) {
            sorts.put(entry.getKey(), entry.getValue());
        }

        return sorts;
    }

    private void addSorts(Map<String, String> sorts, ObjectStruct struct) {
        if (struct != null) {
            for (ObjectField field : ObjectStruct.Static.findIndexedFields(struct)) {
                if (field.as(ToolUi.class).isEffectivelySortable()) {
                    sorts.put(field.getInternalName(), field.getDisplayName());
                }
            }
        }
    }

    public static Predicate getVisibilitiesPredicate(ObjectType selectedType, Collection<String> visibilities, Set<UUID> validTypeIds, boolean showDrafts) {

        Set<UUID> visibilityTypeIds = new HashSet<UUID>();

        Predicate visibilitiesPredicate = null;

        for (String visibility : visibilities) {
            if ("p".equals(visibility)) {
                Set<String> comparisonKeys = new HashSet<String>();
                DatabaseEnvironment environment = Database.Static.getDefault().getEnvironment();

                addVisibilityFields(comparisonKeys, environment);

                for (ObjectType type : environment.getTypes()) {
                    addVisibilityFields(comparisonKeys, type);
                }

                for (String key : comparisonKeys) {
                    if (showDrafts) {
                        visibilitiesPredicate = CompoundPredicate.combine(
                                PredicateParser.OR_OPERATOR,
                                visibilitiesPredicate,
                                PredicateParser.Static.parse(key + " = missing or " + key + " != missing or " + key + " = true"));

                    } else {
                        visibilitiesPredicate = CompoundPredicate.combine(
                                PredicateParser.OR_OPERATOR,
                                visibilitiesPredicate,
                                PredicateParser.Static.parse(key + " = missing"));
                    }
                }

            } else if ("w".equals(visibility)) {
                Set<String> ss = new HashSet<String>();

                for (Workflow w : (selectedType == null ?
                        Query.from(Workflow.class) :
                        Query.from(Workflow.class).where("contentTypes = ?", selectedType)).
                        selectAll()) {
                    for (WorkflowState s : w.getStates()) {
                        String value = s.getName();

                        ss.add(value);
                        addVisibilityTypeIds(visibilityTypeIds, validTypeIds, "cms.workflow.currentState", value);
                    }
                }

                visibilitiesPredicate = CompoundPredicate.combine(
                        PredicateParser.OR_OPERATOR,
                        visibilitiesPredicate,
                        PredicateParser.Static.parse("cms.workflow.currentState = ?", ss));

            } else if (visibility.startsWith("w.")) {
                String value = visibility.substring(2);
                visibilitiesPredicate = CompoundPredicate.combine(
                        PredicateParser.OR_OPERATOR,
                        visibilitiesPredicate,
                        PredicateParser.Static.parse("cms.workflow.currentState = ?", value));

                addVisibilityTypeIds(visibilityTypeIds, validTypeIds, "cms.workflow.currentState", value);

            } else if (visibility.startsWith("b.")) {
                String field = visibility.substring(2);
                visibilitiesPredicate = CompoundPredicate.combine(
                        PredicateParser.OR_OPERATOR,
                        visibilitiesPredicate,
                        PredicateParser.Static.parse(field + " = true"));

                addVisibilityTypeIds(visibilityTypeIds, validTypeIds, field, "true");

            } else if (visibility.startsWith("t.")) {
                visibility = visibility.substring(2);
                int equalAt = visibility.indexOf('=');

                if (equalAt > -1) {
                    String field = visibility.substring(0, equalAt);
                    String value = visibility.substring(equalAt + 1);
                    visibilitiesPredicate = CompoundPredicate.combine(
                            PredicateParser.OR_OPERATOR,
                            visibilitiesPredicate,
                            PredicateParser.Static.parse(field + " = ?", value));

                    addVisibilityTypeIds(visibilityTypeIds, validTypeIds, field, value);
                }
            }
        }

        if (validTypeIds != null) {
            validTypeIds.addAll(visibilityTypeIds);
        }

        return visibilitiesPredicate;

    }

    public Query<?> toQuery(Site site) {

        // If the query string is an URL, hit it to find the ID.
        String queryString = getQueryString();

        if (!ObjectUtils.isBlank(queryString)) {
            try {
                URL qsUrl = new URL(queryString.trim());
                URLConnection qsConnection = qsUrl.openConnection();

                if (qsConnection instanceof HttpURLConnection) {
                    HttpURLConnection qsHttp = (HttpURLConnection) qsConnection;

                    qsHttp.setConnectTimeout(1000);
                    qsHttp.setReadTimeout(1000);
                    qsHttp.setRequestMethod("HEAD");
                    qsHttp.setRequestProperty("Brightspot-Main-Object-Id-Query", "true");

                    InputStream qsInput = qsHttp.getInputStream();

                    try {
                        UUID mainObjectId = ObjectUtils.to(UUID.class, qsHttp.getHeaderField("Brightspot-Main-Object-Id"));

                        if (mainObjectId != null) {
                            return Query.
                                    fromAll().
                                    or("_id = ?", mainObjectId).
                                    or("* matches ?", mainObjectId).
                                    sortRelevant(100.0, "_id = ?", mainObjectId);
                        }

                    } finally {
                        qsInput.close();
                    }
                }

            } catch (IOException error) {
                // Can't connect to the URL in the query string to get the main
                // object ID, but that's OK to ignore and move on.
            }
        }

        Query<?> query = null;
        Set<ObjectType> types = getTypes();
        ObjectType selectedType = getSelectedType();
        Set<ObjectType> validTypes = findValidTypes();
        Set<UUID> validTypeIds = null;
        boolean isAllSearchable = true;

        if (selectedType != null) {
            if (selectedType.isAbstract()) {
                for (ObjectType t : selectedType.findConcreteTypes()) {
                    if (!Content.Static.isSearchableType(t)) {
                        isAllSearchable = false;
                        break;
                    }
                }

            } else {
                isAllSearchable = Content.Static.isSearchableType(selectedType);
            }

            query = Query.fromType(selectedType);

        } else {
            for (ObjectType type : validTypes) {
                if (type.isAbstract()) {
                    for (ObjectType t : type.findConcreteTypes()) {
                        if (!Content.Static.isSearchableType(t)) {
                            isAllSearchable = false;
                            break;
                        }
                    }

                } else if (!Content.Static.isSearchableType(type)) {
                    isAllSearchable = false;
                }
            }

            if (types.size() == 1) {
                for (ObjectType type : types) {
                    query = Query.fromType(type);
                    break;
                }

            } else {
                query = isAllSearchable ? Query.fromGroup(Content.SEARCHABLE_GROUP) : Query.fromAll();

                if (!validTypes.isEmpty()) {
                    validTypeIds = new HashSet<UUID>();

                    for (ObjectType t : validTypes) {
                        validTypeIds.add(t.getId());
                    }
                }
            }
        }

        String sort = getSort();
        boolean metricSort = false;

        if (RELEVANT_SORT_VALUE.equals(sort)) {
            if (isAllSearchable) {
                query.sortRelevant(100.0, "_label = ?", queryString);
                query.sortRelevant(10.0, "_label matches ?", queryString);
            }

        } else if (sort != null) {
            ObjectField sortField = selectedType != null ?
                    selectedType.getFieldGlobally(sort) :
                    Database.Static.getDefault().getEnvironment().getField(sort);

            if (sortField != null) {
                if (sortField.isMetric()) {
                    metricSort = true;
                }

                String sortName = selectedType != null ?
                        selectedType.getInternalName() + "/" + sort :
                        sort;

                if (ObjectField.TEXT_TYPE.equals(sortField.getInternalType())) {
                    query.sortAscending(sortName);

                } else {
                    query.sortDescending(sortName);
                }
            }
        }

        if (metricSort) {
            // Skip Solr-related operations if sorting by metrics.

        } else if (ObjectUtils.isBlank(queryString)) {
            if (isAllSearchable) {
                query.and("* ~= *");
            }

        } else {

            // Strip http: or https: from the query for search by path below.
            if (queryString.length() > 8 &&
                    StringUtils.matches(queryString, "(?i)https?://.*")) {
                int slashAt = queryString.indexOf("/", 8);

                if (slashAt > -1) {
                    queryString = queryString.substring(slashAt);
                }
            }

            // Search by path.
            if (isAllSearchable && queryString.startsWith("/") && queryString.length() > 1) {
                List<String> paths = new ArrayList<String>();

                for (Directory directory : Query.
                        from(Directory.class).
                        where("path ^=[c] ?", queryString).
                        selectAll()) {
                    paths.add(directory.getRawPath());
                }

                int lastSlashAt = queryString.lastIndexOf("/");

                if (lastSlashAt > 0) {
                    for (Directory directory : Query.
                            from(Directory.class).
                            where("path ^=[c] ?", queryString.substring(0, lastSlashAt)).
                            selectAll()) {
                        paths.add(directory.getRawPath() + queryString.substring(lastSlashAt + 1));
                    }
                }

                query.and(Directory.PATHS_FIELD + " ^= ?", paths);

            // Full text search.
            } else if (isAllSearchable) {
                int lastSpaceAt = queryString.lastIndexOf(" ");

                if (lastSpaceAt > -1) {
                    query.and("* ~= ?", Arrays.asList(queryString, queryString.substring(0, lastSpaceAt)));

                } else {
                    query.and("* ~= ?", queryString);
                }

            } else if (selectedType != null) {
                for (String field : selectedType.getLabelFields()) {
                    if (selectedType.getIndex(field) != null) {
                        query.and(selectedType.getInternalName() + "/" + field + " contains[c] ?", queryString);
                    }
                    break;
                }

            } else {
                Predicate predicate = null;

                for (ObjectType type : validTypes) {
                    String prefix = type.getInternalName() + "/";

                    for (String field : type.getLabelFields()) {
                        if (type.getIndex(field) != null) {
                            predicate = CompoundPredicate.combine(
                                    PredicateParser.OR_OPERATOR,
                                    predicate,
                                    PredicateParser.Static.parse(prefix + field + " contains[c] ?", queryString));
                        }
                        break;
                    }
                }

                query.and(predicate);
            }
        }

        if (isOnlyPathed()) {
            query.and(Directory.Static.hasPathPredicate());
        }

        if (isAllSearchable && selectedType == null) {
            DatabaseEnvironment environment = Database.Static.getDefault().getEnvironment();
            String q = getQueryString();

            if (!ObjectUtils.isBlank(q)) {
                q = q.replaceAll("\\s+", "").toLowerCase(Locale.ENGLISH);

                for (ObjectType t : environment.getTypes()) {
                    String name = t.getDisplayName();

                    if (!ObjectUtils.isBlank(name) &&
                            q.contains(name.replaceAll("\\s+", "").toLowerCase(Locale.ENGLISH))) {
                        query.sortRelevant(20.0, "_type = ?", t.as(ToolUi.class).findDisplayTypes());
                    }
                }
            }

            query.sortRelevant(10.0, "_type = ?", environment.getTypeByClass(Singleton.class).as(ToolUi.class).findDisplayTypes());
        }

        String additionalPredicate = getAdditionalPredicate();

        if (!ObjectUtils.isBlank(additionalPredicate)) {
            Object parent = Query.
                    from(Object.class).
                    where("_id = ?", getParentId()).
                    first();
            if (parent == null && getParentTypeId() != null) {
                ObjectType parentType = ObjectType.getInstance(getParentTypeId());
                parent = parentType.createObject(null);
            }
            query.and(additionalPredicate, parent);
        }

        String advancedQuery = getAdvancedQuery();

        if (!ObjectUtils.isBlank(advancedQuery)) {
            query.and(advancedQuery);
        }

        for (String filter : getGlobalFilters().values()) {
            if (filter != null) {
                query.and("* matches ?", filter);
            }
        }

        for (Map.Entry<String, Map<String, String>> entry : getFieldFilters().entrySet()) {
            Map<String, String> value = entry.getValue();

            if (value == null) {
                continue;
            }

            String fieldName = entry.getKey();

            if (selectedType != null && !fieldName.contains("/")) {
                fieldName = selectedType.getInternalName() + "/" + fieldName;
            }

            if (ObjectUtils.to(boolean.class, value.get("m"))) {
                query.and(fieldName + " = missing");

            } else {
                String fieldValue = value.get("");
                String queryType = value.get("t");

                if ("d".equals(queryType)) {
                    Date start = ObjectUtils.to(Date.class, fieldValue);
                    Date end = ObjectUtils.to(Date.class, value.get("x"));

                    if (start != null) {
                        query.and(fieldName + " >= ?", start);
                    }

                    if (end != null) {
                        query.and(fieldName + " <= ?", end);
                    }

                } else if ("n".equals(queryType)) {
                    Double minimum = ObjectUtils.to(Double.class, fieldValue);
                    Double maximum = ObjectUtils.to(Double.class, value.get("x"));

                    if (minimum != null) {
                        query.and(fieldName + " >= ?", fieldValue);
                    }

                    if (maximum != null) {
                        query.and(fieldName + " <= ?", maximum);
                    }

                } else {
                    if (fieldValue == null) {
                        continue;
                    }

                    if ("t".equals(queryType)) {
                        query.and(fieldName + " matches ?", fieldValue);

                    } else if ("b".equals(queryType)) {
                        query.and(fieldName + ("true".equals(fieldValue) ? " = true" : " != true"));

                    } else {
                        query.and(fieldName + " = ?", fieldValue);
                    }
                }
            }
        }

        if (site != null &&
                !site.isAllSitesAccessible()) {
            Set<ObjectType> globalTypes = new HashSet<ObjectType>();

            if (selectedType != null) {
                addGlobalTypes(globalTypes, selectedType);

            } else {
                for (ObjectType type : validTypes) {
                    addGlobalTypes(globalTypes, type);
                }
            }

            query.and(CompoundPredicate.combine(
                    PredicateParser.OR_OPERATOR,
                    site.itemsPredicate(),
                    PredicateParser.Static.parse("_type = ?", globalTypes)));
        }

        Collection<String> visibilities = getVisibilities();

        if (!visibilities.isEmpty()) {

            Predicate visibilitiesPredicate = getVisibilitiesPredicate(selectedType, visibilities, validTypeIds, isShowDrafts());

            query.and(visibilitiesPredicate);

        } else if (selectedType == null &&
                isAllSearchable) {
            Set<String> comparisonKeys = new HashSet<String>();
            DatabaseEnvironment environment = Database.Static.getDefault().getEnvironment();

            addVisibilityFields(comparisonKeys, environment);

            for (ObjectType type : environment.getTypes()) {
                addVisibilityFields(comparisonKeys, type);
            }

            for (String key : comparisonKeys) {
                if (isShowDrafts()) {
                    query.and(key + " = missing or " + key + " != missing or " + key + " = true");

                } else {
                    query.and(key + " = missing");
                }
            }

        } else if (isShowDrafts()) {
            Set<String> comparisonKeys = new HashSet<String>();
            DatabaseEnvironment environment = Database.Static.getDefault().getEnvironment();

            addVisibilityFields(comparisonKeys, environment);
            addVisibilityFields(comparisonKeys, selectedType);

            for (String key : comparisonKeys) {
                query.and(key + " = missing or " + key + " != missing or " + key + " = true");
            }
        }

        if (validTypeIds != null) {
            query.and("_type = ?", validTypeIds);
        }

        String color = getColor();

        if (color != null && color.startsWith("#")) {
            int[] husl = HuslColorSpace.Static.toHUSL(new Color(Integer.parseInt(color.substring(1), 16)));
            int normalized0 = husl[0];
            int normalized1 = husl[1];
            int normalized2 = husl[2];

            if (normalized1 < 30 || normalized2 < 15 || normalized2 > 85) {
                normalized0 = 0;
                normalized1 = 0;
            }

            Set<String> fieldNames = new HashSet<String>();
            int[] binSizes = { 24, 20, 20 };
            int[] normalized = new int[] {
                    (((int) Math.round(normalized0 / 24.0)) * 24) % 360,
                    ((int) Math.round(normalized1 / 20.0)) * 20,
                    ((int) Math.round(normalized2 / 20.0)) * 20 };

            for (int i = -1; i <= 1; i += 1) {
                for (int j = -1; j <= 1; j += 1) {
                    for (int k = -1; k <= 1; k += 1) {
                        int h = (normalized[0] + i * binSizes[0]) % 360;
                        int s = normalized[1] + j * binSizes[1];
                        int l = normalized[2] + k * binSizes[2];

                        if ((i != 0 || j != 0 || k != 0) &&
                                (h >= 0 && h < 360) &&
                                (s >= 0 && s <= 100) &&
                                s != 20 &&
                                (s != 0 || h == 0) &&
                                (l >= 0 && l <= 100) &&
                                (l < 100 && l > 0 || s == 0)) {

                            fieldNames.add("color.distribution/n_" + h + "_" + s + "_" + l);
                        }
                    }
                }
            }

            String originFieldName =
                    "color.distribution/n_" + normalized[0] +
                    "_" + normalized[1] +
                    "_" + normalized[2];

            fieldNames.add(originFieldName);
            query.and(StringUtils.join(new ArrayList<String>(fieldNames), " > 0 || ") + " > 0");
            query.sortDescending(originFieldName);

            List<Sorter> sorters = query.getSorters();

            sorters.add(0, sorters.get(sorters.size() - 1));
        }

        for (Tool tool : Query.from(Tool.class).selectAll()) {
            tool.updateSearchQuery(this, query);
        }

        if (page != null) {
            QueryRestriction.updateQueryUsingAll(query, page);
        }

        return query;
    }

    private static void addVisibilityTypeIds(Set<UUID> visibilityTypeIds, Set<UUID> validTypeIds, String field, String value) {
        if (validTypeIds == null) {
            return;
        }

        byte[] md5 = StringUtils.md5(field + "/" + value.toString().trim().toLowerCase(Locale.ENGLISH));

        for (UUID validTypeId : validTypeIds) {
            byte[] typeId = UuidUtils.toBytes(validTypeId);

            for (int i = 0, length = typeId.length; i < length; ++ i) {
                typeId[i] ^= md5[i];
            }

            visibilityTypeIds.add(UuidUtils.fromBytes(typeId));
        }
    }

    private void addGlobalTypes(Set<ObjectType> globalTypes, ObjectType type) {
        if (type != null && type.getGroups().contains(ObjectType.class.getName())) {
            globalTypes.add(type);
        }
    }

    private static void addVisibilityFields(Set<String> comparisonKeys, ObjectStruct struct) {
        if (struct == null) {
            return;
        }

        for (ObjectIndex index : struct.getIndexes()) {
            if (index.isVisibility()) {
                for (String fieldName : index.getFields()) {
                    ObjectField field = struct.getField(fieldName);

                    if (field != null) {
                        comparisonKeys.add(field.getUniqueName());
                    }
                }
            }
        }
    }

    /**
     * @param page Can't be {@code null}.
     * @param itemWriter May be {@code null}.
     * @throws IOException if unable to write to the given {@code page}.
     */
    public void writeResultHtml(SearchResultItem itemWriter) throws IOException {
        List<SearchResultView> views = new ArrayList<>();

        for (Class<? extends SearchResultView> viewClass : ClassFinder.Static.findClasses(SearchResultView.class)) {
            if (!viewClass.isInterface() && !Modifier.isAbstract(viewClass.getModifiers())) {
                SearchResultView view = TypeDefinition.getInstance(viewClass).newInstance();

                if (view.isSupported(this)) {
                    views.add(view);
                }
            }
        }

        String selectedViewClassName = page.param(String.class, "view");
        ToolUser user = page.getUser();

        if (user != null) {
            Map<String, String> userViews = user.getSearchViews();
            ObjectType selectedType = getSelectedType();
            String userViewKey = selectedType != null ? selectedType.getId().toString() : "";
            String userViewClassName = userViews.get(userViewKey);

            if (selectedViewClassName == null) {
                selectedViewClassName = userViewClassName;

            } else if (!ObjectUtils.equals(userViewClassName, selectedViewClassName)) {
                userViews.put(userViewKey, selectedViewClassName);
                user.save();
            }
        }

        SearchResultView selectedView = null;

        for (SearchResultView view : views) {
            if (view.getClass().getName().equals(selectedViewClassName)) {
                selectedView = view;
                break;
            }
        }

        if (selectedView == null) {
            for (SearchResultView view : views) {
                if (view.isPreferred(this)) {
                    selectedView = view;
                    break;
                }
            }
        }

        if (selectedView == null) {
            selectedView = new ListSearchResultView();
        }

        if (selectedView.isHtmlWrapped(this, page, itemWriter)) {
            page.writeStart("div", "class", "searchResult-container");
                page.writeStart("div", "class", "searchResult-views");
                    page.writeStart("ul", "class", "piped");
                        for (SearchResultView view : views) {
                            page.writeStart("li", "class", view.equals(selectedView) ? "selected" : null);
                                page.writeStart("a",
                                        "class", "icon icon-" + view.getIconName(),
                                        "href", page.url("", "view", view.getClass().getName()));
                                    page.writeHtml(view.getDisplayName());
                                page.writeEnd();
                            page.writeEnd();
                        }
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("div", "class", "searchResult-view");
                    writeViewHtml(itemWriter, selectedView);
                page.writeEnd();

                page.writeStart("div", "class", "frame searchResult-actions", "name", "searchResultActions");
                    page.writeStart("a",
                            "href", page.toolUrl(CmsTool.class, "/searchResultActions",
                                    "search", ObjectUtils.toJson(getState().getSimpleValues())));
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();

        } else {
            writeViewHtml(itemWriter, selectedView);
        }
    }

    private void writeViewHtml(SearchResultItem itemWriter, SearchResultView view) throws IOException {
        try {
            view.writeHtml(this, page, itemWriter != null ? itemWriter : new SearchResultItem());

        } catch (IllegalArgumentException | Query.NoFieldException error) {
            page.writeStart("div", "class", "message message-error");
            page.writeHtml("Invalid advanced query: ");
            page.writeHtml(error.getMessage());
            page.writeEnd();
        }
    }

    /** @deprecated Use {@link #toQuery(Site)} instead. */
    @Deprecated
    public Query<?> toQuery() {
        return toQuery(null);
    }

    // --- Deprecated ---

    /** @deprecated Use {@link #ADDITIONA_PREDICATE_PARAMETER} instead. */
    @Deprecated
    public static final String ADDITIONAL_QUERY_PARAMETER = ADDITIONAL_PREDICATE_PARAMETER;

    /** @deprecated Use {@link #ONLY_PATHED_PARAMETER} instead. */
    @Deprecated
    public static final String IS_ONLY_PATHED = ONLY_PATHED_PARAMETER;

    /** @deprecated Use {@link #TYPES_PARAMETER} instead. */
    @Deprecated
    public static final String REQUESTED_TYPES_PARAMETER = TYPES_PARAMETER;

    /** @deprecated Use {@link #Search(ToolPageContext, Collection)} instead. */
    @Deprecated
    public Search(ToolPageContext page, UUID... typeIds) {
        this(page, typeIds != null ? Arrays.asList(typeIds) : null);
    }

    /** @deprecated Use {@link #getTypes} instead. */
    @Deprecated
    public Set<ObjectType> getRequestedTypes() {
        return getTypes();
    }

    /** @deprecated Use {@link #findValidTypes} instead. */
    public Set<ObjectType> getValidTypes() {
        return findValidTypes();
    }

    /** @deprecated Use {@link #toQuery} instead. */
    @Deprecated
    public Query<?> getQuery() {
        return toQuery();
    }

    @Deprecated
    private transient ToolPageContext page;

    @Deprecated
    private transient PaginatedResult<?> result;

    /** @deprecated Use {@link #toQuery} instead. */
    @Deprecated
    public PaginatedResult<?> getResult() {
        if (result == null) {
            result = toQuery(page != null ? page.getSite() : null).select(getOffset(), getLimit());
        }
        return result;
    }
}
