package com.psddev.cms.tool;

import com.psddev.dari.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** An areas within the tool UI. */
public class Area extends Plugin {

    private String hierarchy;
    private String url;

    /**
     * Returns the {@code /}-delimited hierarchy used to display the
     * navigation.
     */
    public String getHierarchy() {
        if (hierarchy == null) {
            String path = getInternalName();

            for (Area parent = this; (parent = parent.getParent()) != null;) {
                path = parent.getInternalName() + "/" + path;
            }

            hierarchy = path;
        }

        return hierarchy;
    }

    /**
     * Sets the {@code /}-delimited hierarchy used to display the
     * navigation.
     */
    public void setHierarchy(String hierarchy) {
        this.hierarchy = hierarchy;
    }

    /** Returns the URL. */
    public String getUrl() {
        return url;
    }

    /** Sets the URL. */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the unique ID that represents this area for use in
     * permissions.
     */
    public String getPermissionId() {
        return "area/" + getHierarchy();
    }

    /**
     * Returns {@code true} if this area or any of its children are
     * associated with the given {@code tool} and {@code path}.
     */
    public boolean isSelected(Tool tool, String path) {
        Tool areaTool = getTool();

        if (!ObjectUtils.equals(areaTool, tool)) {
            return false;
        }

        Area selected = null;
        List<Area> areas = Tool.Static.getPluginsByClass(Area.class);

        for (Area area : areas) {
            if (ObjectUtils.equals(area.getUrl(), path)) {
                selected = area;
                break;
            }
        }

        if (selected == null) {
            Collections.sort(areas, new Comparator<Area>() {
                @Override
                public int compare(Area x, Area y) {
                    String xUrl = x.getUrl();
                    String yUrl = y.getUrl();
                    int xUrlLength = xUrl != null ? xUrl.length() : 0;
                    int yUrlLength = yUrl != null ? yUrl.length() : 0;
                    return xUrlLength < yUrlLength ? 1 : xUrlLength > yUrlLength ? -1 : 0;
                }
            });
            for (Area area : areas) {
                if (ObjectUtils.equals(area.getTool(), tool)
                        && area.getUrl() != null
                        && area.getUrl().endsWith("/")
                        && path.startsWith(area.getUrl())) {
                    selected = area;
                    break;
                }
            }
        }

        if (selected == null) {
            for (Area area : Tool.Static.getTopAreas()) {
                selected = area;
                break;
            }
        }

        if (getInternalName().equals(selected.getInternalName())) {
            return true;
        }

        for (Area child : findChildren()) {
            if (child.isSelected(tool, path)) {
                return true;
            }
        }

        return false;
    }

    /** Returns {@code true} if this area has any children. */
    public boolean hasChildren() {
        String hierarchy = getHierarchy() + "/";

        for (Area area : Tool.Static.getPluginsByClass(Area.class)) {
            if (area.getHierarchy().startsWith(hierarchy)) {
                return true;
            }
        }

        return false;
    }

    /** Returns all child areas. */
    public List<Area> getChildren() {
        String hierarchy = getHierarchy() + "/";
        List<Area> children = new ArrayList<Area>();

        for (Area area : Tool.Static.getPluginsByClass(Area.class)) {
            if (area.getHierarchy().startsWith(hierarchy)) {
                children.add(area);
            }
        }

        return children;
    }

    // --- Deprecated ---

    /** @deprecated Use {@link #isSelected(Tool, String)} instead. */
    @Deprecated
    public boolean isSelected(String path) {
        return false;
    }

    @Deprecated
    private Area parent;

    /** @deprecated Use {@link #getHierarchy} instead. */
    @Deprecated
    public Area getParent() {
        return parent;
    }

    /** @deprecated Use {@link #setHierarchy} instead. */
    @Deprecated
    public void setParent(Area parent) {
        this.parent = parent;
    }

    /** @deprecated Use {@link #getChildren} instead. */
    @Deprecated
    public List<Area> findChildren() {
        return getChildren();
    }
}
