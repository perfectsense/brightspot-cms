<%@ page session="false" import="

com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.State,
com.psddev.dari.util.ObjectUtils,

java.util.ArrayList,
java.util.Arrays,
java.util.Collections,
java.util.List,
java.util.Locale,
java.util.Comparator
" %><%

ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));

ObjectField field = (ObjectField) request.getAttribute("field");
ToolUi ui = field.as(ToolUi.class);
String fieldName = field.getInternalName();

String inputName = (String) request.getAttribute("inputName");
Locale fieldValue = (Locale) state.get(fieldName);

String placeholder = ui.getPlaceholder();
if (field.isRequired()) {
    if (ObjectUtils.isBlank(placeholder)) {
        placeholder = "(Required)";
    } else {
        placeholder += " (Required)";
    }
}

if (Boolean.TRUE.equals(request.getAttribute("isFormPost"))) {
    fieldValue = wp.param(Locale.class, inputName);
    state.put(fieldName, fieldValue);
    return;
}

Locale defaultLocale = Locale.getDefault();
Locale userLocale = ObjectUtils.firstNonNull(wp.getUser().getLocale(), defaultLocale);

wp.writeStart("div", "class", "inputSmall");

    wp.writeStart("select",
            "id", wp.getId(),
            "name", inputName,
            "data-searchable", true,
            "placeholder", placeholder);

        wp.writeStart("option", "value", "");
            wp.writeHtml("Default (");
            wp.writeHtml(createLocaleDisplayName(defaultLocale, userLocale));
            wp.writeHtml(")");
        wp.writeEnd();

        List<Locale> availableLocales = new ArrayList<>(Arrays.asList(Locale.getAvailableLocales()));

        availableLocales.remove(Locale.ROOT);

        Collections.<Locale>sort(availableLocales, localeComparator(userLocale));

        for (Locale availableLocale : availableLocales) {
            wp.writeStart("option",
                    "selected", availableLocale.equals(fieldValue) ? "selected" : null,
                    "value", availableLocale.toLanguageTag());
                wp.writeHtml(createLocaleDisplayName(availableLocale, userLocale));
            wp.writeEnd();
        }
    wp.writeEnd();

wp.writeEnd();

%><%!

private String createLocaleDisplayName(Locale locale, Locale userLocale) {
    String userDisplayName = locale.getDisplayName(userLocale);
    String displayName = locale.getDisplayName(locale);
    StringBuilder combined = new StringBuilder();

    combined.append(userDisplayName);

    if (!userDisplayName.equals(displayName)) {
        combined.append(" - ");
        combined.append(displayName);
    }

    return combined.toString();
}

private Comparator<Locale> localeComparator(final Locale userLocale) {
    return new Comparator<Locale>() {
        @Override
        public int compare(Locale o1, Locale o2) {
            return ObjectUtils.compare(createLocaleDisplayName(o1, userLocale), createLocaleDisplayName(o2, userLocale), true);
        }
    };
}
%>
