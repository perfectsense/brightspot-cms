package com.psddev.cms.tool.page;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.SearchResultField;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.TypeDefinition;

@RoutingFilter.Path(application = "cms", value = "/searchResultFields")
public class SearchResultFields extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        ObjectType type = ObjectType.getInstance(page.param(UUID.class, "typeId"));
        ToolUser user = page.getUser();
        Map<String, List<String>> fieldNamesByTypeId = user.getSearchResultFieldsByTypeId();
        String typeId = type != null ? type.getId().toString() : "";
        List<String> fieldNames = fieldNamesByTypeId.get(typeId);
        String searchResultUrl = page.param(String.class, "searchResultUrl");

        if (page.isFormPost()) {
            if (page.param(boolean.class, "custom")) {
                fieldNamesByTypeId.put(typeId, page.params(String.class, "fieldNames"));

            } else {
                fieldNamesByTypeId.remove(typeId);
            }

            user.save();

            page.writeStart("div", "id", page.createId());
            page.writeEnd();

            page.writeStart("script", "type", "text/javascript");
                page.writeRaw("$('#" + page.getId() + "').popup('source').closest('.searchForm').find('.searchFiltersRest').submit();");
                page.writeRaw("$('#" + page.getId() + "').popup('close');");
            page.writeEnd();
            return;
        }

        page.writeStart("div", "class", "widget");
            page.writeStart("h1");
                if (type != null) {
                    page.writeObjectLabel(type);
                    page.writeHtml(' ');
                }

                page.writeHtml("Fields");
            page.writeEnd();

            page.writeStart("form",
                    "method", "post",
                    "action", page.url(""));

                page.writeStart("div");
                    page.writeElement("input",
                            "type", "radio",
                            "id", page.createId(),
                            "name", "custom",
                            "value", "false",
                            "checked", fieldNames == null ? "checked" : null);

                    page.writeStart("label", "for", page.getId());
                        page.writeHtml("Default");
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("div");
                    page.writeElement("input",
                            "type", "radio",
                            "id", page.createId(),
                            "name", "custom",
                            "value", "true",
                            "checked", fieldNames != null ? "checked" : null);

                    page.writeStart("label", "for", page.getId());
                        page.writeHtml("Custom");
                    page.writeEnd();

                    page.writeStart("select",
                            "multiple", "multiple",
                            "name", "fieldNames");

                        if (type != null) {
                            page.writeStart("optgroup", "label", type.getDisplayName());
                                writeObjectFieldOptions(page, fieldNames, type.getFields());
                            page.writeEnd();
                        }

                        page.writeStart("optgroup", "label", "Custom");
                            for (Class<? extends SearchResultField> c : ClassFinder.Static.findClasses(SearchResultField.class)) {
                                if (!c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {
                                    SearchResultField field = TypeDefinition.getInstance(c).newInstance();

                                    if (field.isSupported(type)) {
                                        String cName = c.getName();

                                        page.writeStart("option",
                                                "value", cName,
                                                "selected", fieldNames != null && fieldNames.contains(cName) ? "selected" : null);

                                            page.writeHtml(field.getDisplayName());
                                        page.writeEnd();
                                    }
                                }
                            }
                        page.writeEnd();

                        page.writeStart("optgroup", "label", "Global");
                            writeObjectFieldOptions(page, fieldNames, Database.Static.getDefault().getEnvironment().getFields());
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("div", "class", "actions");
                    page.writeStart("button");
                        page.writeHtml("Update");
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeEnd();
    }

    private void writeObjectFieldOptions(
            ToolPageContext page,
            List<String> fieldNames,
            List<ObjectField> fields)
            throws IOException {

        for (ObjectField field : fields) {
            String fieldName = field.getInternalName();

            page.writeStart("option",
                    "value", fieldName,
                    "selected", fieldNames != null && fieldNames.contains(fieldName) ? "selected" : null);

                page.writeHtml(field.getDisplayName());
            page.writeEnd();
        }
    }
}
