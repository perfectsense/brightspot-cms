package com.psddev.cms.db;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Recordable.Embedded
public class SiteSpecificContentTemplates extends Record {

    @Required
    @ToolUi.DropDown
    private Set<Site> sites;

    @Required
    private List<ContentTemplate> contentTemplates;

    public Set<Site> getSites() {
        if (sites == null) {
            sites = new LinkedHashSet<>();
        }
        return sites;
    }

    public void setSites(Set<Site> sites) {
        this.sites = sites;
    }

    public List<ContentTemplate> getContentTemplates() {
        if (contentTemplates == null) {
            contentTemplates = new ArrayList<>();
        }
        return contentTemplates;
    }

    public void setContentTemplates(List<ContentTemplate> contentTemplates) {
        this.contentTemplates = contentTemplates;
    }

    @Override
    public String getLabel() {
        Set<Site> sites = getSites();
        List<ContentTemplate> contentTemplates = getContentTemplates();

        if (sites.isEmpty() && contentTemplates.isEmpty()) {
            return super.getLabel();
        }

        StringBuilder label = new StringBuilder();

        if (!sites.isEmpty()) {
            label.append("On ");

            label.append(sites.stream()
                    .map(Site::getLabel)
                    .collect(Collectors.joining(", ")));

            label.append(": ");
        }

        if (!contentTemplates.isEmpty()) {
            label.append(contentTemplates.stream()
                    .map(ContentTemplate::getLabel)
                    .collect(Collectors.joining(", ")));
        }

        return label.toString();
    }
}
