package com.psddev.cms.tool.content;

import com.psddev.cms.db.Global;
import com.psddev.cms.db.Localization;
import com.psddev.cms.db.Site;
import com.psddev.cms.tool.ContentEditSection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.UpdatingContentEditWidget;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SitesWidget extends UpdatingContentEditWidget {

    @Override
    public ContentEditSection getSection(ToolPageContext page, Object content) {
        return ContentEditSection.RIGHT;
    }

    @Override
    public double getPosition(ToolPageContext page, Object content, ContentEditSection section) {
        return -20.0;
    }

    @Override
    public String getHeading(ToolPageContext page, Object content) {
        return Localization.currentUserText(getClass(), "title");
    }

    @Override
    public void displayOrUpdate(ToolPageContext page, Object content, ContentEditSection section) throws IOException {
        List<Site> allSites = Site.Static.findAll();

        if (allSites.isEmpty()) {
            return;
        }

        Object original = page.getRequest().getAttribute("original");

        if (original == null) {
            original = content;
        }

        if (original instanceof Global) {
            return;
        }

        State state = State.getInstance(original);
        Site.ObjectModification siteData = state.as(Site.ObjectModification.class);

        String namePrefix = state.getId() + "/sites/";
        String ownerName = namePrefix + "owner";
        String accessName = namePrefix + "access";
        String consumerIdName = namePrefix + "consumerId";

        Site owner = siteData.getOwner();
        Set<Site> consumers = siteData.getConsumers();

        if (section == null) {
            if (owner == null || page.hasPermission(owner.getPermissionId())) {
                owner = Query.from(Site.class).where("_id = ?", page.param(UUID.class, ownerName)).first();
                siteData.setOwner(owner);
            }

            String access = page.param(String.class, accessName);
            consumers.clear();

            if ("no".equals(access)) {
                siteData.setGlobal(false);
                siteData.setBlacklist(null);
                siteData.setConsumers(null);

            } else if ("all".equals(access)) {
                siteData.setGlobal(true);
                siteData.setBlacklist(null);
                siteData.setConsumers(null);

            } else if ("some".equals(access)) {
                siteData.setGlobal(false);
                List<UUID> consumerIds = page.params(UUID.class, consumerIdName);
                for (Site site : allSites) {
                    if (consumerIds.contains(site.getId())) {
                        consumers.add(site);
                    }
                }
            }

            return;
        }

        String sitesContainerId = page.createId();
        String access = siteData.isGlobal()
                ? "all" : (consumers.isEmpty()
                        ? "no"
                        : "some");

        page.writeStart("div", "class", "ContentEditSites-owner");
        {
            page.writeStart("label", "for", page.createId());
            page.writeHtml(page.localize(getClass(), "label.owner"));
            page.writeEnd();

            if (owner != null && !page.hasPermission(owner.getPermissionId())) {
                page.writeStart("div");
                page.getObjectLabel(owner);
                page.writeEnd();

            } else {
                page.writeStart("select",
                        "class", "toggleable",
                        "data-root", ".widget",
                        "name", ownerName,
                        "style", "width: 100%;");
                {
                    page.writeStart("option",
                            "selected", owner == null ? "selected" : null,
                            "value", "",
                            "data-show", ".siteItem");
                    page.writeHtml("None");
                    page.writeEnd();

                    for (Site site : allSites) {
                        if (page.hasPermission(site.getPermissionId())) {
                            page.writeStart("option",
                                    "selected", site.equals(owner) ? "selected" : null,
                                    "value", site.getId(),
                                    "data-show", ".siteItem:not(.siteItem-" + site.getId() + ")",
                                    "data-hide", ".siteItem-" + site.getId());
                            page.writeObjectLabel(site);
                            page.writeEnd();
                        }
                    }
                }
                page.writeEnd();
            }
        }
        page.writeEnd();

        page.writeStart("div", "class", "ContentEditSites-access");
        {
            page.writeStart("label", "for", page.createId());
            page.writeHtml(page.localize(getClass(), "label.access"));
            page.writeEnd();

            page.writeStart("select", "class", "toggleable", "id", page.getId(), "name", accessName, "style", "width: 100%;");
            {
                page.writeStart("option",
                        "selected", "no".equals(access) ? "selected" : null,
                        "data-hide", "#" + sitesContainerId,
                        "value", "no");
                page.writeHtml(page.localize(getClass(), "option.none"));
                page.writeEnd();

                page.writeStart("option",
                        "selected", "all".equals(access) ? "selected" : null,
                        "data-hide", "#" + sitesContainerId,
                        "value", "all");
                page.writeHtml(page.localize(getClass(), "option.all"));
                page.writeEnd();

                page.writeStart("option",
                        "selected", "some".equals(access) ? "selected" : null,
                        "data-show", "#" + sitesContainerId,
                        "value", "some");
                page.writeHtml(page.localize(getClass(), "option.some"));
                page.writeEnd();
            }
            page.writeEnd();

            page.writeStart("ul", "id", sitesContainerId);
            {
                for (Site site : allSites) {
                    if (page.hasPermission(site.getPermissionId())) {
                        page.writeStart("li", "class", "siteItem siteItem-" + site.getId());
                        {
                            page.writeElement("input",
                                    "checked", consumers.contains(site) ? "checked" : null,
                                    "id", page.createId(),
                                    "name", consumerIdName,
                                    "type", "checkbox",
                                    "value", site.getId());

                            page.writeStart("label", "for", page.getId());
                            page.writeObjectLabel(site);
                            page.writeEnd();
                        }
                        page.writeEnd();

                    } else if (consumers.contains(site)) {
                        page.writeElement("input",
                                "name", consumerIdName,
                                "type", "hidden",
                                "value", site.getId());
                    }
                }
            }
            page.writeEnd();
        }
        page.writeEnd();
    }
}
