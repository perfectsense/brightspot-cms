package com.psddev.cms.view;

import com.psddev.dari.util.Settings;

/**
 * ViewRenderer annotation processor that associates the
 * {@link JsonView} annotation with a
 * {@link com.psddev.cms.view.JsonViewRenderer}.
 */
public class JsonViewRendererAnnotationProcessor implements ViewRendererAnnotationProcessor<JsonView> {

    @Override
    public ViewRenderer createRenderer(Class<?> viewClass, JsonView annotation) {
        JsonViewRenderer jsonViewRenderer = new JsonViewRenderer();

        jsonViewRenderer.setIndented(!Settings.isProduction());
        jsonViewRenderer.setIncludeClassNames(!Settings.isProduction());

        return jsonViewRenderer;
    }
}
