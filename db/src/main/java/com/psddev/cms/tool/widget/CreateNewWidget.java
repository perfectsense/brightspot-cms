package com.psddev.cms.tool.widget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolRole;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DefaultDashboardWidget;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Singleton;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;

@SuppressWarnings("deprecation")
public class CreateNewWidget extends DefaultDashboardWidget {

    @Override
    public int getColumnIndex() {
        return 1;
    }

    @Override
    public int getWidgetIndex() {
        return 0;
    }

    @Override
    public void writeHtml(ToolPageContext page, Dashboard dashboard) throws IOException, ServletException {
        String redirect = page.param(String.class, "redirect");
        CmsTool.CommonContentSettings settings = null;
        ToolUser user = page.getUser();

        if (user != null) {
            ToolRole role = user.getRole();

            if (role != null) {
                settings = role.getRoleCommonContentSettings();
            }
        }

        if (settings == null) {
            Site site = page.getSite();

            if (site != null) {
                settings = site.getCommonContentSettings();
            }
        }

        if (settings == null) {
            settings = page.getCmsTool().getCommonContentSettings();
        }

        Set<ObjectType> createNewTypes = settings.getCreateNewTypes();
        Set<Content> editExistingContents = new TreeSet<Content>(settings.getEditExistingContents());
        List<TypeTemplate> typeTemplates = new ArrayList<TypeTemplate>();
        Map<ObjectType, Integer> typeCounts = new HashMap<ObjectType, Integer>();

        if (!createNewTypes.isEmpty()) {
            for (ObjectType type : createNewTypes) {
                TypeTemplate typeTemplate = new TypeTemplate(type, null);

                if (typeTemplate.getCollapsedId().equals(redirect)) {
                    page.redirect("/content/edit.jsp", "typeId", type.getId());
                    return;
                }

                typeTemplates.add(typeTemplate);
                typeCounts.put(type, 1);
            }

        } else {
            for (com.psddev.cms.db.Template template : Query
                    .from(com.psddev.cms.db.Template.class)
                    .where(page.siteItemsPredicate())
                    .sortAscending("name")
                    .selectAll()) {

                for (ObjectType type : template.getContentTypes()) {
                    if (type.getGroups().contains(Singleton.class.getName())) {
                        continue;
                    }

                    TypeTemplate typeTemplate = new TypeTemplate(type, template);

                    if (typeTemplate.getCollapsedId().equals(redirect)) {
                        page.redirect("/content/edit.jsp", "typeId", type.getId(), "templateId", template.getId());
                        return;
                    }

                    typeTemplates.add(typeTemplate);
                    typeCounts.put(type, typeCounts.containsKey(type) ? typeCounts.get(type) + 1 : 1);
                }
            }

            for (ObjectType type : Database.Static.getDefault().getEnvironment().getTypes()) {
                if (type.isConcrete()
                        && type.getGroups().contains(Directory.Item.class.getName())
                        && !type.getGroups().contains(Singleton.class.getName())
                        && !typeCounts.containsKey(type)
                        && page.hasPermission("type/" + type.getId() + "/write")) {
                    TypeTemplate typeTemplate = new TypeTemplate(type, null);

                    if (typeTemplate.getCollapsedId().equals(redirect)) {
                        page.redirect("/content/edit.jsp", "typeId", type.getId());
                        return;
                    }

                    typeTemplates.add(typeTemplate);
                    typeCounts.put(type, 1);
                }
            }
        }

        Collections.sort(typeTemplates);

        List<TypeTemplate> favorites = new ArrayList<TypeTemplate>();
        List<TypeTemplate> collapsed = new ArrayList<TypeTemplate>();

        if (page.isFormPost()) {
            Set<String> collapsedIds = new HashSet<String>();

            for (TypeTemplate typeTemplate : typeTemplates) {
                if (page.param(boolean.class, typeTemplate.getParameterName())) {
                    favorites.add(typeTemplate);
                } else {
                    collapsed.add(typeTemplate);
                    collapsedIds.add(typeTemplate.getCollapsedId());
                }
            }

            user.as(ToolUserCreateNewSettings.class).setCollapsedIds(collapsedIds);
            user.save();

        } else {
            Set<String> collapsedIds = user.as(ToolUserCreateNewSettings.class).getCollapsedIds();

            if (collapsedIds == null) {
                favorites = typeTemplates;

            } else {
                for (TypeTemplate typeTemplate : typeTemplates) {
                    if (collapsedIds.contains(typeTemplate.getCollapsedId())) {
                        collapsed.add(typeTemplate);
                    } else {
                        favorites.add(typeTemplate);
                    }
                }
            }
        }

        String widgetId = page.createId();

        page.writeStart("div", "class", "widget p-commonContent", "id", widgetId);
            page.writeStart("h1", "class", "icon icon-file").writeHtml("Common Content").writeEnd();

            if (page.param(boolean.class, "customize")) {
                page.writeStart("form",
                        "method", "post",
                        "action", page.url(null));

                    page.writeStart("table", "class", "table-striped");
                        page.writeStart("thead");
                            page.writeStart("tr");
                                page.writeStart("th").writeEnd();
                                page.writeStart("th", "class", "p-commonContent-favorite").writeHtml("Favorite?").writeEnd();
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("tbody");
                            for (TypeTemplate typeTemplate : typeTemplates) {
                                page.writeStart("tr");
                                    page.writeStart("td").writeHtml(getTypeTemplateLabel(typeCounts, typeTemplate)).writeEnd();

                                    page.writeStart("td", "class", "p-commonContent-favorite checkboxContainer");
                                        page.writeElement("input",
                                                "type", "checkbox",
                                                "id", page.getId(),
                                                "name", typeTemplate.getParameterName(),
                                                "value", "true",
                                                "checked", collapsed.contains(typeTemplate) ? null : "checked");
                                    page.writeEnd();
                                page.writeEnd();
                            }
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("div", "class", "actions");
                        page.writeStart("button",
                                "class", "action action-save");
                            page.writeHtml("Save");
                        page.writeEnd();

                        page.writeStart("a",
                                "class", "action action-cancel action-pullRight",
                                "href", page.url(null));
                            page.writeHtml("Cancel");
                        page.writeEnd();
                    page.writeEnd();

                page.writeEnd();

            } else {
                page.writeStart("div", "class", "widgetControls");
                    page.writeStart("a",
                            "class", "action action-customize",
                            "href", page.url("", "customize", "true"));
                        page.writeHtml("Customize");
                    page.writeEnd();
                page.writeEnd();

                Set<UUID> automaticallySavedDraftIds = user.getAutomaticallySavedDraftIds();
                List<Object> automaticallySavedDrafts = Query
                        .from(Object.class)
                        .where("_id = ?", automaticallySavedDraftIds)
                        .selectAll();

                if (!automaticallySavedDrafts.isEmpty()) {
                    boolean removed = false;

                    for (Iterator<Object> i = automaticallySavedDrafts.iterator(); i.hasNext();) {
                        State draft = State.getInstance(i.next());

                        if (!draft.as(Content.ObjectModification.class).isDraft()) {
                            removed = true;

                            automaticallySavedDraftIds.remove(draft.getId());
                            i.remove();
                        }
                    }

                    if (removed) {
                        user.save();
                    }

                    if (!automaticallySavedDrafts.isEmpty()) {
                        page.writeStart("h2").writeHtml("Automatically Saved Drafts").writeEnd();

                        page.writeStart("ul", "class", "links");
                            for (Object draft : automaticallySavedDrafts) {
                                page.writeStart("li");
                                    page.writeStart("a",
                                            "target", "_top",
                                            "href", page.objectUrl("/content/edit.jsp", draft));
                                        page.writeTypeObjectLabel(draft);
                                    page.writeEnd();
                                page.writeEnd();
                            }
                        page.writeEnd();
                    }
                }

                page.writeStart("div", "class", "p-commonContent-new", "style", page.cssString(
                        "-moz-box-sizing", "border-box",
                        "-webkit-box-sizing", "border-box",
                        "box-sizing", "border-box",
                        "float", "left",
                        "padding-right", "5px",
                        "width", "50%"));
                    page.writeStart("h2").writeHtml("Create New").writeEnd();

                    page.writeStart("ul", "class", "links pageThumbnails");
                        for (TypeTemplate typeTemplate : favorites) {
                            ObjectType type = typeTemplate.getType();
                            com.psddev.cms.db.Template template = typeTemplate.getTemplate();
                            State state = State.getInstance(Query.fromType(type).where("cms.template.default = ?", template).first());
                            String permalink = null;

                            if (state != null) {
                                permalink = state.as(Directory.ObjectModification.class).getPermalink();
                            }

                            page.writeStart("li", "data-preview-url", permalink);
                                page.writeStart("a",
                                        "target", "_top",
                                        "href", page.url("/content/edit.jsp",
                                                "typeId", type.getId(),
                                                "templateId", template != null ? template.getId() : null));
                                    page.writeHtml(getTypeTemplateLabel(typeCounts, typeTemplate));
                                page.writeEnd();
                            page.writeEnd();
                        }
                    page.writeEnd();

                    if (!collapsed.isEmpty()) {
                        page.writeStart("form",
                                "method", "get",
                                "action", page.url(null),
                                "target", "_top");
                            page.writeStart("select", "name", "redirect");
                                for (TypeTemplate typeTemplate : collapsed) {
                                    page.writeStart("option", "value", typeTemplate.getCollapsedId());
                                        page.writeHtml(getTypeTemplateLabel(typeCounts, typeTemplate));
                                    page.writeEnd();
                                }
                            page.writeEnd();

                            page.writeHtml(" ");

                            page.writeStart("button", "class", "action action-create");
                                page.writeHtml("New");
                            page.writeEnd();
                        page.writeEnd();
                    }
                page.writeEnd();

                if (editExistingContents.isEmpty()) {
                    for (Object item : Query
                            .from(Object.class)
                            .where("_type = ?", Database.Static.getDefault().getEnvironment().getTypesByGroup(Singleton.class.getName()))
                            .selectAll()) {
                        if (item instanceof Content) {
                            editExistingContents.add((Content) item);
                        }
                    }
                }

                if (!editExistingContents.isEmpty()) {
                    page.writeStart("div", "class", "p-commonContent-existing", "style", page.cssString(
                            "-moz-box-sizing", "border-box",
                            "-webkit-box-sizing", "border-box",
                            "box-sizing", "border-box",
                            "float", "left",
                            "padding-left", "5px",
                            "width", "50%"));
                        page.writeStart("h2").writeHtml("Edit Existing").writeEnd();

                        page.writeStart("ul", "class", "links pageThumbnails");
                            for (Object content : editExistingContents) {
                                page.writeStart("li");
                                    page.writeStart("a",
                                            "target", "_top",
                                            "href", page.objectUrl("/content/edit.jsp", content));
                                        page.writeTypeObjectLabel(content);
                                    page.writeEnd();
                                page.writeEnd();
                            }
                        page.writeEnd();
                    page.writeEnd();
                }

                page.writeStart("div", "style", page.cssString(
                        "clear", "both"));
                page.writeEnd();
            }
        page.writeEnd();
    }

    @FieldInternalNamePrefix("toolUserCreateNewSettings.")
    private static class ToolUserCreateNewSettings extends Modification<ToolUser> {

        @ToolUi.Hidden
        private Set<String> collapsedIds;

        public Set<String> getCollapsedIds() {
            if (collapsedIds == null) {
                collapsedIds = new LinkedHashSet<>();
            }
            return collapsedIds;
        }

        public void setCollapsedIds(Set<String> collapsedIds) {
            this.collapsedIds = collapsedIds;
        }
    }

    private static String getTypeTemplateLabel(Map<ObjectType, Integer> typeCounts, TypeTemplate typeTemplate) {
        ObjectType type = typeTemplate.getType();
        com.psddev.cms.db.Template template = typeTemplate.getTemplate();
        StringBuilder label = new StringBuilder();

        label.append(ToolPageContext.Static.getObjectLabel(type));

        if (template != null && typeCounts.get(type) > 1) {
            label.append(" - ");
            label.append(ToolPageContext.Static.getObjectLabel(template));
        }

        return label.toString();
    }

    private static class TypeTemplate implements Comparable<TypeTemplate> {

        private final ObjectType type;
        private final com.psddev.cms.db.Template template;

        public TypeTemplate(ObjectType type, com.psddev.cms.db.Template template) {
            this.type = type;
            this.template = template;
        }

        public ObjectType getType() {
            return type;
        }

        public com.psddev.cms.db.Template getTemplate() {
            return template;
        }

        public String getParameterName() {
            StringBuilder name = new StringBuilder();

            name.append("favorite.");
            name.append(type.getId());
            name.append('.');

            if (template != null) {
                name.append(template.getId());
            }

            return name.toString();
        }

        public String getCollapsedId() {
            StringBuilder id = new StringBuilder();

            id.append(type.getId());
            id.append(',');

            if (template != null) {
                id.append(template.getId());
            }

            return id.toString();
        }

        @Override
        public int compareTo(TypeTemplate other) {
            int comparison = type.compareTo(other.getType());
            return comparison == 0 ? ObjectUtils.compare(template, other.getTemplate(), true) : comparison;
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hashCode(type, template);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;

            } else if (other instanceof TypeTemplate) {
                TypeTemplate o = (TypeTemplate) other;
                return type.equals(o.getType()) && ObjectUtils.equals(template, o.getTemplate());

            } else {
                return false;
            }
        }
    }

}
