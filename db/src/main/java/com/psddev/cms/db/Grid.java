package com.psddev.cms.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.psddev.dari.util.HtmlGrid;
import com.psddev.dari.util.HtmlObject;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ObjectUtils;

/**
 * @deprecated No replacement. Create your own.
 */
@Deprecated
@Grid.Embedded
public class Grid extends Content implements Renderer {

    @Required
    private ContentStream contents;

    @Required
    private List<GridLayout> layouts;

    private String defaultContext;
    private List<GridContext> contexts;

    public ContentStream getContents() {
        return contents;
    }

    public void setContents(ContentStream contents) {
        this.contents = contents;
    }

    public List<GridLayout> getLayouts() {
        if (layouts == null) {
            layouts = new ArrayList<GridLayout>();
        }
        return layouts;
    }

    public void setLayouts(List<GridLayout> layouts) {
        this.layouts = layouts;
    }

    public String getDefaultContext() {
        return defaultContext;
    }

    public void setDefaultContext(String defaultContext) {
        this.defaultContext = defaultContext;
    }

    public List<GridContext> getContexts() {
        if (contexts == null) {
            contexts = new ArrayList<GridContext>();
        }
        return contexts;
    }

    public void setContexts(List<GridContext> contexts) {
        this.contexts = contexts;
    }

    @Override
    protected void beforeSave() {
        for (GridLayout l : getLayouts()) {
            new HtmlGrid(l.getTemplate());
        }
    }

    @Override
    public void renderObject(
            HttpServletRequest request,
            HttpServletResponse response,
            HtmlWriter writer)
            throws IOException {

        String cssClass = "_gl-" + getId();
        int maxSize = 0;
        HtmlGrid maxGrid = null;

        writer.writeStart("style", "type", "text/css");
            writer.writeCommonGridCss();

            for (GridLayout l : getLayouts()) {
                HtmlGrid grid = new HtmlGrid(l.getTemplate());
                int size = grid.getAreas().size();
                String prefix = l.getPrefix();

                if (maxSize < size) {
                    maxSize = size;
                    maxGrid = grid;
                }

                writer.writeGridCss(
                        (ObjectUtils.isBlank(prefix) ? "." : prefix + " .") + cssClass,
                        grid);
            }
        writer.writeEnd();

        List<HtmlObject> contentRenderers = new ArrayList<HtmlObject>();
        String defaultContext = getDefaultContext();
        Map<Integer, String> contextsMap = new HashMap<Integer, String>();

        for (GridContext c : getContexts()) {
            String context = c.getContext();

            for (Integer area : c.getAreas()) {
                contextsMap.put(area, context);
            }
        }

        List<?> contents = getContents().findContents(0, maxSize);

        for (int i = 0, size = contents.size(); i < size; ++ i) {
            contentRenderers.add(new ContentRenderer(
                    request,
                    response,
                    contents.get(i),
                    ObjectUtils.firstNonNull(contextsMap.get(i), defaultContext)));
        }

        writer.writeStart("div", "class", cssClass);
            writer.writeGrid(contentRenderers, maxGrid);
        writer.writeEnd();
    }

    private class ContentRenderer implements HtmlObject {

        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final Object content;
        private final String context;

        public ContentRenderer(HttpServletRequest request, HttpServletResponse response, Object content, String context) {
            this.request = request;
            this.response = response;
            this.content = content;
            this.context = context;
        }

        @Override
        public void format(HtmlWriter writer) throws IOException {
            try {
                if (!ObjectUtils.isBlank(context)) {
                    ContextTag.Static.pushContext(request, context);
                }

                try {
                    PageFilter.renderObject(request, response, writer, content);

                } finally {
                    if (!ObjectUtils.isBlank(context)) {
                        ContextTag.Static.popContext(request);
                    }
                }

            } catch (ServletException error) {
                throw new IOException(error);
            }
        }
    }
}
