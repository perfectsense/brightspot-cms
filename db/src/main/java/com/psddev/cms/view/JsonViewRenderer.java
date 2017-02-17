package com.psddev.cms.view;

import com.psddev.dari.util.ObjectUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renderer that converts the views to JSON.
 */
public class JsonViewRenderer implements ViewRenderer {

    private boolean includeClassNames;

    private boolean indented;

    private boolean disallowMixedOutput;

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

    /**
     * @return true if the view and all its sub-views should be rendered as
     * JSON even if they have a different view renderer,
     */
    public boolean isDisallowMixedOutput() {
        return disallowMixedOutput;
    }

    /**
     * Sets whether the output format should be mixed or not.
     *
     * @param disallowMixedOutput true if mixed output should be disallowed.
     */
    public void setDisallowMixedOutput(boolean disallowMixedOutput) {
        this.disallowMixedOutput = disallowMixedOutput;
    }

    @Deprecated
    @Override
    public ViewOutput render(Object view) {
        return render(view, null);
    }

    @Override
    public ViewOutput render(Object view, ViewTemplateLoader loader) {
        return () -> ObjectUtils.toJson(getRenderableMap(view, loader), indented);
    }

    // Gets the final map that is passed to ObjectUtils#toJson
    private Map<String, Object> getRenderableMap(Object view, ViewTemplateLoader loader) {

        Map<String, Object> viewMap;
        if (view instanceof Map) {
            viewMap = (Map<String, Object>) view;
        } else {
            viewMap = new ViewMap(view, includeClassNames);
        }

        if (disallowMixedOutput) {
            return viewMap;
        }

        Map<String, Object> jsonMap = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : viewMap.entrySet()) {

            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            ViewRenderer renderer = ViewRenderer.createRenderer(value);
            if (renderer != null) {

                if (renderer instanceof JsonViewRenderer) {
                    jsonMap.put(key, ((JsonViewRenderer) renderer).getRenderableMap(value, loader));

                } else {
                    Object renderedValue = null;

                    ViewOutput viewOutput = renderer.render(value, loader);

                    if (viewOutput != null) {
                        String output = viewOutput.get();

                        if (output != null) {
                            try {
                                renderedValue = ObjectUtils.fromJson(output);

                            } catch (RuntimeException e) {
                                renderedValue = output;
                            }
                        }
                    }

                    jsonMap.put(key, renderedValue);
                }

            } else {
                jsonMap.put(key, value);
            }
        }

        return jsonMap;
    }
}
