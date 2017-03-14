package com.psddev.cms.view;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.CodeUtils;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.DebugFilter;
import com.psddev.dari.util.DebugServlet;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;

@SuppressWarnings("serial")
public class ViewInterfaceSchemaDebugServlet extends DebugServlet {

    @Override
    public String getName() {
        return "View Interface: Schema Viewer";
    }

    @Override
    public List<String> getPaths() {
        return Collections.singletonList("view-interface-schema");
    }

    @Override
    protected void service(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {

        new DebugFilter.PageWriter(getServletContext(), request, response) { {

            List<TypeDefinition<?>> viewTypes = ClassFinder.findClasses(Object.class)
                    .stream()
                    .filter(c -> c.isAnnotationPresent(ViewInterface.class))
                    .map(TypeDefinition::getInstance)
                    .collect(Collectors.toList());

            Collections.sort(viewTypes, (t1, t2) -> t1.getObjectClass().getSimpleName().compareTo(t2.getObjectClass().getSimpleName()));

            Set<TypeDefinition<?>> selectedTypes = new HashSet<>();
            List<String> typeIds = page.params(String.class, "typeId");
            if (typeIds != null) {
                Set<String> typeIdsSet = new HashSet<>();
                typeIdsSet.addAll(typeIds);
                for (TypeDefinition<?> t : viewTypes) {
                    if (typeIdsSet.contains(t.getObjectClass().getName())) {
                        selectedTypes.add(t);
                    }
                }
            }

            startPage("View Interface", "Schema"); {
                writeStart("form", "method", "get"); {
                    writeStart("select", "multiple", "multiple", "name", "typeId", "style", "width: 90%;"); {
                        for (TypeDefinition<?> t : viewTypes) {
                            writeStart("option",
                                    "selected", selectedTypes.contains(t) ? "selected" : null,
                                    "value", t.getObjectClass().getName()); {
                                writeHtml(StringUtils.removeEnd(t.getObjectClass().getSimpleName(), "View"));
                                writeHtml(" (").writeHtml(t.getObjectClass().getName()).writeHtml(")");
                            }
                            writeEnd();
                        }
                    }
                    writeEnd();
                    writeElement("br");
                    writeElement("input", "class", "btn", "type", "submit", "value", "View");
                }
                writeEnd();

                includeStylesheet("/_resource/chosen/chosen.css");
                includeScript("/_resource/chosen/chosen.jquery.min.js");
                writeStart("script", "type", "text/javascript"); {
                    write("(function() {"); {
                        write("$('select[name=typeId]').chosen({ 'search_contains': true });");
                    }
                    write("})();");
                }
                writeEnd();

                writeStart("style", "type", "text/css"); {
                    write(".column { display: table-cell; padding-right: 15em; text-align: center; vertical-align: top; }");
                    write(".column dl { margin-bottom: 0; }");
                    write(".type { border: 1px solid black; display: inline-block; margin-bottom: 5em; padding: 0.5em; text-align: left; }");
                    write(".type h2 { white-space: nowrap; }");
                    write(".type dt { margin-bottom: 5px; }");
                    write(".type dd:last-child table { margin-bottom: 0; }");
                    write(".type .reference { color: white; white-space: nowrap; }");
                }
                writeEnd();

                writeStart("div", "class", "types"); {

                    Set<TypeDefinition<?>> allTypes = new HashSet<>();
                    Set<TypeDefinition<?>> currentTypes = new HashSet<>(selectedTypes);

                    // how many levels deep to follow
                    int depth = page.paramOrDefault(int.class, "d", -1);

                    while (!currentTypes.isEmpty() && (depth--) != 0) {
                        writeStart("div", "class", "column"); {
                            allTypes.addAll(currentTypes);
                            Set<TypeDefinition<?>> nextTypes = new LinkedHashSet<>();

                            for (TypeDefinition<?> t : currentTypes) {

                                Map<String, List<Method>> fieldsByClass = new CompactMap<>();

                                for (Method field : t.getAllGetters().values()) {
                                    String declaringClass = field.getDeclaringClass().getName();
                                    if (declaringClass != null) {
                                        List<Method> fields = fieldsByClass.get(declaringClass);
                                        if (fields == null) {
                                            fields = new ArrayList<>();
                                            fieldsByClass.put(declaringClass, fields);
                                        }
                                        fields.add(field);
                                    }
                                }

                                writeStart("div").writeEnd();
                                writeStart("div", "class", "type", "id", "type-" + t.getObjectClass().getName().replace('.', '_')); {
                                    writeStart("h2").writeHtml(StringUtils.removeEnd(t.getObjectClass().getSimpleName(), "View")).writeEnd();
                                    writeStart("dl"); {

                                        for (Map.Entry<String, List<Method>> entry : fieldsByClass.entrySet()) {
                                            String className = entry.getKey();
                                            File source = CodeUtils.getSource(className);

                                            writeStart("dt"); {
                                                if (source == null) {
                                                    writeHtml(className);

                                                } else {
                                                    writeStart("a",
                                                            "target", "_blank",
                                                            "href", DebugFilter.Static.getServletPath(page.getRequest(), "code", "file", source)); {
                                                        writeHtml(className);
                                                    }
                                                    writeEnd();
                                                }
                                            }
                                            writeEnd();

                                            writeStart("dd"); {
                                                writeStart("table", "class", "table table-condensed"); {
                                                    writeStart("tbody"); {
                                                        for (Method field : entry.getValue()) {

                                                            String internalName = field.getName();
                                                            Class<?> internalType = field.getReturnType();
                                                            Class<?> internalItemType = null;

                                                            String internalNameLabel = Character.toLowerCase(internalName.charAt(3)) + internalName.substring(4);
                                                            String internalTypeLabel = "";
                                                            String internalItemTypeLabel;

                                                            if (Collection.class.equals(internalType)) {
                                                                Type genericType = field.getGenericReturnType();

                                                                if (genericType instanceof ParameterizedType) {

                                                                    ParameterizedType parameterizedType = (ParameterizedType) genericType;

                                                                    Type[] typeArguments = parameterizedType.getActualTypeArguments();

                                                                    if (typeArguments.length == 1) {
                                                                        Type typeArgument = typeArguments[0];

                                                                        if (typeArgument instanceof WildcardType) {

                                                                            WildcardType wildcardType = (WildcardType) typeArgument;
                                                                            Type[] upperBounds = wildcardType.getUpperBounds();

                                                                            if (upperBounds.length == 1) {
                                                                                Type upperBound = upperBounds[0];

                                                                                if (upperBound instanceof Class) {
                                                                                    internalItemType = (Class<?>) upperBound;
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }

                                                                if (internalItemType == null) {
                                                                    internalItemType = Object.class;
                                                                }

                                                                internalTypeLabel = "list/";

                                                            } else {
                                                                internalItemType = internalType;
                                                            }

                                                            if (CharSequence.class.isAssignableFrom(internalItemType)) {
                                                                internalItemTypeLabel = "text";

                                                            } else if (Number.class.isAssignableFrom(internalItemType)) {
                                                                internalItemTypeLabel = "number";

                                                            } else if (Boolean.class.isAssignableFrom(internalItemType)) {
                                                                internalItemTypeLabel = "boolean";

                                                            } else if (Map.class.isAssignableFrom(internalItemType)) {
                                                                internalItemTypeLabel = "map";

                                                            } else if (internalItemType == Object.class) {
                                                                internalItemTypeLabel = "object";

                                                            } else {
                                                                internalItemTypeLabel = "view";
                                                            }

                                                            internalTypeLabel += internalItemTypeLabel;

                                                            // only show fields that reference other types
                                                            if (page.param(boolean.class, "nf") && !internalItemTypeLabel.equals("view")) {
                                                                continue;
                                                            }

                                                            writeStart("tr"); {
                                                                writeStart("td").writeHtml(internalNameLabel).writeEnd();
                                                                writeStart("td").writeHtml(internalTypeLabel).writeEnd();

                                                                writeStart("td"); {
                                                                    if ("view".equals(internalItemTypeLabel)) {

                                                                        List<TypeDefinition<?>> itemTypes = ClassFinder.findClasses(internalItemType).stream()
                                                                                .filter(c -> c.isAnnotationPresent(ViewInterface.class))
                                                                                .map(TypeDefinition::getInstance)
                                                                                .collect(Collectors.toList());

                                                                        if (!ObjectUtils.isBlank(itemTypes)) {
                                                                            for (TypeDefinition<?> itemType : itemTypes) {
                                                                                if (!allTypes.contains(itemType)) {
                                                                                    nextTypes.add(itemType);
                                                                                }
                                                                                writeStart("a",
                                                                                        "class", "label reference",
                                                                                        "data-typeId", itemType.getObjectClass().getName().replace('.', '_'),
                                                                                        "href", page.url(null,
                                                                                                "typeId", itemType.getObjectClass().getName(),
                                                                                                "nf", page.param(Boolean.class, "nf"),
                                                                                                "d", page.param(Integer.class, "d"))); {
                                                                                    writeHtml(StringUtils.removeEnd(itemType.getObjectClass().getSimpleName(), "View"));
                                                                                }
                                                                                writeEnd();
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                writeEnd();
                                                            }
                                                            writeEnd();
                                                        }
                                                    }
                                                    writeEnd();
                                                }
                                                writeEnd();
                                            }
                                            writeEnd();
                                        }
                                    }
                                    writeEnd();
                                }
                                writeEnd();
                            }

                            currentTypes = nextTypes;
                        }
                        writeEnd();
                    }
                }
                writeEnd();

                includeScript("/_resource/dari/db-schema.js");
            }
            endPage();
        } };
    }
}
