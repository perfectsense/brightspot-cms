<%@ page session="false" import="

com.psddev.cms.db.ReferentialTextMarker,
com.psddev.cms.db.RichTextReference,
com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Reference,
com.psddev.dari.db.ReferentialText,
com.psddev.dari.db.Query,
com.psddev.dari.db.State,
com.psddev.dari.util.HtmlWriter,
com.psddev.dari.util.StorageItem,

com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.StringUtils,

java.io.StringWriter,
java.util.ArrayList,
java.util.List,
java.util.Map,
java.util.Set,
java.util.UUID,
java.util.regex.Matcher
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
Object object = request.getAttribute("object");
State state = State.getInstance(object);

ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();
ReferentialText fieldValue = (ReferentialText) state.getValue(fieldName);

String inputName = (String) request.getAttribute("inputName");

if ((Boolean) request.getAttribute("isFormPost")) {
    ReferentialText refText = new ReferentialText();

    refText.setResolveInvisible(state.isResolveInvisible());
    refText.addHtml(wp.param(String.class, inputName));

    fieldValue = refText;

    state.putValue(fieldName, fieldValue);
    return;
}

// --- Presentation ---

ToolUi ui = field.as(ToolUi.class);
Set<String> rteTags = ui.findRichTextElementTags();
Number suggestedMinimum = ui.getSuggestedMinimum();
Number suggestedMaximum = ui.getSuggestedMaximum();
boolean renderBlockElements = ui.isRichTextBlock();

wp.writeStart("div", "class", "inputSmall inputSmall-text");
wp.writeStart("textarea",
        "class", "richtext",
        "data-expandable-class", "code",
        "id", wp.getId(),
        "name", inputName,
        "data-rte-tags", ObjectUtils.isBlank(rteTags) ? null : ObjectUtils.toJson(rteTags),
        "data-suggested-maximum", suggestedMaximum != null ? suggestedMaximum.intValue() : null,
        "data-suggested-minimum", suggestedMinimum != null ? suggestedMinimum.intValue() : null,
        "data-user", wp.getObjectLabel(wp.getUser()),
        "data-user-id", wp.getUser() != null ? wp.getUser().getId() : null,
        "data-first-draft", Boolean.TRUE.equals(request.getAttribute("firstDraft")),
        "data-final-draft", Boolean.TRUE.equals(request.getAttribute("finalDraft")),
        "data-render-block-elements", renderBlockElements);

if (fieldValue != null) {
    for (Object item : fieldValue) {

        if (item instanceof Reference) {
            Reference reference = (Reference) item;
            Object referenceObject = reference.getObject();
            if (referenceObject != null) {

                final List<Object> attributes = new ArrayList<Object>();
                final State referenceState = State.getInstance(referenceObject);

                // update the reference label and preview every time
                reference.as(RichTextReference.class).setLabel(referenceState.getLabel());
                reference.as(RichTextReference.class).setPreview(
                        referenceState.getPreview() != null ? referenceState.getPreview().getPublicUrl() : null);

                attributes.add("class");
                attributes.add("enhancement" + ((referenceObject instanceof ReferentialTextMarker) ? " marker" : ""));

                attributes.add("data-id");
                attributes.add(referenceState.getId());

                attributes.add("data-reference");
                attributes.add(ObjectUtils.toJson(reference.getState().getSimpleValues()));

                // backward compatibility + rte css attribute selector support
                attributes.add("data-preview");
                attributes.add(reference.as(RichTextReference.class).getPreview());
                attributes.add("data-alignment");
                attributes.add(reference.as(RichTextReference.class).getAlignment());

                StringWriter html = new StringWriter();
                new HtmlWriter(html) {{;
                    writeStart("span", attributes.toArray());
                    writeHtml(referenceState.getLabel());
                    writeEnd();
                }};

                wp.write(wp.h(html.toString()));
            }

        } else {
            wp.write(wp.h(item));
        }
    }
}

wp.writeEnd();
wp.writeEnd();
%><%!

private static void addStringToReferentialText(ReferentialText referentialText, String string) {
    string = StringUtils.replaceAll(string, "(?i)<p[^>]*>(?:\\s|&nbsp;)*</p>", "");
    string = string.trim();
    if (!ObjectUtils.isBlank(string)) {
        referentialText.add(string);
    }
}
%>
