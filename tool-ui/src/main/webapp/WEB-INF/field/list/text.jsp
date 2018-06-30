<%@ page session="false" import="

com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.State,

com.psddev.dari.util.ObjectUtils,

java.util.ArrayList,
java.util.List
" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Set" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));

ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();
List<Object> fieldValue;
Object fieldValueObject = state.getValue(fieldName);
if(fieldValueObject instanceof List) {
    fieldValue = (List<Object>) fieldValueObject;
} else if(fieldValueObject instanceof Collection) {
    fieldValue = new ArrayList<Object>((Collection<Object>) fieldValueObject);
} else {
    fieldValue = null;
}

Set<ObjectField.Value> validValues = field.getValues();

String inputName = (String) request.getAttribute("inputName");
String textName = inputName + ".text";
String toggleName = inputName + ".toggle";

if ((Boolean) request.getAttribute("isFormPost")) {
    fieldValue = new ArrayList<Object>();

    if(ObjectUtils.isBlank(validValues)) {
        String[] texts = wp.params(textName);
        String[] toggles = wp.params(toggleName);

        for (int i = 0, s = Math.min(texts.length, toggles.length); i < s; ++ i) {
            String text = texts[i];
            if (Boolean.parseBoolean(toggles[i]) && !ObjectUtils.isBlank(text)) {
                fieldValue.add(text);
            }
        }

    } else {
        for (String text : wp.params(textName)) {
            if (!ObjectUtils.isBlank(text)) {
                fieldValue.add(text);
            }
        }
    }

    state.putValue(fieldName, fieldValue);
    return;
}

// --- Presentation ---

ToolUi ui = field.as(ToolUi.class);
%><% if (ObjectUtils.isBlank(validValues)) { %>
    <div class="inputSmall repeatableText <%=ui.isRichText() ? "usingTextAreas" : ""%>">
        <ol>
            <% if (fieldValue!= null) {%>
                <% if (!ui.isRichText()) { %>
                    <% for (Object text : fieldValue) { %>
                        <li>
                            <input checked name="<%= wp.h(toggleName) %>" type="checkbox" value="true">
                            <input class="expandable" name="<%= wp.h(textName) %>" type="text" value="<%= wp.h(text) %>">
                        </li>
                    <% } %>
                <% } else {
                    for (Object text : fieldValue) {
                        wp.write("<li><div class=\"inputSmall inputSmall-text\">");

                        if (validValues != null) {
                            wp.write("<select id=\"", wp.getId(), "\" name=\"", wp.h(inputName), "\">");
                            wp.write("<option value=\"\">");
                            wp.write("</option>");
                            for (ObjectField.Value value : validValues) {
                                wp.write("<option");
                                if (ObjectUtils.equals(value.getValue(), text)) {
                                    wp.write(" selected");
                                }
                                wp.write(" value=\"", wp.h(value.getValue()), "\">");
                                wp.write(wp.h(value.getLabel()));
                                wp.write("</option>");
                            }
                            wp.write("</select>");

                        } else if (ui.isColorPicker()) {
                            wp.writeElement("input",
                                    "type", "text",
                                    "class", "color",
                                    "name", wp.h(textName),
                                    "value", text);

                        } else if (ui.isSecret()) {
                            wp.writeElement("input",
                                    "type", "password",
                                    "id", wp.getId(),
                                    "name", wp.h(textName),
                                    "value", text);

                        } else {
                            wp.writeStart("textarea",
                                    "class", ui.isRichText() ? "richtext" : null,
                                    "id", wp.getId(),
                                    "name", wp.h(textName),
                                    "data-dynamic-field-name", field.getInternalName(),
                                    "data-code-type", ui.getCodeType(),
                                    "data-inline", true,
                                    "data-user", wp.getObjectLabel(wp.getUser()),
                                    "data-user-id", wp.getUser() != null ? wp.getUser().getId() : null,
                                    "data-first-draft", Boolean.TRUE.equals(request.getAttribute("firstDraft")),
                                    "data-track-changes", !Boolean.TRUE.equals(request.getAttribute("finalDraft")));
                            wp.writeHtml(text);
                            wp.writeEnd();
                        }

                        wp.write("<input name=\"" + wp.h(toggleName) + "\" type=\"hidden\" value=\"true\">");
                        wp.write("</div></li>");
                    }
                }
            }
            %>
            <script type="text/template">
                <li><%
                    if (!ui.isRichText()) {
                        %>
                            <input name="<%= wp.h(toggleName) %>" type="checkbox" value="true">
                            <input class="expandable" name="<%= wp.h(textName) %>" type="text">
                        <%
                    } else {
                        %>
                            <div class="inputSmall inputSmall-text">
                                <input name="<%= wp.h(toggleName) %>" type="hidden" value="true">
                                <%
                                    wp.writeStart("textarea",
                                        "class", "expandable " + (ui.isRichText() ? "richtext" : null),
                                        "id", wp.getId(),
                                        "name", wp.h(textName),
                                        "data-dynamic-field-name", field.getInternalName(),
                                        "data-code-type", ui.getCodeType(),
                                        "data-inline", true,
                                        "data-user", wp.getObjectLabel(wp.getUser()),
                                        "data-user-id", wp.getUser() != null ? wp.getUser().getId() : null,
                                        "data-first-draft", Boolean.TRUE.equals(request.getAttribute("firstDraft")),
                                        "data-track-changes", !Boolean.TRUE.equals(request.getAttribute("finalDraft")));
                                    wp.writeEnd();
                                %>
                            </div>
                    <%}%>
                </li>
            </script>
        </ol>
    </div>
<% } else { %>
    <div class="inputSmall">
        <select multiple name="<%= wp.h(textName) %>">
            <% for (ObjectField.Value value : validValues) { %>
                <%
                boolean containsValue = false;
                if (fieldValue != null) {
                    for (Object fieldValueItem : fieldValue) {
                        if (fieldValueItem == null) {

                        } else if (fieldValueItem.getClass().isEnum()) {
                            Enum<?> e = (Enum<?>) fieldValueItem;
                            if (e.name().equals(value.getValue())) {
                                containsValue = true;
                                break;
                            }
                        } else {
                            if (fieldValueItem.toString().equals(value.getValue())) {
                                containsValue = true;
                                break;
                            }
                        }
                    }
                }
                %>
                <option<%= containsValue ? " selected" : "" %> value="<%= wp.h(value.getValue()) %>"><%= wp.h(value.getLabel()) %></option>
            <% } %>
        </select>
    </div>
<% } %>
