<%@ page session="false" import="

com.psddev.cms.tool.ToolPageContext,
com.psddev.cms.tool.page.content.Edit,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.State,
com.psddev.dari.util.ObjectUtils
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));

ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();

String inputName = (String) request.getAttribute("inputName");

if ((Boolean) request.getAttribute("isFormPost")) {
    Object value = wp.param(String.class, inputName);

    if (value == null) {
        state.put(fieldName, null);

    } else if (field.getStep() != null && field.getStep().doubleValue() * 10 % 10 == 0.0) {
        Long valueLong = ObjectUtils.to(Long.class, value);

        if (valueLong != null) {
            state.put(fieldName, valueLong);

        } else {
            state.addError(field, String.format(
                    "[%s] is not an integer!", value));
        }

    } else {
        Double valueDouble = ObjectUtils.to(Double.class, value);

        if (valueDouble != null) {
            state.put(fieldName, valueDouble);

        } else {
            state.addError(field, String.format(
                    "[%s] is not an number!", value));
        }
    }

    return;
}

// --- Presentation ---

%><div class="inputSmall">
    <input id="<%= wp.getId() %>" name="<%= wp.h(inputName) %>" type="text" placeholder="<%= wp.h(Edit.createPlaceholderText(wp, field)) %>" value="<%= wp.h(state.getValue(fieldName)) %>">
</div>
