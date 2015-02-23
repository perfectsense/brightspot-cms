package com.psddev.cms.db;

import java.util.LinkedHashSet;
import java.util.Set;

import com.psddev.dari.db.Record;
import com.psddev.dari.util.ObjectUtils;

/**
 * @deprecated No replacement. Create your own.
 */
@Deprecated
@GridContext.Embedded
public class GridContext extends Record {

    @Required
    private String context;

    @Required
    private Set<Integer> areas;

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Set<Integer> getAreas() {
        if (areas == null) {
            areas = new LinkedHashSet<Integer>();
        }
        return areas;
    }

    public void setAreas(Set<Integer> areas) {
        this.areas = areas;
    }

    @Override
    public String getLabel() {
        StringBuilder label = new StringBuilder();
        String context = getContext();

        if (!ObjectUtils.isBlank(context)) {
            label.append("Context: ");
            label.append(context);

            Set<Integer> areas = getAreas();

            if (!ObjectUtils.isBlank(areas)) {
                label.append(" \u2192 Areas: ");
                label.append(areas);
            }
        }

        return label.toString();
    }
}
