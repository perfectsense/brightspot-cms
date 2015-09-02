<%@ page session="false" import="

com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.State,

com.psddev.dari.util.ObjectUtils
" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Set" %>
<%@ page import="com.psddev.dari.db.ObjectType" %>
<%@ page import="com.psddev.dari.db.Query" %>
<%@ page import="java.util.UUID" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));

ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();
Map<String, Object> fieldValue = (Map<String, Object>) state.getValue(fieldName);
if(fieldValue == null) {
    fieldValue = new HashMap<String, Object>();
}

List<ObjectType> validTypes = field.as(ToolUi.class).findDisplayTypes();
boolean isValueExternal = ToolUi.isValueExternal(field);

String inputName = (String) request.getAttribute("inputName");
String idName = inputName + ".id";
String typeIdName = inputName + ".typeId";
String keyName = inputName + ".key";

if ((Boolean) request.getAttribute("isFormPost")) {

    fieldValue.clear();

    if (!isValueExternal) {
        UUID[] ids = wp.uuidParams(idName);
        UUID[] typeIds = wp.uuidParams(typeIdName);
        String[] keys = wp.params(keyName);
        for (int i = 0, s = Math.min(Math.min(ids.length, typeIds.length), keys.length); i < s; ++ i) {
            ObjectType type = ObjectType.getInstance(typeIds[i]);
            Object item = type.createObject(null);
            State.getInstance(item).setId(ids[i]);
            wp.updateUsingParameters(item);
            fieldValue.put(keys[i], item);
        }

    } else {
        UUID[] ids = wp.uuidParams(inputName);
        String[] keys = wp.params(keyName);
        for(int i = 0, s = Math.min(ids.length, keys.length); i < s; ++ i) {
            Object item = Query.fromAll().where("_id = ?", ids[i]).resolveInvisible().first();
            if (item != null) {
                fieldValue.put(keys[i], item);
            }
        }
    }

    state.putValue(fieldName, fieldValue);
    return;
}

// --- Presentation ---

%><% if (!isValueExternal) { %>
    <div class="inputLarge repeatableForm">
        <ul>
            <%
            for (Map.Entry<String, Object> entry : fieldValue.entrySet()) {
                Object item = entry.getValue();
                State itemState = State.getInstance(item);
                ObjectType itemType = itemState.getType();
                %>
                <li data-type="<%= wp.objectLabel(itemType) %>" data-label=" ">
                    <div class="repeatableLabel inputSmall">
                        <textArea name="<%= keyName %>"><%= entry.getKey() %></textArea>
                    </div>
                    <input name="<%= wp.h(idName) %>" type="hidden" value="<%= itemState.getId() %>">
                    <input name="<%= wp.h(typeIdName) %>" type="hidden" value="<%= itemType.getId() %>">
                    <% wp.writeFormFields(item); %>
                </li>
            <% } %>
            <% for (ObjectType type : validTypes) { %>
                <script type="text/template">
                    <li data-type="<%= wp.objectLabel(type) %>">
                        <div class="repeatableLabel inputSmall">
                            <textArea name="<%= keyName %>" value=""></textArea>
                        </div>
                        <div class="frame">
                            <a href="<%= wp.cmsUrl("/content/repeatableObject.jsp", "inputName", inputName, "typeId", type.getId()) %>"></a>
                        </div>
                    </li>
                </script>
            <% } %>
        </ul>
    </div>
<% } else {

    Set<ObjectType> valueTypes = field.getTypes();
    String validTypeIds;
    if (ObjectUtils.isBlank(valueTypes)) {
        validTypeIds = "";
    } else {
        StringBuilder sb = new StringBuilder();
        for (ObjectType type : valueTypes) {
            sb.append(type.getId()).append(",");
        }
        sb.setLength(sb.length() - 1);
        validTypeIds = sb.toString();
    }
    %>

    <div class="inputSmall repeatableObjectId">
        <textarea class="json" id="<%= wp.getId() %>" name="<%= wp.h(inputName) %>"><%= wp.h(ObjectUtils.toJson(fieldValue, true)) %></textarea>
    </div>

<% } %>
