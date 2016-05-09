package com.psddev.cms.db;

import java.util.List;

import com.psddev.dari.db.Record;

@Record.BootstrapPackages("Sites")
public class SiteCategory extends Record {

    @Required
    @Indexed(unique = true)
    private String name;

    @JunctionField("siteCategory")
    private List<Site> sites;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Site> getSites() {
        return sites;
    }

    public void setSites(List<Site> sites) {
        this.sites = sites;
    }

    public String getPermissionId() {
        return "siteCategory/" + getId();
    }
}
