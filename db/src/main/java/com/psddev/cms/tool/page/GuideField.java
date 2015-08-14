package com.psddev.cms.tool.page;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.GuideType;
import com.psddev.cms.db.PageFilter;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Reference;
import com.psddev.dari.db.ReferentialText;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "guideField")
public class GuideField extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        ObjectType type = ObjectType.getInstance(page.param(UUID.class, "typeId"));

        if (type == null) {
            throw new IllegalArgumentException();
        }

        String fieldName = page.param(String.class, "field");
        ObjectField field = type.getField(fieldName);

        if (field == null) {
            field = type.getEnvironment().getField(fieldName);
        }

        page.writeHeader();
            page.writeStart("style", "type", "text/css");
                page.writeCss(".cms-guideField th",
                        "width", "25%;");
            page.writeEnd();

            page.writeStart("div", "class", "widget cms-guideField");
                page.writeStart("h1", "class", "icon icon-object-guide");
                    page.writeHtml(field.getLabel());
                page.writeEnd();

                page.writeStart("div", "class", "tabbed");

                    // Editorial field description.
                    page.writeStart("div", "data-tab", "For Editors");
                        GuideType guideType = GuideType.Static.getGuideType(type);
                        ReferentialText fieldDescription = null;

                        if (guideType != null) {
                            fieldDescription = guideType.getFieldDescription(fieldName, null, false);
                        }

                        if (fieldDescription != null && !fieldDescription.isEmpty()) {
                            StringBuilder cleaned = new StringBuilder();

                            for (Object item : fieldDescription) {
                                if (item != null) {
                                    if (item instanceof Reference) {
                                        StringWriter writer = new StringWriter();

                                        PageFilter.renderObject(
                                                page.getRequest(),
                                                page.getResponse(),
                                                writer,
                                                ((Reference) item).getObject());

                                        cleaned.append(writer.toString());

                                    } else {
                                        cleaned.append(item.toString());
                                    }
                                }
                            }

                            page.writeRaw(cleaned.toString().replaceAll("(?i)(\\s*<br[^>]*>\\s*)+$", ""));
                        }

                        // Constraints.
                        List<String> constraints = new ArrayList<String>();

                        if (field.isRequired()) {
                            constraints.add("Required");
                        }

                        Object absMin = GuideType.Static.getFieldMinimumValue(field);
                        Object absMax = GuideType.Static.getFieldMaximumValue(field);

                        if (absMin != null) {
                            constraints.add("Absolute Minimum: " + absMin);
                        }

                        if (absMax != null) {
                            constraints.add("Absolute Maximum: " + absMax);
                        }

                        ToolUi fieldUi = field.as(ToolUi.class);
                        Object sugMin = fieldUi.getSuggestedMinimum();
                        Object sugMax = fieldUi.getSuggestedMaximum();

                        if (sugMin != null) {
                            constraints.add("Suggested Minimum: " + sugMin);
                        }

                        if (sugMax != null) {
                            constraints.add("Suggested Maximum: " + sugMax);
                        }

                        if (!constraints.isEmpty()) {
                            page.writeStart("h2", "style", "margin-top:15px;");
                                page.writeHtml("Constraints");
                            page.writeEnd();

                            page.writeStart("ul");
                                for (String constraint : constraints) {
                                    page.writeStart("li");
                                        page.writeHtml(constraint);
                                    page.writeEnd();
                                }
                            page.writeEnd();
                        }

                        if ((fieldDescription == null
                                || fieldDescription.isEmpty())
                                && constraints.isEmpty()) {
                            page.writeStart("div", "class", "message message-info");
                                page.writeHtml("No editorial production guide for this field.");
                            page.writeEnd();
                        }
                    page.writeEnd();

                    // Development help.
                    page.writeStart("div", "data-tab", "For Developers");
                        Class<?> fieldDeclaringClass = ObjectUtils.getClassByName(field.getJavaDeclaringClassName());

                        if (fieldDeclaringClass != null) {
                            Field javaField = field.getJavaField(fieldDeclaringClass);

                            if (javaField != null) {

                                page.writeStart("table", "class", "table-striped");
                                    page.writeStart("tbody");
                                        page.writeStart("tr");
                                            page.writeStart("th");
                                                page.writeHtml("Declared In");
                                            page.writeEnd();

                                            page.writeStart("td");
                                                page.writeJavaClassLink(fieldDeclaringClass);
                                            page.writeEnd();
                                        page.writeEnd();

                                        page.writeStart("tr");
                                            page.writeStart("th");
                                                page.writeHtml("Field");
                                            page.writeEnd();

                                            page.writeStart("td");
                                                page.writeStart("code");
                                                    page.writeHtml(javaField);
                                                page.writeEnd();
                                            page.writeEnd();
                                        page.writeEnd();

                                        page.writeStart("tr");
                                            page.writeStart("th");
                                                page.writeHtml("Internal Name");
                                            page.writeEnd();

                                            page.writeStart("td");
                                                page.writeStart("code");
                                                    page.writeHtml(field.getInternalName());
                                                page.writeEnd();
                                            page.writeEnd();
                                        page.writeEnd();
                                    page.writeEnd();
                                page.writeEnd();

                                ContentTools.Static.writeJavaAnnotationDescriptions(page, javaField);
                            }
                        }
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }
}
