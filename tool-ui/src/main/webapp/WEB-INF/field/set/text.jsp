<%@ page session="false" import="

com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,
com.psddev.cms.tool.page.content.field.TextField,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.State,

com.psddev.dari.util.ObjectUtils,

java.util.Collection,
java.util.Set,
java.util.TreeSet
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));

ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();
Set<Object> fieldValue;
Object fieldValueObject = state.getValue(fieldName);
if (fieldValueObject instanceof Set) {
    fieldValue = (Set<Object>) fieldValueObject;
} else if (fieldValueObject instanceof Collection) {
    fieldValue = new TreeSet<Object>((Collection<Object>) fieldValueObject);
} else {
    fieldValue = null;
}
Set<ObjectField.Value> validValues = field.getValues();

String inputName = (String) request.getAttribute("inputName");
String textName = inputName + ".text";
String toggleName = inputName + ".toggle";

if ((Boolean) request.getAttribute("isFormPost")) {
    fieldValue = new TreeSet<Object>();

    if (ObjectUtils.isBlank(validValues)) {
        String[] texts = wp.params(textName);
        String[] toggles = wp.params(toggleName);
        for (int i = 0, s = Math.min(texts.length, toggles.length); i < s; i ++) {
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

    TextField.put(state, field, fieldValue);
    return;
}

ToolUi ui = field.as(ToolUi.class);
String placeholder = ui.getPlaceholder();
String dynamicPlaceholder = ui.getPlaceholderDynamicText();

if (field.isRequired()) {
    if (ObjectUtils.isBlank(placeholder)) {
        placeholder = "(Required)";

    } else {
        placeholder += " (Required)";
    }
}

// --- Presentation ---

%><% if (ObjectUtils.isBlank(validValues)) { %>
    <div class="inputSmall repeatableText">
        <ul>
            <% if (fieldValue != null) for (Object text : fieldValue) { %>
                <li>
                    <input checked name="<%= wp.h(toggleName) %>" type="checkbox" value="true">
                    <input class="expandable" name="<%= wp.h(textName) %>" type="text" value="<%= wp.h(text) %>">
                </li>
            <% } %>
            <script type="text/template">
                <li>
                    <input name="<%= wp.h(toggleName) %>" type="checkbox" value="true">
                    <input class="expandable" name="<%= wp.h(textName) %>" type="text">
                </li>
            </script>
        </ul>
    </div>

<% } else { %>
    <div class="inputSmall">
        <%
            wp.writeStart("select",
                    "multiple", "multiple",
                    "placeholder", placeholder,
                    "data-dynamic-placeholder", dynamicPlaceholder,
                    "data-dynamic-field-name", field.getInternalName(),
                    "name", textName);
        %>
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
        <%
            wp.writeEnd();
        %>
    </div>
<% } %>
