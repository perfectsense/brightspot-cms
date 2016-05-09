package com.psddev.cms.db;

import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;

import java.io.IOException;
import java.util.Map;

public class ToolUserSearch extends Record {

    @Indexed
    private String key;

    private String queryString;
    private ObjectType selectedType;
    private int filtersCount;
    private String search;

    public static Query<ToolUserSearch> createQuery(ToolUser user, String recentName) {
        return Query.from(ToolUserSearch.class)
                .where("key startsWith ?", user.getId().toString() + recentName)
                .sortDescending("key");
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public ObjectType getSelectedType() {
        return selectedType;
    }

    public void setSelectedType(ObjectType selectedType) {
        this.selectedType = selectedType;
    }

    public int getFiltersCount() {
        return filtersCount;
    }

    public void setFiltersCount(int filtersCount) {
        this.filtersCount = filtersCount;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String toLocalizedLabel(ToolPageContext page) throws IOException {
        String selectedTypeLabel = selectedType != null ? selectedType.getLabel() : null;
        String key;

        if (!ObjectUtils.isBlank(queryString)) {
            if (selectedTypeLabel != null) {
                if (filtersCount > 0) {
                    key = "recentSearch.queryTypeFilters";

                } else {
                    key = "recentSearch.queryType";
                }

            } else {
                if (filtersCount > 0) {
                    key = "recentSearch.queryFilters";

                } else {
                    key = "recentSearch.query";
                }
            }

        } else if (selectedType != null) {
            if (filtersCount > 0) {
                key = "recentSearch.typeFilters";

            } else {
                key = "recentSearch.type";
            }

        } else if (filtersCount > 0) {
            key = "recentSearch.filters";

        } else {
            key = null;
        }

        if (key != null) {
            Map<String, Object> nameOptions = new CompactMap<>();

            nameOptions.put("queryString", queryString);
            nameOptions.put("selectedTypeLabel", selectedTypeLabel);
            nameOptions.put("filtersCount", filtersCount);

            return page.localize(getClass(), nameOptions, key);

        } else {
            return null;
        }
    }
}
