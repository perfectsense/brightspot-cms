<%@ page session="false" import="

com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.State,

java.util.Date,

org.joda.time.DateTime,
org.joda.time.DateTimeZone,
org.joda.time.format.DateTimeFormat
" %><%

ToolPageContext wp = new ToolPageContext(pageContext);
State state = State.getInstance(request.getAttribute("object"));
ObjectField field = (ObjectField) request.getAttribute("field");
String inputName = (String) request.getAttribute("inputName");
String fieldName = field.getInternalName();
Date fieldValue = (Date) state.getByPath(fieldName);

if (Boolean.TRUE.equals(request.getAttribute("isFormPost"))) {
    fieldValue = wp.param(Date.class, inputName);

    if (fieldValue != null) {
        DateTimeZone timeZone = wp.getUserDateTimeZone();
        fieldValue = new Date(DateTimeFormat.
                forPattern("yyyy-MM-dd HH:mm:ss").
                withZone(timeZone).
                parseMillis(new DateTime(fieldValue).toString("yyyy-MM-dd HH:mm:ss")));
    }

    state.put(fieldName, fieldValue);
    return;
}

wp.writeStart("div", "class", "inputSmall");
    wp.writeElement("input",
            "type", "text",
            "class", "date",
            "name", inputName,
            "placeholder", field.as(ToolUi.class).getPlaceholder(),
            "value", fieldValue != null ?
                    wp.formatUserDateTimeWith(fieldValue, "yyyy-MM-dd HH:mm:ss") :
                    null);
wp.writeEnd();
%>
