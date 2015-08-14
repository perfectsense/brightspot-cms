package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.psddev.cms.db.Taxon;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ComparisonPredicate;
import com.psddev.dari.db.CompoundPredicate;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectFieldComparator;
import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.db.ObjectStruct;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Singleton;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;

@ToolUi.Hidden
public abstract class SearchAdvancedPredicate extends Record implements Singleton {

    @Indexed(unique = true)
    @Required
    protected String parameterValue;

    public String getParameterValue() {
        return parameterValue;
    }

    public abstract String getLabel();

    public abstract Predicate writeInputs(
            SearchAdvancedQuery servlet,
            ToolPageContext page,
            List<String> paramNames,
            String predicateParam,
            String paramPrefix)
            throws IOException;

    public abstract static class Compound extends SearchAdvancedPredicate {

        public abstract String getOperator();

        @Override
        public Predicate writeInputs(
                SearchAdvancedQuery servlet,
                ToolPageContext page,
                List<String> paramNames,
                String predicateParam,
                String paramPrefix)
                throws IOException {

            Predicate predicate = null;
            String subPredicateIndexParam = paramPrefix + ".p";
            int lastSubPredicateIndex = -1;

            for (String paramName : paramNames) {
                if (paramName.startsWith(subPredicateIndexParam)) {
                    Integer subPredicateIndex = ObjectUtils.to(Integer.class, paramName.substring(subPredicateIndexParam.length()));

                    if (subPredicateIndex != null) {
                        if (lastSubPredicateIndex < subPredicateIndex) {
                            lastSubPredicateIndex = subPredicateIndex;
                        }
                    }
                }
            }

            page.writeStart("button",
                    "class", "icon icon-action-add link",
                    "name", paramPrefix + ".p" + (lastSubPredicateIndex + 1),
                    "value", 1);
                page.writeHtml("Add Another ");
                page.writeHtml(getLabel());
            page.writeEnd();

            page.writeStart("ul");
                for (String paramName : paramNames) {
                    if (paramName.startsWith(subPredicateIndexParam)) {
                        Integer subPredicateIndex = ObjectUtils.to(Integer.class, paramName.substring(subPredicateIndexParam.length()));

                        if (subPredicateIndex != null) {
                            page.writeStart("li");
                                predicate = CompoundPredicate.combine(
                                        getOperator(),
                                        predicate,
                                        servlet.writeSearchAdvancedPredicate(page, paramNames, paramName, paramPrefix + "." + subPredicateIndex));
                            page.writeEnd();
                        }
                    }
                }
            page.writeEnd();

            return predicate;
        }
    }

    public static class And extends Compound {

        public And() {
            this.parameterValue = "A";
        }

        @Override
        public String getLabel() {
            return "Match All (AND)";
        }

        @Override
        public String getOperator() {
            return "AND";
        }
    }

    public static class Or extends Compound {

        public Or() {
            this.parameterValue = "O";
        }

        @Override
        public String getLabel() {
            return "Match Any (OR)";
        }

        @Override
        public String getOperator() {
            return "OR";
        }
    }

    public static class Not extends Compound {

        public Not() {
            this.parameterValue = "N";
        }

        @Override
        public String getLabel() {
            return "Match None (NOT)";
        }

        @Override
        public String getOperator() {
            return "NOT";
        }
    }

    public static class Comparison extends SearchAdvancedPredicate {

        public Comparison() {
            this.parameterValue = "C";
        }

        @Override
        public String getLabel() {
            return "Comparison";
        }

        @Override
        public Predicate writeInputs(
                SearchAdvancedQuery servlet,
                ToolPageContext page,
                List<String> paramNames,
                String predicateParam,
                String paramPrefix)
                throws IOException {

            String comparisonTypeParam = paramPrefix + ".ct";
            String comparisonPathParam = paramPrefix + ".cp";
            String comparisonOperatorParam = paramPrefix + ".co";
            String comparisonValueParam = paramPrefix + ".cv";
            DatabaseEnvironment environment = Database.Static.getDefault().getEnvironment();
            Set<UUID> comparisonTypeIds = new HashSet<UUID>();
            Set<ObjectType> comparisonTypes = new HashSet<ObjectType>();

            for (UUID id : page.params(UUID.class, comparisonTypeParam)) {
                comparisonTypeIds.add(id);
                comparisonTypes.add(ObjectType.getInstance(id));
            }

            String comparisonPath = page.param(String.class, comparisonPathParam);
            ComparisonOperator comparisonOperator = page.param(ComparisonOperator.class, comparisonOperatorParam);
            PathedField comparisonPathedField = null;
            ObjectField comparisonField = null;

            page.writeHtml(" ");
            page.writeMultipleTypeSelect(
                    null,
                    comparisonTypes,
                    "name", comparisonTypeParam,
                    "placeholder", "Any Types",
                    "data-bsp-autosubmit", "",
                    "data-searchable", true);

            page.writeHtml(" ");
            page.writeStart("select",
                    "name", comparisonPathParam,
                    "data-bsp-autosubmit", "",
                    "data-searchable", true);
                page.writeStart("option", "value", "");
                    page.writeHtml("Any Fields");
                page.writeEnd();

                if (!comparisonTypes.isEmpty()) {
                    Set<PathedField> pathedFields = null;

                    for (ObjectType t : comparisonTypes) {
                        Set<PathedField> pf = getPathedFields(t);

                        if (pathedFields == null) {
                            pathedFields = pf;

                        } else {
                            pathedFields.retainAll(pf);
                        }
                    }

                    if (!pathedFields.isEmpty()) {
                        page.writeStart("optgroup", "label", "Type-Specific Fields");
                            for (PathedField pf : pathedFields) {
                                String path = pf.getPath();

                                if (comparisonField == null
                                        && path.equals(comparisonPath)) {
                                    List<ObjectField> pfs = pf.getFields();

                                    if (!pfs.isEmpty()) {
                                        comparisonPathedField = pf;
                                        comparisonField = pfs.get(pfs.size() - 1);
                                    }
                                }

                                page.writeStart("option",
                                        "selected", path.equals(comparisonPath) ? "selected" : null,
                                        "value", path);
                                    page.writeHtml(pf.getDisplayName());
                                page.writeEnd();
                            }
                        page.writeEnd();
                    }
                }

                page.writeStart("optgroup", "label", "Global Fields");
                    for (PathedField pf : getPathedFields(environment)) {
                        String path = pf.getPath();

                        if (comparisonField == null
                                && path.equals(comparisonPath)) {
                            List<ObjectField> pfs = pf.getFields();

                            if (!pfs.isEmpty()) {
                                comparisonPathedField = pf;
                                comparisonField = pfs.get(pfs.size() - 1);
                            }
                        }

                        page.writeStart("option",
                                "selected", path.equals(comparisonPath) ? "selected" : null,
                                "value", path);
                            page.writeHtml(pf.getDisplayName());
                        page.writeEnd();
                    }
                page.writeEnd();
            page.writeEnd();

            page.writeHtml(" ");
            page.writeStart("select",
                    "data-bsp-autosubmit", "",
                    "name", comparisonOperatorParam);
                for (ComparisonOperator op : ComparisonOperator.values()) {
                    if ((ObjectUtils.isBlank(comparisonPath)
                            && !op.equals(ComparisonOperator.M))
                            || (!ObjectUtils.isBlank(comparisonPath)
                            && !op.isDisplayedFor(comparisonField))) {
                        if (op.equals(comparisonOperator)) {
                            comparisonOperator = null;
                        }

                        continue;
                    }

                    if (comparisonOperator == null) {
                        comparisonOperator = op;
                    }

                    page.writeStart("option",
                            "selected", op.equals(comparisonOperator) ? "selected" : null,
                            "value", op.name());
                        page.writeHtml(op.getLabel());
                        page.writeHtml(":");
                    page.writeEnd();
                }
            page.writeEnd();

            page.writeHtml(" ");
            comparisonOperator.writeValueInputs(page, comparisonValueParam, comparisonField);

            return CompoundPredicate.combine(
                    "AND",
                    comparisonTypeIds.isEmpty() ? null : new ComparisonPredicate("=", false, "_type", comparisonTypeIds),
                    comparisonOperator.createPredicate(page, comparisonValueParam, comparisonPathedField));
        }

        private Set<PathedField> getPathedFields(ObjectStruct struct) {
            Set<PathedField> pathedFields = new TreeSet<PathedField>();

            addPathedFields(pathedFields, null, struct);
            return pathedFields;
        }

        private void addPathedFields(Set<PathedField> pathedFields, List<ObjectField> prefix, ObjectStruct struct) {
            List<ObjectField> fields = struct.getFields();
            Set<String> indexedFields = new HashSet<String>();

            for (ObjectIndex index : struct.getIndexes()) {
                indexedFields.addAll(index.getFields());
            }

            for (Iterator<ObjectField> i = fields.iterator(); i.hasNext();) {
                ObjectField field = i.next();
                String declaring = field.getJavaDeclaringClassName();

                if (declaring != null
                        && declaring.startsWith("com.psddev.dari.db.")) {
                    continue;
                }

                String fieldName = field.getInternalName();
                boolean embedded = field.isEmbedded();

                if (!embedded
                        && ObjectField.RECORD_TYPE.equals(field.getInternalItemType())) {
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
                        addPathedFields(pathedFields, copyConcatenate(prefix, field), t);
                    }

                } else if (indexedFields.contains(fieldName)
                        && !field.isDeprecated()
                        && !field.as(ToolUi.class).isHidden()) {
                    pathedFields.add(new PathedField(copyConcatenate(prefix, field)));
                }
            }
        }

        private static <T> List<T> copyConcatenate(List<T> list, T item) {
            list = list != null ? new ArrayList<T>(list) : new ArrayList<T>();

            list.add(item);
            return list;
        }

        private static class PathedField implements Comparable<PathedField> {

            private final List<ObjectField> fields;
            private final String path;
            private final String displayName;

            public PathedField(List<ObjectField> fields) {
                this.fields = Collections.unmodifiableList(fields);

                StringBuilder path = new StringBuilder();

                for (ObjectField f : getFields()) {
                    path.append(f.getInternalName());
                    path.append('/');
                }

                path.setLength(path.length() - 1);

                this.path = path.toString();

                StringBuilder displayName = new StringBuilder();

                for (ObjectField f : getFields()) {
                    displayName.append(f.getDisplayName());
                    displayName.append(" \u2192 ");
                }

                displayName.setLength(displayName.length() - 3);

                this.displayName = displayName.toString();
            }

            public List<ObjectField> getFields() {
                return fields;
            }

            public String getPath() {
                return path;
            }

            public String getDisplayName() {
                return displayName;
            }

            @Override
            public int compareTo(PathedField other) {
                return getDisplayName().compareTo(other.getDisplayName());
            }

            @Override
            public int hashCode() {
                return getPath().hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;

                } else if (other instanceof PathedField) {
                    return getPath().equals(((PathedField) other).getPath());

                } else {
                    return false;
                }
            }
        }

        private enum TaxonOption {
            C("Or Its Children"),
            D("Or Its Descendants");

            private final String label;

            private TaxonOption(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }

        private enum ComparisonOperator {

            M("Contains Text") {

                @Override
                public boolean isDisplayedFor(ObjectField field) {
                    String t = field.getInternalItemType();

                    return ObjectField.REFERENTIAL_TEXT_TYPE.equals(t)
                            || ObjectField.TEXT_TYPE.equals(t);
                }

                @Override
                public void writeValueInputs(
                        ToolPageContext page,
                        String valueParam,
                        ObjectField field)
                        throws IOException {

                    page.writeElement("input",
                            "type", "text",
                            "name", valueParam,
                            "value", page.param(String.class, valueParam));
                }

                @Override
                public Predicate createPredicate(
                        ToolPageContext page,
                        String valueParam,
                        PathedField pathedField) {

                    return createPredicateWithOperatorAndValue(
                            pathedField,
                            "matches",
                            page.param(String.class, valueParam));
                }
            },

            I("Is") {

                @Override
                public boolean isDisplayedFor(ObjectField field) {
                    String t = field.getInternalItemType();

                    return ObjectField.NUMBER_TYPE.equals(t)
                            || ObjectField.RECORD_TYPE.equals(t)
                            || ObjectField.TEXT_TYPE.equals(t);
                }

                @Override
                public void writeValueInputs(
                        ToolPageContext page,
                        String valueParam,
                        ObjectField field)
                        throws IOException {

                    if (ObjectField.RECORD_TYPE.equals(field.getInternalItemType())) {
                        List<Object> selected = Query
                                .fromAll()
                                .where("_id = ?", page.params(UUID.class, valueParam))
                                .selectAll();

                        if (page.isObjectSelectDropDown(field)) {
                            List<?> items = new Search(field).toQuery(page.getSite()).selectAll();

                            Collections.sort(items, new ObjectFieldComparator("_label", false));

                            page.writeStart("select",
                                    "name", valueParam,
                                    "multiple", "multiple",
                                    "data-searchable", "true");

                                for (Object item : items) {
                                    State itemState = State.getInstance(item);

                                    page.writeStart("option",
                                            "selected", selected.contains(item) ? "selected" : null,
                                            "value", itemState.getId());
                                        page.writeObjectLabel(item);
                                    page.writeEnd();
                                }
                            page.writeEnd();

                        } else {
                            if (selected.isEmpty()) {
                                selected.add(null);
                            }

                            String taxonParam = valueParam + "x";
                            List<TaxonOption> taxonOptions = page.params(TaxonOption.class, taxonParam);
                            boolean taxon = false;

                            for (ObjectType t : field.getTypes()) {
                                if (t.getGroups().contains(Taxon.class.getName())) {
                                    taxon = true;
                                    break;
                                }
                            }

                            page.writeStart("div",
                                    "class", "repeatableObjectId",
                                    "style", "overflow:hidden;");

                                page.writeStart("ul");
                                    for (int i = 0, size = selected.size(); i < size; ++ i) {
                                        Object item = selected.get(i);

                                        page.writeStart("li");
                                            page.writeObjectSelect(
                                                    field,
                                                    item,
                                                    "name", valueParam);

                                            if (taxon) {
                                                TaxonOption taxonOption = i < taxonOptions.size() ? taxonOptions.get(i) : null;

                                                page.writeHtml(" ");
                                                page.writeStart("select",
                                                        "name", taxonParam);
                                                    page.writeStart("option");
                                                        page.writeHtml("Only");
                                                    page.writeEnd();

                                                    for (TaxonOption o : TaxonOption.values()) {
                                                        page.writeStart("option",
                                                                "selected", o.equals(taxonOption) ? "selected" : null,
                                                                "value", o.name());
                                                            page.writeHtml(o.getLabel());
                                                        page.writeEnd();
                                                    }
                                                page.writeEnd();
                                            }
                                        page.writeEnd();
                                    }

                                    page.writeStart("script", "type", "text/template");
                                        page.writeStart("li");
                                            page.writeObjectSelect(
                                                    field,
                                                    null,
                                                    "name", valueParam);

                                            if (taxon) {
                                                page.writeHtml(" ");
                                                page.writeStart("select",
                                                        "name", taxonParam);
                                                    page.writeStart("option");
                                                        page.writeHtml("Only");
                                                    page.writeEnd();

                                                    for (TaxonOption o : TaxonOption.values()) {
                                                        page.writeStart("option", "value", o.name());
                                                            page.writeHtml(o.getLabel());
                                                        page.writeEnd();
                                                    }
                                                page.writeEnd();
                                            }
                                        page.writeEnd();
                                    page.writeEnd();
                                page.writeEnd();
                            page.writeEnd();
                        }

                    } else {
                        page.writeElement("input",
                                "type", "text",
                                "name", valueParam,
                                "value", page.param(String.class, valueParam));
                    }
                }

                @Override
                public Predicate createPredicate(
                        ToolPageContext page,
                        String valueParam,
                        PathedField pathedField) {

                    TaxonOption taxonOption = page.param(TaxonOption.class, valueParam + "x");

                    if (taxonOption != null) {
                        Taxon top = Query.from(Taxon.class).where("_id = ?", page.param(UUID.class, valueParam)).first();
                        Set<UUID> values = new HashSet<UUID>();

                        if (top != null) {
                            values.add(top.getState().getId());

                            if (taxonOption.equals(TaxonOption.D)) {
                                addChildren(values, top);

                            } else {
                                for (Taxon c : top.getChildren()) {
                                    values.add(c.getState().getId());
                                }
                            }
                        }

                        return createPredicateWithOperatorAndValue(
                                pathedField,
                                "=",
                                values);

                    } else {
                        return createPredicateWithOperatorAndValue(
                                pathedField,
                                "=",
                                page.params(String.class, valueParam));
                    }
                }

                private void addChildren(Set<UUID> values, Taxon parent) {
                    for (Taxon c : parent.getChildren()) {
                        values.add(c.getState().getId());
                        addChildren(values, c);
                    }
                }
            },

            N("Is Not") {

                @Override
                public boolean isDisplayedFor(ObjectField field) {
                    return I.isDisplayedFor(field);
                }

                @Override
                public void writeValueInputs(
                        ToolPageContext page,
                        String valueParam,
                        ObjectField field)
                        throws IOException {
                    I.writeValueInputs(page, valueParam, field);
                }

                @Override
                public Predicate createPredicate(
                        ToolPageContext page,
                        String valueParam,
                        PathedField pathedField) {

                    return createPredicateWithOperatorAndValue(
                            pathedField,
                            "!=",
                            page.param(String.class, valueParam));
                }
            },

            T("Is Set") {

                @Override
                public boolean isDisplayedFor(ObjectField field) {
                    return ObjectField.BOOLEAN_TYPE.equals(field.getInternalType());
                }

                @Override
                public void writeValueInputs(
                        ToolPageContext page,
                        String valueParam,
                        ObjectField field)
                        throws IOException {
                }

                @Override
                public Predicate createPredicate(
                        ToolPageContext page,
                        String valueParam,
                        PathedField pathedField) {

                    return createPredicateWithOperatorAndValue(
                            pathedField,
                            "=",
                            Boolean.TRUE);
                }
            },

            F("Is Not Set") {

                @Override
                public boolean isDisplayedFor(ObjectField field) {
                    return T.isDisplayedFor(field);
                }

                @Override
                public void writeValueInputs(
                        ToolPageContext page,
                        String valueParam,
                        ObjectField field)
                        throws IOException {
                }

                @Override
                public Predicate createPredicate(
                        ToolPageContext page,
                        String valueParam,
                        PathedField pathedField) {

                    return createPredicateWithOperatorAndValue(
                            pathedField,
                            "!=",
                            Boolean.TRUE);
                }
            },

            S("Is Missing") {

                @Override
                public boolean isDisplayedFor(ObjectField field) {
                    return !ObjectField.BOOLEAN_TYPE.equals(field.getInternalType());
                }

                @Override
                public void writeValueInputs(
                        ToolPageContext page,
                        String valueParam,
                        ObjectField field)
                        throws IOException {
                }

                @Override
                public Predicate createPredicate(
                        ToolPageContext page,
                        String valueParam,
                        PathedField pathedField) {

                    return createPredicateWithOperatorAndValue(
                            pathedField,
                            "=",
                            Query.MISSING_VALUE);
                }
            },

            G("Is Not Missing") {

                @Override
                public boolean isDisplayedFor(ObjectField field) {
                    return S.isDisplayedFor(field);
                }

                @Override
                public void writeValueInputs(
                        ToolPageContext page,
                        String valueParam,
                        ObjectField field)
                        throws IOException {
                }

                @Override
                public Predicate createPredicate(
                        ToolPageContext page,
                        String valueParam,
                        PathedField pathedField) {

                    return createPredicateWithOperatorAndValue(
                            pathedField,
                            "!=",
                            Query.MISSING_VALUE);
                }
            },

            B("Between") {

                @Override
                public boolean isDisplayedFor(ObjectField field) {
                    String t = field.getInternalItemType();

                    return ObjectField.DATE_TYPE.equals(t)
                            || ObjectField.NUMBER_TYPE.equals(t);
                }

                @Override
                public void writeValueInputs(
                        ToolPageContext page,
                        String valueParam,
                        ObjectField field)
                        throws IOException {

                    boolean date = ObjectField.DATE_TYPE.equals(field.getInternalItemType());
                    String valueToParam = valueParam + "t";

                    page.writeElement("input",
                            "type", "text",
                            "class", date ? "date" : null,
                            "name", valueParam,
                            "value", page.param(String.class, valueParam));

                    page.writeHtml(" ");
                    page.writeElement("input",
                            "type", "text",
                            "class", date ? "date" : null,
                            "name", valueToParam,
                            "value", page.param(String.class, valueToParam));
                }

                @Override
                public Predicate createPredicate(
                        ToolPageContext page,
                        String valueParam,
                        PathedField pathedField) {

                    List<ObjectField> fields = pathedField.getFields();
                    boolean date = ObjectField.DATE_TYPE.equals(fields.get(fields.size() - 1).getInternalItemType());
                    Object from = page.param(date ? Date.class : Double.class, valueParam);
                    Object to = page.param(date ? Date.class : Double.class, valueParam + "t");

                    return CompoundPredicate.combine(
                            "AND",
                            from == null ? null : createPredicateWithOperatorAndValue(pathedField, ">=", from),
                            to == null ? null : createPredicateWithOperatorAndValue(pathedField, "<=", to));
                }
            };

            private final String label;

            private ComparisonOperator(String label) {
                this.label = label;
            }

            public abstract boolean isDisplayedFor(ObjectField field);

            public abstract void writeValueInputs(
                    ToolPageContext page,
                    String valueParam,
                    ObjectField field)
                    throws IOException;

            public abstract Predicate createPredicate(
                    ToolPageContext page,
                    String valueParam,
                    PathedField pathedField);

            public String getLabel() {
                return label;
            }

            public Predicate createPredicateWithOperatorAndValue(
                    PathedField pathedField,
                    String operator,
                    Object value) {

                if (value == null) {
                    return null;

                } else {
                    StringBuilder key = new StringBuilder();

                    if (pathedField == null) {
                        key.append("_any");

                    } else {
                        for (ObjectField f : pathedField.getFields()) {
                            if (key.length() == 0) {
                                key.append(f.getUniqueName());

                            } else {
                                key.append('/');
                                key.append(f.getInternalName());
                            }
                        }
                    }

                    return new ComparisonPredicate(
                            operator,
                            false,
                            key.toString(),
                            value instanceof Iterable ? (Iterable<?>) value : Arrays.asList(value));
                }
            }
        }
    }
}
