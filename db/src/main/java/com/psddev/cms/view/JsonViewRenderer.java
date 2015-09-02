package com.psddev.cms.view;

import com.psddev.dari.util.ObjectUtils;

import java.util.Map;

/**
 * Renderer that converts the views to JSON.
 */
public class JsonViewRenderer implements ViewRenderer {

    private boolean includeClassNames;

    private boolean indented;

    /**
     * @return true if class names should be included in the output in the
     * "class" key, false otherwise.
     */
    public boolean isIncludeClassNames() {
        return includeClassNames;
    }

    /**
     * Sets whether class names should be included in the output in the "class"
     * key.
     *
     * @param includeClassNames true if class names should be included.
     */
    public void setIncludeClassNames(boolean includeClassNames) {
        this.includeClassNames = includeClassNames;
    }

    /**
     * @return true if the output should be indented, false otherwise.
     */
    public boolean isIndented() {
        return indented;
    }

    /**
     * Sets whether the output should be indented or not.
     *
     * @param indented true if the output should be indented.
     */
    public void setIndented(boolean indented) {
        this.indented = indented;
    }

    @Override
    public ViewOutput render(Object view) {

        Map<?, ?> viewMap;
        if (view instanceof Map) {
            viewMap = (Map<?, ?>) view;
        } else {
            viewMap = new ViewMap(view, includeClassNames);
        }

        return () -> ObjectUtils.toJson(viewMap, indented);
    }
}
