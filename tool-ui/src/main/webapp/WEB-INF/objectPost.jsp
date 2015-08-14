<%@ page session="false" import="

com.psddev.cms.db.Content,
com.psddev.cms.db.ToolUser,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Query,
com.psddev.dari.db.State,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.TypeReference,

java.util.ArrayList,
java.util.Collection,
java.util.Date,
java.util.List,
java.util.Map
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
Object object = request.getAttribute("object");
Collection<String> includeFields = ObjectUtils.to(new TypeReference<Collection<String>>() { }, request.getAttribute("includeFields"));
Collection<String> excludeFields = ObjectUtils.to(new TypeReference<Collection<String>>() { }, request.getAttribute("excludeFields"));
State state = State.getInstance(object);
ObjectType type = state.getType();
List<ObjectField> fields = new ArrayList<>();

if (type != null) {
    fields.addAll(type.getFields());
}

if (wp.param(boolean.class, state.getId() + "/_includeGlobals") && !fields.isEmpty()) {
    for (ObjectField field : state.getDatabase().getEnvironment().getFields()) {
        if (Boolean.FALSE.equals(field.getState().get("cms.ui.hidden"))) {
            fields.add(field);
        }
    }
}

if (object instanceof Query) {
    state.clear();
    state.putAll((Map<String, Object>) ObjectUtils.fromJson(wp.param(String.class, state.getId() + "/_query")));

} else if (fields != null) {
    Object oldContainer = request.getAttribute("containerObject");
    boolean draftCheck = false;

    try {
        if (oldContainer == null) {
            request.setAttribute("containerObject", object);
        }

        if (request.getAttribute("firstDraft") == null) {
            draftCheck = true;
            request.setAttribute("firstDraft", state.isNew());
            request.setAttribute("finalDraft", state.isNew() || state.isVisible());
        }

        for (ObjectField field : fields) {
            String name = field.getInternalName();

            if ((includeFields == null ||
                    includeFields.contains(name)) &&
                    (excludeFields == null ||
                    !excludeFields.contains(name))) {
                wp.processField(object, field);
            }
        }

    } finally {
        if (oldContainer == null) {
            request.setAttribute("containerObject", null);
        }

        if (draftCheck) {
            request.setAttribute("firstDraft", null);
            request.setAttribute("finalDraft", null);
        }
    }

} else {
    state.setJsonString(wp.param("data", ""));
}
%>
