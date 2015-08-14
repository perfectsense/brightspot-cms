<%@ page session="false" import="

com.psddev.cms.db.Content,
com.psddev.cms.db.Site,
com.psddev.cms.db.Template,
com.psddev.cms.db.ToolUi,
com.psddev.cms.db.Workflow,
com.psddev.cms.db.WorkflowTransition,
com.psddev.cms.tool.Area,
com.psddev.cms.tool.Plugin,
com.psddev.cms.tool.ToolPageContext,
com.psddev.cms.tool.Widget,

com.psddev.dari.db.Application,
com.psddev.dari.db.Database,
com.psddev.dari.db.Modification,
com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Query,
com.psddev.dari.db.State,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.SparseSet,

java.io.IOException,
java.util.ArrayList,
java.util.Collections,
java.util.HashMap,
java.util.HashSet,
java.util.Iterator,
java.util.LinkedHashMap,
java.util.List,
java.util.Map,
java.util.Set,
java.util.TreeSet
" %>
<%@ page import="com.psddev.dari.util.StringUtils" %>
<%@ page import="com.psddev.dari.db.DatabaseEnvironment" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));

ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();
String fieldValue = (String) state.getValue(fieldName);
SparseSet permissions = new SparseSet(ObjectUtils.isBlank(fieldValue) ? "+/" : fieldValue);

String inputName = (String) request.getAttribute("inputName");

if ((Boolean) request.getAttribute("isFormPost")) {

    permissions.clear();
    Set<String> denieds = new HashSet<String>();
    for (String permissionId : wp.params(inputName)) {

        if (permissionId == null) {
            permissionId = "";
        }

        String parent = wp.param(inputName + "." + permissionId);
        if ("all".equals(parent)) {
            permissions.add(permissionId);
            permissions.add(permissionId + "/");

        } else if ("some".equals(parent)) {
            permissions.add(permissionId);

        } else if ("no".equals(parent)) {
            if (permissionId.length() == 0) {
                break;
            } else {
                denieds.add(permissionId + "/");
            }

        } else {
            boolean isDenied = false;
            for (String denied : denieds) {
                if (permissionId.startsWith(denied)) {
                    isDenied = true;
                    break;
                }
            }
            if (!isDenied) {
                permissions.add(permissionId);
            }
        }
    }

    for (String permissionId : wp.params(inputName)) {
        String parent = wp.param(inputName + "." + permissionId);

        if ("some".equals(parent)) {
            String excludeFields = wp.param(inputName + "." + permissionId + "/excludeFields");

            if (!ObjectUtils.isBlank(excludeFields)) {
                for (String fn : excludeFields.trim().split("\\s+")) {
                    permissions.remove(permissionId + "/field/" + fn);
                    permissions.remove(permissionId + "/field/" + fn + "/");
                }
            }
        }
    }

    state.putValue(fieldName, permissions.toString());
    return;
}

Map<ObjectType, Workflow> workflows = new HashMap<ObjectType, Workflow>();

for (Workflow w : Query.from(Workflow.class).selectAll()) {
    for (ObjectType t : w.getContentTypes()) {
        workflows.put(t, w);
    }
}

wp.writeStart("div", "class", "inputSmall permissions");
    wp.writeStart("div", "class", "permissionsSection");
        writeParent(wp, permissions, "Sites", "site");

        wp.writeStart("ul");
            wp.writeStart("li");
                writeChild(wp, permissions, "Global", "site/global");
            wp.writeEnd();

            for (Site site : Site.findAll()) {
                wp.writeStart("li");
                    writeChild(wp, permissions, site, site.getPermissionId());
                wp.writeEnd();
            }
        wp.writeEnd();
    wp.writeEnd();

    wp.writeStart("div", "class", "permissionsSection");
        writeParent(wp, permissions, "Areas", "area");

        wp.writeStart("ul");
            for (Area top : wp.getCmsTool().findTopAreas()) {
                if (top.hasChildren()) {
                    wp.writeStart("li");
                        writeParent(wp, permissions, top, top.getPermissionId());

                        wp.writeStart("ul");
                            for (Area child : top.findChildren()) {
                                wp.writeStart("li");
                                    writeChild(wp, permissions, child, child.getPermissionId());
                                wp.writeEnd();
                            }
                        wp.writeEnd();
                    wp.writeEnd();

                } else {
                    wp.writeStart("li");
                        writeChild(wp, permissions, top, top.getPermissionId());
                    wp.writeEnd();
                }
            }
        wp.writeEnd();
    wp.writeEnd();

    wp.writeStart("div", "class", "permissionsSection");
        writeParent(wp, permissions, "Widgets", "widget");

        wp.writeStart("ul");
            for (Widget widget : wp.getCmsTool().findPlugins(Widget.class)) {
                wp.writeStart("li");
                    writeChild(wp, permissions, widget, widget.getPermissionId());
                wp.writeEnd();
            }
        wp.writeEnd();
    wp.writeEnd();

    DatabaseEnvironment dbEnv = Database.Static.getDefault().getEnvironment();
    Set<String> tabNames = new TreeSet<>();

    for (ObjectField f : dbEnv.getFields()) {
        String tabName = f.as(ToolUi.class).getTab();

        if (!ObjectUtils.isBlank(tabName)) {
            tabNames.add(tabName);//
        }
    }

    for (ObjectType t : dbEnv.getTypes()) {
        for (ObjectField f : t.getFields()) {
            String tabName = f.as(ToolUi.class).getTab();

            if (!ObjectUtils.isBlank(tabName)) {
                tabNames.add(tabName);//
            }
        }
    }

    if (!tabNames.isEmpty()) {
        wp.writeStart("div", "class", "permissionsSection");
            writeParent(wp, permissions, "Tabs", "tab");

            wp.writeStart("ul");
                for (String tabName : tabNames) {
                    String permissionId = "tab/" + StringUtils.toNormalized(tabName);

                    wp.writeStart("li");
                        wp.writeElement("input",
                                "type", "checkbox",
                                "id", wp.createId(),
                                "name", wp.getRequest().getAttribute("inputName"),
                                "value", permissionId,
                                "checked", permissions.contains(permissionId) ? "checked" : null);

                        wp.writeHtml(" ");

                        wp.writeStart("label", "for", wp.getId());
                            wp.writeHtml(tabName);
                        wp.writeEnd();
                    wp.writeEnd();
                }
            wp.writeEnd();
        wp.writeEnd();
    }

    wp.writeStart("div", "class", "permissionsType");
        writeParent(wp, permissions, "Types", "type");

        List<ObjectType> mainTypes = Template.Static.findUsedTypes(wp.getSite());
        List<ObjectType> internalTypes = new ArrayList<ObjectType>();
        List<ObjectType> typesList = new ArrayList<ObjectType>(Database.Static.getDefault().getEnvironment().getTypes());

        mainTypes.retainAll(typesList);
        typesList.removeAll(mainTypes);

        for (Iterator<ObjectType> i = typesList.iterator(); i.hasNext(); ) {
            ObjectType t = i.next();
            String name = t.getInternalName();
            Set<String> groups = t.getGroups();

            if (!t.isConcrete() ||
                    name.startsWith("com.psddev.cms.db.") ||
                    name.startsWith("com.psddev.cms.tool.") ||
                    groups.contains(Modification.class.getName()) ||
                    groups.contains(Application.class.getName()) ||
                    groups.contains(Plugin.class.getName()) ||
                    !groups.contains(Content.SEARCHABLE_GROUP)) {
                i.remove();

                wp.writeElement("input",
                        "type", "hidden",
                        "name", inputName,
                        "value", "type/" + t.getId().toString());

                wp.writeElement("input",
                        "type", "hidden",
                        "name", inputName,
                        "value", "type/" + t.getId().toString() + "/");
            }
        }

        Map<String, List<ObjectType>> typeGroups = new LinkedHashMap<String, List<ObjectType>>();

        Collections.sort(mainTypes);
        Collections.sort(typesList);

        typeGroups.put("Main Content Types", mainTypes);
        typeGroups.put("Misc Content Types", typesList);

        for (Map.Entry<String, List<ObjectType>> entry : typeGroups.entrySet()) {
            wp.writeStart("h2").writeHtml(entry.getKey()).writeEnd();

            wp.writeStart("ul");
                for (ObjectType type : entry.getValue()) {
                    String typePermissionId = "type/" + type.getId().toString();

                    wp.writeStart("li");
                        writeParent(wp, permissions, type, typePermissionId);

                        wp.writeElement("input",
                                "type", "hidden",
                                "name", inputName,
                                "value", typePermissionId + "/field");

                        wp.writeElement("input",
                                "type", "hidden",
                                "name", inputName,
                                "value", typePermissionId + "/field/");

                        wp.writeStart("ul");
                            wp.writeStart("li");
                                writeChild(wp, permissions, "Read", typePermissionId + "/read");
                            wp.writeEnd();

                            wp.writeStart("li");
                                writeChild(wp, permissions, "Write", typePermissionId + "/write");
                            wp.writeEnd();

                            wp.writeStart("li");
                                writeChild(wp, permissions, "Publish", typePermissionId + "/publish");
                            wp.writeEnd();

                            wp.writeStart("li");
                                writeChild(wp, permissions, "Archive", typePermissionId + "/archive");
                            wp.writeEnd();

                            wp.writeStart("li");
                                writeChild(wp, permissions, "Restore", typePermissionId + "/restore");
                            wp.writeEnd();

                            wp.writeStart("li");
                                writeChild(wp, permissions, "Delete Permanently", typePermissionId + "/delete");
                            wp.writeEnd();

                            wp.writeStart("li");
                                writeChild(wp, permissions, "Bulk Edit", typePermissionId + "/bulkEdit");
                            wp.writeEnd();

                            wp.writeStart("li");
                                writeChild(wp, permissions, "Bulk Workflow", typePermissionId + "/bulkWorkflow");
                            wp.writeEnd();

                            wp.writeStart("li");
                                writeChild(wp, permissions, "Bulk Archive", typePermissionId + "/bulkArchive");
                            wp.writeEnd();

                            Workflow workflow = workflows.get(type);

                            if (workflow != null) {
                                for (Map.Entry<String, WorkflowTransition> entry2 : workflow.getTransitions().entrySet()) {
                                    String transition = entry2.getKey();
                                    String transitionDisplay = entry2.getValue().getDisplayName();

                                    wp.writeStart("li");
                                        writeChild(wp, permissions, "Workflow: " + transitionDisplay, typePermissionId + "/" + transition);
                                    wp.writeEnd();
                                }
                            }

                            wp.writeStart("li");
                                wp.writeStart("label", "for", wp.createId());
                                    wp.writeHtml("Exclude Fields: " );
                                wp.writeEnd();

                                StringBuilder excludeFields = new StringBuilder();

                                for (ObjectField typeField : type.getFields()) {
                                    String fn = typeField.getInternalName();

                                    if (!permissions.contains(typePermissionId + "/field/" + fn)) {
                                        excludeFields.append(fn);
                                        excludeFields.append(" ");
                                    }
                                }

                                wp.writeElement("input",
                                        "type", "text",
                                        "id", wp.getId(),
                                        "name", inputName + "." + typePermissionId + "/excludeFields",
                                        "value", excludeFields);
                            wp.writeEnd();
                        wp.writeEnd();
                    wp.writeEnd();
                }
            wp.writeEnd();
        }
    wp.writeEnd();
wp.writeEnd();
%><%!

private static void writeLabel(ToolPageContext wp, Object object) throws IOException {

    if (object instanceof String) {
        wp.write(wp.h(object));

    } else if (object instanceof ObjectField) {
        wp.write(wp.h(((ObjectField) object).getLabel()));

    } else {
        wp.write(wp.objectLabel(object));
    }
}

private static void writeParent(ToolPageContext wp, Set<String> permissions, Object object, String permissionId) throws IOException {
    String inputName = (String) wp.getRequest().getAttribute("inputName");
    boolean hasSelf = permissions.contains(permissionId);
    boolean hasChildren = permissions.contains(permissionId + "/");

    wp.writeElement("input",
            "type", "hidden",
            "name", inputName,
            "value", permissionId);

    wp.writeStart("select",
            "id", wp.createId(),
            "name", inputName + "." + permissionId);
        wp.writeStart("option",
                "selected", hasSelf && hasChildren ? "selected" : null,
                "value", "all");
            wp.writeHtml("All ");
            writeLabel(wp, object);
        wp.writeEnd();

        wp.writeStart("option",
                "selected", hasSelf && !hasChildren ? "selected" : null,
                "value", "some");
            wp.writeHtml("Some ");
            writeLabel(wp, object);
        wp.writeEnd();

        wp.writeStart("option",
                "selected", !hasSelf && !hasChildren ? "selected" : null,
                "value", "no");
            wp.writeHtml("No ");
            writeLabel(wp, object);
        wp.writeEnd();
    wp.writeEnd();
}

private static void writeChild(ToolPageContext wp, Set<String> permissions, Object object, String permissionId) throws IOException {

    wp.write("<input");
    if (permissions.contains(permissionId)) {
        wp.write(" checked");
    }
    wp.write(" id=\"");
    wp.write(wp.createId());
    wp.write("\" name=\"");
    wp.write(wp.h(wp.getRequest().getAttribute("inputName")));
    wp.write("\" type=\"checkbox\" value=\"");
    wp.write(wp.h(permissionId));

    wp.write("\"> <label for=\"");
    wp.write(wp.getId());
    wp.write("\">");
    writeLabel(wp, object);
    wp.write("</label>");
}
%>
