<%@ page session="false" import="

com.psddev.cms.db.Localization,
com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,
com.psddev.cms.tool.page.content.Edit,
com.psddev.cms.tool.page.TestSms,
com.psddev.cms.tool.page.content.field.TextField,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.Reference,
com.psddev.dari.db.ReferentialText,
com.psddev.dari.db.State,
com.psddev.dari.util.ObjectUtils,

java.util.Set
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));

ObjectField field = (ObjectField) request.getAttribute("field");
ToolUi ui = field.as(ToolUi.class);
String fieldName = field.getInternalName();
Object fieldValue = state.getValue(fieldName);

String inputName = (String) request.getAttribute("inputName");

String placeholder = Edit.createPlaceholderText(wp, field);
Number suggestedMinimum = ui.getSuggestedMinimum();
Number suggestedMaximum = ui.getSuggestedMaximum();
boolean testSms = ui.isEffectivelyTestSms();

if ((Boolean) request.getAttribute("isFormPost")) {
    String newValue = wp.param(String.class, inputName);

    if (ui.isRichText()) {
        StringBuilder newValueBuilder = new StringBuilder();

        for (Object item : new ReferentialText(newValue, Boolean.TRUE.equals(request.getAttribute("finalDraft")))) {
            if (!(item == null || item instanceof Reference)) {
                newValueBuilder.append(item.toString());
            }
        }

        newValue = newValueBuilder.toString();

        if (newValue.matches("(?i)<br[^>]*/?>")) {
            newValue = null;
        }
    }

    TextField.put(state, field, newValue);
    return;
}

// --- Presentation ---

if (testSms) {
    wp.writeStart("div", "class", "Sms");
}

wp.write("<div class=\"inputSmall inputSmall-text\">");

Set<ObjectField.Value> validValues = field.getValues();
if (validValues != null) {
    wp.write("<select id=\"", wp.getId(), "\" name=\"", wp.h(inputName), "\">");
    wp.write("<option value=\"\">");
    wp.write(wp.h(placeholder));
    wp.write("</option>");
    for (ObjectField.Value value : validValues) {
        wp.write("<option");
        if (ObjectUtils.equals(value.getValue(), fieldValue)) {
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
            "name", inputName,
            "placeholder", ObjectUtils.firstNonBlank(
                    placeholder,
                    Localization.currentUserText(
                            "com.psddev.cms.tool.page.content.field.TextField",
                            "placeholder.noColor")),
            "value", fieldValue);

} else if (ui.isSecret()) {
    wp.writeElement("input",
            "type", "text",
            "class", "secret",
            "id", wp.getId(),
            "name", inputName,
            "placeholder", placeholder,
            "value", fieldValue);

} else {
    Set<String> rteTags = ui.findRichTextElementTags();

    wp.writeStart("textarea",
            "class", ui.isRichText() ? "richtext" : null,
            "id", wp.getId(),
            "name", inputName,
            "placeholder", placeholder,
            "data-dynamic-placeholder", ui.getPlaceholderDynamicText(),
            "data-dynamic-field-name", field.getInternalName(),
            "data-code-type", ui.getCodeType(),
            "data-editable-placeholder", ui.isPlaceholderEditable() ? ui.getPlaceholder() : null,
            "data-placeholder-clear-on-change", ui.isPlaceholderClearOnChange() ? true : null,
            "data-rte-toolbar", ui.getRichTextToolbarClassName(),
            "data-rte-tags", ObjectUtils.isBlank(rteTags) ? null : ObjectUtils.toJson(rteTags),
            "data-suggested-maximum", suggestedMaximum != null ? suggestedMaximum.intValue() : null,
            "data-suggested-minimum", suggestedMinimum != null ? suggestedMinimum.intValue() : null,
            "data-inline", ui.isRichTextInline(),
            "data-user", wp.getObjectLabel(wp.getUser()),
            "data-user-id", wp.getUser() != null ? wp.getUser().getId() : null,
            "data-first-draft", Boolean.TRUE.equals(request.getAttribute("firstDraft")),
            "data-track-changes", !Boolean.TRUE.equals(request.getAttribute("finalDraft")));
        wp.writeHtml(fieldValue);
    wp.writeEnd();
}

wp.write("</div>");

if (testSms) {
    wp.writeStart("button", "class", "Sms-button").writeHtml(wp.localize(TestSms.class, "action.send")).writeEnd();
    wp.writeStart("div", "class", "Sms-response");
        wp.writeStart("div", "class", "Sms-pending").writeEnd();
    wp.writeEnd();

    // Sms div.
    wp.writeEnd();
}
%>
