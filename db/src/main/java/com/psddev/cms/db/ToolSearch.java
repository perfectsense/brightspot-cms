package com.psddev.cms.db;

import com.psddev.dari.db.CompoundPredicate;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Sorter;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * @deprecated Use {@link Query} instead.
 */
@Deprecated
public class ToolSearch extends Record {

    public static final String FIELD_PREFIX = "q.";
    public static final String OPERATOR_PREFIX = "qo.";

    private ObjectType queryType;
    private String sortFieldName;

    private transient List<ObjectField> indexedFields;
    private transient List<ObjectField> sortableFields;
    private transient ObjectField sortField;

    public ObjectType getQueryType() {
        return queryType != null ? ObjectType.getInstance(queryType.getId()) : null;
    }

    public void setQueryType(ObjectType queryType) {
        this.queryType = queryType;
        this.indexedFields = null;
        this.sortableFields = null;
    }

    public String getSortFieldName() {
        return sortFieldName;
    }

    public void setSortFieldName(String sortFieldName) {
        this.sortFieldName = sortFieldName;
        this.sortField = null;
    }

    public List<ObjectField> getIndexedFields() {
        if (indexedFields == null) {
            ObjectType type = getQueryType();
            Set<String> indexedFieldNames = new HashSet<String>();
            List<ObjectField> newIndexedFields = new ArrayList<ObjectField>();

            for (ObjectIndex index : type.getIndexes()) {
                List<String> fields = index.getFields();
                if (fields != null) {
                    indexedFieldNames.addAll(fields);
                }
            }

            for (ObjectField field : type.getFields()) {
                if (indexedFieldNames.contains(field.getInternalName())) {
                    newIndexedFields.add(field);
                }
            }

            indexedFields = newIndexedFields;
        }

        return indexedFields;
    }

    public List<ObjectField> getSortableFields() {
        if (sortableFields == null) {
            List<ObjectField> newSortableFields = new ArrayList<ObjectField>();
            DatabaseEnvironment environment = getQueryType().getState().getDatabase().getEnvironment();

            newSortableFields.add(environment.getField("cms.content.publishDate"));
            newSortableFields.add(environment.getField("cms.content.updateDate"));

            for (ObjectField field : getIndexedFields()) {
                if (!field.isInternalCollectionType()) {
                    newSortableFields.add(field);
                }
            }

            sortableFields = newSortableFields;
        }

        return sortableFields;
    }

    public ObjectField getSortField() {
        if (sortField == null) {
            ObjectType type = getQueryType();
            ObjectField newSortField = null;

            for (ObjectField field : getSortableFields()) {
                if (field.getInternalName().equals(getSortFieldName())) {
                    newSortField = field;
                    break;
                }
            }

            if (newSortField == null) {
                LABEL_FIELD: for (String labelField : type.getLabelFields()) {
                    for (ObjectField field : getSortableFields()) {
                        if (field.getInternalName().equals(labelField)) {
                            newSortField = field;
                            break LABEL_FIELD;
                        }
                    }
                }
            }

            if (newSortField == null) {
                newSortField = type.getState().getDatabase().getEnvironment().getField("cms.content.publishDate");
            }

            sortField = newSortField;
        }

        return sortField;
    }

    public Query<?> toQuery() {
        State state = getState();
        Query<?> query = Query.fromType(getQueryType());
        Predicate predicate = query.getPredicate();

        for (ObjectField field : getIndexedFields()) {
            String name = field.getInternalName();
            Object value = state.get(FIELD_PREFIX + name);

            if (!ObjectUtils.isBlank(value)) {
                String type = field.getInternalItemType();
                String operator = (String) state.get(OPERATOR_PREFIX + name);

                if (operator == null) {
                    operator = ObjectField.REFERENTIAL_TEXT_TYPE.equals(type) || ObjectField.TEXT_TYPE.equals(type) ? "matchesAll" : "equalsAny";
                }

                predicate = CompoundPredicate.combine(
                        PredicateParser.AND_OPERATOR,
                        predicate,
                        PredicateParser.Static.parse(name + " " + operator + " ?", value));
            }
        }

        query.setPredicate(predicate);

        ObjectField sortField = getSortField();
        if (ObjectField.DATE_TYPE.equals(sortField.getInternalItemType())) {
            query.sortDescending(sortField.getInternalName());
        } else {
            query.sortAscending(sortField.getInternalName());
        }

        return query;
    }

    public Query<?> toPreviousQuery(State state) {
        String sortFieldName = getSortFieldName();
        Query<?> query = toQuery().and(sortFieldName + " <= ? and _id != ?", state.get(sortFieldName), state.getId());

        for (ListIterator<Sorter> i = query.getSorters().listIterator(); i.hasNext();) {
            Sorter sorter = i.next();
            if (Sorter.ASCENDING_OPERATOR.equals(sorter.getOperator())) {
                List<Object> options = sorter.getOptions();
                if (options != null
                        && options.size() > 0
                        && sortFieldName.equals(options.get(0))) {
                    i.set(new Sorter(Sorter.DESCENDING_OPERATOR, options));
                }
            }
        }

        return query;
    }

    public Query<?> toNextQuery(State state) {
        String sortFieldName = getSortFieldName();
        return toQuery().and(sortFieldName + " >= ? and _id != ?", state.get(sortFieldName), state.getId());
    }
}
