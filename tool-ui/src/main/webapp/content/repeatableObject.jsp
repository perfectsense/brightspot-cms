<%@ page session="false" import="

com.psddev.cms.db.ContentTemplate,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Query,
com.psddev.dari.db.State,

java.util.UUID
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requireUser()) {
    return;
}

String inputName = wp.param("inputName");

ObjectType type;
Object object;
UUID typeId = wp.param(UUID.class, "typeId");
ContentTemplate template = Query.from(ContentTemplate.class)
        .where("_id = ?", typeId)
        .first();

if (template != null) {
    type = template.getTemplateType();
    object = template.getTemplate();

} else {
    type = ObjectType.getInstance(typeId);
    object = type.createObject(null);
}

State objectState = State.getInstance(object);

objectState.setId(null);

// --- Presentation ---

%><input type="hidden" name="<%= wp.h(inputName) %>.id" value="<%= objectState.getId() %>" />
<input type="hidden" name="<%= wp.h(inputName) %>.typeId" value="<%= type.getId() %>" />
<input type="hidden" name="<%= wp.h(inputName) %>.publishDate" value="" />
<input type="hidden" name="<%= wp.h(inputName) %>.data" value="" />
<% wp.writeFormFields(object); %>
