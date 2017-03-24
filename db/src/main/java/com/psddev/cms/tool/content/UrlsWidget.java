package com.psddev.cms.tool.content;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Localization;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.Workflow;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.ContentEditSection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.UpdatingContentEditWidget;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UrlsWidget extends UpdatingContentEditWidget {

    @Override
    public ContentEditSection getSection(ToolPageContext page, Object content) {
        return ContentEditSection.RIGHT;
    }

    @Override
    public double getPosition(ToolPageContext page, Object content, ContentEditSection section) {
        return -30.0;
    }

    @Override
    public String getHeading(ToolPageContext page, Object content) {
        return Localization.currentUserText(getClass(), "title");
    }

    @Override
    @SuppressWarnings("deprecation")
    public Collection<Class<? extends UpdatingContentEditWidget>> getUpdateDependencies() {
        return Collections.singleton(TemplateWidget.class);
    }

    @Override
    public void displayOrUpdate(ToolPageContext page, Object content, ContentEditSection section) throws IOException {
        Object original = page.getRequest().getAttribute("original");

        if (original == null) {
            original = content;
        }

        Site site = page.getSite();
        State state = State.getInstance(original);

        String namePrefix = state.getId() + "/directory.";
        String automaticName = namePrefix + "automatic";
        String automaticContainerId = page.createId();
        String pathName = namePrefix + "path";
        String removeName = namePrefix + "remove";
        String typeName = namePrefix + "type";
        String siteIdName = namePrefix + "siteId";

        Directory.Data dirData = state.as(Directory.Data.class);
        Map<UUID, Site> sites = new CompactMap<>();

        ToolUser user = page.getUser();

        Set<Site> userSites = new HashSet<>();
        if (user != null) {
            userSites.addAll(user.findOtherAccessibleSites());
            userSites.add(user.getCurrentSite());
        }

        for (Site s : Query
                .from(Site.class)
                .where("name != missing")
                .sortAscending("name")
                .selectAll()) {

            if (userSites.contains(s)) {
                sites.put(s.getId(), s);
            }
        }

        CmsTool cms = page.getCmsTool();

        if (cms.isAlwaysGeneratePermalinks()) {
            if (section == null) {
                dirData.setPathsMode(page.param(boolean.class, automaticName) ? null : Directory.PathsMode.MANUAL);
                dirData.clearPaths();

                List<String> paths = page.params(String.class, pathName);
                List<UUID> siteIds = page.params(UUID.class, siteIdName);
                List<Directory.PathType> types = page.params(Directory.PathType.class, typeName);

                for (int i = 0, size = Math.min(paths.size(), types.size()); i < size; i ++) {
                    if (!page.param(boolean.class, removeName + "." + i)) {
                        dirData.addPath(i < siteIds.size() ? sites.get(siteIds.get(i)) : null, paths.get(i), types.get(i));
                    }
                }

                // Automatically generate URLs if requested.
                if (!Directory.PathsMode.MANUAL.equals(dirData.getPathsMode())) {
                    Set<Directory.Path> oldPaths = new LinkedHashSet<>(dirData.getPaths());

                    for (Directory.Path path : State.getInstance(content).as(Directory.ObjectModification.class).createPaths(site)) {
                        dirData.addPath(path.getSite(), path.getPath(), path.getType());
                    }

                    Set<Directory.Path> newPaths = new LinkedHashSet<>(dirData.getPaths());

                    newPaths.removeAll(oldPaths);
                    state.getExtras().put("cms.newPaths", newPaths);
                }

                return;
            }

            writeErrors(page, state);
            writeAutomaticContainer(page, automaticName, dirData, automaticContainerId);

            Set<Directory.Path> paths = dirData.getPaths();

            if (!paths.isEmpty()
                    && !state.isNew()
                    && !state.as(Content.ObjectModification.class).isDraft()
                    && (Directory.PathsMode.MANUAL.equals(dirData.getPathsMode())
                    || !page.getCmsTool().isSingleGeneratedPermalink()
                    || State.getInstance(content).as(Directory.ObjectModification.class).createPaths(site).isEmpty())) {

                writePaths(page, paths, sites, pathName, removeName, siteIdName, typeName);
            }

        } else {
            boolean initialDraft = state.isNew()
                    || state.as(Content.ObjectModification.class).isDraft()
                    || state.as(Workflow.Data.class).getCurrentState() != null;

            if (section == null) {
                dirData.setPathsMode(page.param(boolean.class, automaticName) ? null : Directory.PathsMode.MANUAL);

                Set<Directory.Path> viewOnlyPaths = !ObjectUtils.isBlank(dirData.getPaths())
                        ? dirData.getPaths()
                                .stream()
                                .filter(path -> (path.getSite() == null && !user.hasPermission("site/global"))
                                        || (path.getSite() != null && !user.hasPermission(path.getSite().getPermissionId())))
                                .collect(Collectors.toSet())
                        : null;

                dirData.clearPaths();

                if (!ObjectUtils.isBlank(viewOnlyPaths)) {
                    for (Directory.Path path : viewOnlyPaths) {
                        dirData.addPath(path.getSite(), path.getPath(), path.getType());
                    }
                }

                List<String> paths = page.params(String.class, pathName);
                List<UUID> siteIds = page.params(UUID.class, siteIdName);
                List<Directory.PathType> types = page.params(Directory.PathType.class, typeName);

                for (int i = 0, size = Math.min(paths.size(), types.size()); i < size; i ++) {
                    if (!page.param(boolean.class, removeName + "." + i)) {
                        Site removeSite = i < siteIds.size() ? sites.get(siteIds.get(i)) : null;
                        if ((removeSite != null && user.hasPermission(removeSite.getPermissionId())) || (removeSite == null && user.hasPermission("site/global"))) {
                            dirData.addPath(i < siteIds.size() ? sites.get(siteIds.get(i)) : null, paths.get(i), types.get(i));
                        }
                    }
                }

                // Automatically generate URLs if requested.
                if (initialDraft) {
                    if (!Directory.PathsMode.MANUAL.equals(dirData.getPathsMode())) {
                        Set<Directory.Path> oldPaths = new LinkedHashSet<>(dirData.getPaths());
                        Set<String> oldRawPaths = new LinkedHashSet<>(dirData.getRawPaths());

                        dirData.clearPaths();

                        for (Directory.Path path : State.getInstance(content).as(Directory.ObjectModification.class).createPaths(site)) {
                            dirData.addPath(path.getSite(), path.getPath(), path.getType());
                        }

                        Set<Directory.Path> newPaths = new LinkedHashSet<>(dirData.getPaths());
                        Set<String> newRawPaths = new LinkedHashSet<>(dirData.getRawPaths());

                        dirData.clearPaths();

                        Stream.concat(oldPaths.stream(), newPaths.stream())
                                .forEach(p -> dirData.addPath(p.getSite(), p.getPath(), p.getType()));

                        state.getExtras().put("cms.newPaths", newPaths);
                        dirData.setAutomaticRawPaths(newRawPaths);

                    } else {
                        dirData.setAutomaticRawPaths(null);
                    }

                } else {
                    dirData.setPathsMode(Directory.PathsMode.MANUAL);
                    dirData.setAutomaticRawPaths(null);
                }

                return;
            }

            writeErrors(page, state);

            if (initialDraft) {
                writeAutomaticContainer(page, automaticName, dirData, automaticContainerId);

            } else if (!Directory.PathsMode.MANUAL.equals(dirData.getPathsMode())) {
                page.writeElement("input",
                        "type", "hidden",
                        "name", automaticName,
                        "value", true);
            }

            Set<Directory.Path> paths = initialDraft ? dirData.getManualPaths() : dirData.getPaths();

            if (!paths.isEmpty()) {
                writePaths(page, paths, sites, pathName, removeName, siteIdName, typeName);
            }
        }

        page.writeStart("div", "class", "widget-urlsRepeatable repeatableInputs");
        {
            page.writeStart("ul");
            {
                page.writeStart("script", "type", "text/template");
                {
                    page.writeStart("li", "class", "widget-urlsItem", "data-type", "URL");
                    {
                        page.writeStart("textarea", "class", "widget-urlsItemLabel", "name", pathName);
                        page.writeEnd();

                        if (!sites.isEmpty()) {
                            page.writeStart("select", "name", siteIdName);
                            {
                                if (user != null && user.hasPermission("site/global")) {
                                    page.writeStart("option", "value", "");
                                    page.writeHtml("Global");
                                    page.writeEnd();
                                }

                                for (Site s : sites.values()) {
                                    page.writeStart("option", "value", s.getId(), "selected", s.equals(site) ? "selected" : null);
                                    page.writeObjectLabel(s);
                                    page.writeEnd();
                                }
                            }
                            page.writeEnd();
                            page.writeHtml(" ");
                        }

                        page.writeStart("select", "name", typeName);
                        {
                            for (Directory.PathType pathType : Directory.PathType.values()) {
                                page.writeStart("option", "value", pathType.name());
                                page.writeHtml(pathType);
                                page.writeEnd();
                            }
                        }
                        page.writeEnd();
                    }
                    page.writeEnd();
                }
                page.writeEnd();
            }
            page.writeEnd();
        }
        page.writeEnd();

        page.writeStart("script", "type", "text/javascript");
        {
            page.writeRaw("(function($, window, undefined) {");
            {
                page.writeRaw("var $automaticContainer = $('#").writeRaw(automaticContainerId).writeRaw("'),");
                page.writeRaw("$form = $automaticContainer.closest('form');");

                page.writeRaw("$form.bind('cms-updateContentState', function(event, data) {");
                page.writeRaw("$automaticContainer.html(data._urlWidgetHtml || '');");
                page.writeRaw("});");
            }
            page.writeRaw("})(jQuery, window);");
        }
        page.writeEnd();
    }

    private void writeErrors(ToolPageContext page, State state) throws IOException {
        List<String> errors = state.getErrors(state.getField(Directory.PATHS_FIELD));

        if (!ObjectUtils.isBlank(errors)) {
            page.writeStart("div", "class", "message message-error");
            {
                for (String error : errors) {
                    page.writeHtml(error);
                }
            }
            page.writeEnd();
        }
    }

    private void writeAutomaticContainer(ToolPageContext page, String automaticName, Directory.Data dirData, String automaticContainerId) throws IOException {
        page.writeStart("div", "class", "widget-urlsAutomatic");
        {
            page.writeStart("label");
            {
                page.writeElement("input",
                        "type", "checkbox",
                        "name", automaticName,
                        "value", "true",
                        "checked", Directory.PathsMode.MANUAL.equals(dirData.getPathsMode()) ? null : "checked");

                page.writeHtml(" Generate Permalink?");
            }
            page.writeEnd();

            page.writeStart("div", "id", automaticContainerId);
            page.writeEnd();
        }
        page.writeEnd();
    }

    private void writePaths(ToolPageContext page, Set<Directory.Path> paths, Map<UUID, Site> sites, String pathName, String removeName, String siteIdName, String typeName) throws IOException {
        ToolUser user = page.getUser();
        int index = 0;

        page.writeStart("ul", "class", "widget-urls");
        {
            for (Directory.Path path : paths) {
                Site pathSite = path.getSite();
                String pathPath = path.getPath();
                String pathDisplay = ObjectUtils.firstNonNull(Directory.extractExternalUrl(pathPath), pathPath);
                String href = pathSite != null ? pathSite.getPrimaryUrl() + pathPath : pathPath;

                while (href.endsWith("*")) {
                    href = href.substring(0, href.length() - 1);
                }

                if (user != null
                        && ((path.getSite() == null && !user.hasPermission("site/global"))
                        || (path.getSite() != null && !user.hasPermission(path.getSite().getPermissionId())))) {

                    page.writeStart("li", "class", "widget-urlsItem");
                    {
                        page.writeStart("div", "class", "widget-urlsItemLabel");
                        {
                            page.writeStart("a", "href", href, "target", "_blank");
                            page.writeHtml(pathDisplay);
                            page.writeEnd();
                        }
                        page.writeEnd();

                        page.writeStart("label");
                        {
                            if (path.getSite() == null) {
                                page.writeHtml("Global");

                            } else {
                                page.writeObjectLabel(path.getSite());
                            }

                            if (path.getType() != null) {
                                page.writeHtml(": ");
                                page.writeHtml(path.getType());
                            }
                        }
                        page.writeEnd();
                    }
                    page.writeEnd();

                } else {

                    page.writeStart("li", "class", "widget-urlsItem");
                    {
                        page.writeElement("input",
                                "type", "hidden",
                                "id", page.createId(),
                                "name", pathName,
                                "value", pathPath);

                        page.writeStart("div", "class", "widget-urlsItemLabel");
                        {
                            page.writeStart("a", "href", href, "target", "_blank");
                            page.writeHtml(pathDisplay);
                            page.writeEnd();

                            page.writeStart("label",
                                    "class", "widget-urlsItemRemove");
                            {
                                page.writeHtml(" ");

                                page.writeElement("input",
                                        "type", "checkbox",
                                        "name", removeName + "." + index,
                                        "value", "true");

                                page.writeStart("span", "class", "widget-urlsItemRemoveText");
                                page.writeHtml("Remove");
                                page.writeEnd();
                            }
                            page.writeEnd();
                        }
                        page.writeEnd();

                        if (!sites.isEmpty()) {
                            page.writeStart("select", "name", siteIdName);
                            {
                                if (user != null && user.hasPermission("site/global")) {
                                    page.writeStart("option", "value", "");
                                    page.writeHtml("Global");
                                    page.writeEnd();
                                }

                                for (Site s : sites.values()) {
                                    page.writeStart("option",
                                            "selected", s.equals(path.getSite()) ? "selected" : null,
                                            "value", s.getId());
                                    page.writeObjectLabel(s);
                                    page.writeEnd();
                                }
                            }
                            page.writeEnd();
                            page.writeHtml(" ");
                        }

                        page.writeStart("select", "name", typeName);
                        {
                            for (Directory.PathType pathType : Directory.PathType.values()) {
                                page.writeStart("option",
                                        "selected", pathType.equals(path.getType()) ? "selected" : null,
                                        "value", pathType.name());
                                page.writeHtml(pathType);
                                page.writeEnd();
                            }
                        }
                        page.writeEnd();
                    }
                    page.writeEnd();

                    ++index;
                }
            }
        }
        page.writeEnd();
    }
}
