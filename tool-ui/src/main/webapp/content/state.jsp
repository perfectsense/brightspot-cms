<%@ page session="false" import="

com.psddev.cms.db.ContentSection,
com.psddev.cms.db.Page,
com.psddev.cms.db.Section,
com.psddev.cms.db.Template,
com.psddev.cms.tool.ToolPageContext,
com.psddev.cms.tool.page.content.Edit,

com.psddev.dari.db.Query,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.State,

com.psddev.dari.util.ObjectUtils,

java.util.Map,
java.util.Set,
java.util.UUID
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requireUser()) {
    return;
}

Object selected = wp.findOrReserve();
State state = State.getInstance(selected);

Template template = null;
if (selected != null) {
    template = state.as(Template.ObjectModification.class).getDefault();
}

if (selected == null) {
    return;
}

Object editing = selected;
if (selected instanceof Page) {
    Object sectionContent = Query.findById(Object.class, wp.uuidParam("contentId"));
    if (sectionContent != null) {
        editing = sectionContent;
    }
}

State editingState = State.getInstance(editing);
try {
    editingState.beginWrites();
    wp.updateUsingParameters(editing);
    Edit.updateUsingWidgets(wp, editing);
    wp.publish(editing);
} catch (Exception error) {
} finally {
    editingState.endWrites();
}

// --- Presentation ---

response.setContentType("application/json");
Map<String, Object> data = (Map<String, Object>) ObjectUtils.fromJson(editingState.getJsonString());
data.put("_id", editingState.getId().toString());
data.put("_typeId", editingState.getTypeId().toString());
wp.write(ObjectUtils.toJson(data));
%>
