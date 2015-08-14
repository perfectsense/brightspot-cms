package com.psddev.cms.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.TryCatchFinally;

import com.psddev.dari.util.HtmlGrid;
import com.psddev.dari.util.HtmlObject;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeReference;

public class LayoutTag extends BodyTagSupport implements DynamicAttributes, TryCatchFinally {

    private static final long serialVersionUID = 1L;

    private static final String ATTRIBUTE_PREFIX = LayoutTag.class.getName() + ".";
    private static final String GRID_CSS_WRITTEN_ATTRIBUTE = ATTRIBUTE_PREFIX + "gridCssWritten";
    private static final String GRID_CSS_CLASS_WRITTEN_ATTRIBUTE_PREFIX = ATTRIBUTE_PREFIX + "gridCssClassWritten.";
    private static final String GRID_CSS_COMMON_WRITTEN_ATTRIBUTE = ATTRIBUTE_PREFIX + "gridCssCommonWritten";
    private static final String GRID_JAVASCRIPT_WRITTEN_ATTRIBUTE = ATTRIBUTE_PREFIX + "gridJavaScriptWritten";
    private static final String GRIDS_ATTRIBUTE = ATTRIBUTE_PREFIX + "grids";

    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

    private transient HtmlWriter writer;
    private transient List<CssClassHtmlGrid> cssGrids;
    private transient List<String> cssClasses;
    private transient String extraCss;
    private transient Map<String, Object> areas;

    public Map<String, Object> getAreas() {
        return areas;
    }

    // --- DynamicAttributes support ---

    @Override
    public void setDynamicAttribute(String uri, String localName, Object value) {
        if (value != null) {
            attributes.put(localName, value);
        }
    }

    // --- TryCatchFinally support ---

    @Override
    public void doCatch(Throwable error) throws Throwable {
        throw error;
    }

    @Override
    public void doFinally() {
        attributes.clear();

        writer = null;
        cssGrids = null;
        cssClasses = null;
        extraCss = null;
        areas = null;
    }

    // --- TagSupport support ---

    protected HtmlGrid getGrid(HttpServletRequest request, Object area) {
        if (area instanceof Integer) {
            int areaInt = (Integer) area;
            int gridOffset = 0;

            for (CssClassHtmlGrid entry : cssGrids) {
                HtmlGrid grid = entry.grid;
                int gridAreaSize = entry.grid.getAreas().size();

                if (areaInt < gridOffset + gridAreaSize) {
                    return grid;

                } else {
                    gridOffset += gridAreaSize;
                }
            }

        } else if (cssGrids != null && !cssGrids.isEmpty()) {
            return cssGrids.iterator().next().grid;
        }

        return null;
    }

    public Object getAreaName(HttpServletRequest request, Object area) {
        if (area instanceof Integer) {
            int areaInt = (Integer) area;
            int gridOffset = 0;

            for (CssClassHtmlGrid entry : cssGrids) {
                int gridAreaSize = entry.grid.getAreas().size();

                if (areaInt < gridOffset + gridAreaSize) {
                    return areaInt - gridOffset;

                } else {
                    gridOffset += gridAreaSize;
                }
            }
        }

        return area;
    }

    @Override
    public int doStartTag() throws JspException {
        ServletContext context = pageContext.getServletContext();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        try {
            writer = new HtmlWriter(pageContext.getOut());
            cssGrids = new ArrayList<CssClassHtmlGrid>();
            cssClasses = ObjectUtils.to(new TypeReference<List<String>>() { }, attributes.remove("class"));
            Set<String> gridCssClasses = new HashSet<String>();
            extraCss = ObjectUtils.to(String.class, attributes.remove("extraClass"));

            if (cssClasses != null) {
                for (Object cssClassObject : cssClasses) {
                    if (cssClassObject != null) {
                        String cssClassString = cssClassObject.toString();

                        for (String cssClass : cssClassString.split(" ")) {
                            cssClass = cssClass.trim();
                            @SuppressWarnings("unchecked")
                            Map<String, HtmlGrid> grids = (Map<String, HtmlGrid>) request.getAttribute(GRIDS_ATTRIBUTE);

                            if (grids == null) {
                                grids = HtmlGrid.Static.findAll(context, request);
                                request.setAttribute(GRIDS_ATTRIBUTE, grids);
                            }

                            HtmlGrid grid = grids.get("." + cssClass);

                            if (grid != null) {
                                cssGrids.add(new CssClassHtmlGrid(cssClassString, grid));
                                gridCssClasses.add(cssClass);
                            }
                        }
                    }
                }
            }

            if (cssGrids.isEmpty()) {
                areas = null;
                writer.writeStart("div",
                        attributes,
                        "class", cssClasses != null && !cssClasses.isEmpty()
                                ? cssClasses.get(0) + " " + extraCss
                                : extraCss);

            } else {
                areas = new LinkedHashMap<String, Object>();
                LayoutTag.Static.writeGridCssByClasses(writer, context, request, gridCssClasses);
                LayoutTag.Static.writeGridJavaScript(writer, context, request);
            }

        } catch (IOException error) {
            throw new JspException(error);
        }

        return areas != null ? EVAL_BODY_BUFFERED : EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            if (cssGrids.isEmpty()) {
                writer.writeEnd();

            } else {
                List<Object> areasList = new ArrayList<Object>(areas.values());
                int areaSize = areasList.size();
                int gridOffset = 0;

                for (CssClassHtmlGrid cssGrid : cssGrids) {
                    String cssClass = cssGrid.cssClass;
                    HtmlGrid grid = cssGrid.grid;
                    Map<String, GridItem> items = new LinkedHashMap<String, GridItem>();
                    int gridAreaSize = grid.getAreas().size();

                    for (Map.Entry<String, Object> areaEntry : areas.entrySet()) {
                        items.put(areaEntry.getKey(), new GridItem(areaEntry.getValue()));
                    }

                    int i = 0;

                    for (; i < gridAreaSize && gridOffset + i < areaSize; ++ i) {
                        items.put(String.valueOf(i), new GridItem(areasList.get(gridOffset + i)));
                    }

                    for (; i < gridAreaSize; ++ i) {
                        items.put(String.valueOf(i), null);
                    }

                    gridOffset += gridAreaSize;

                    if (extraCss != null) {
                        cssClass += " " + extraCss;
                    }

                    writer.writeStart("div", attributes, "class", cssClass);
                        writer.writeGrid(items, grid);
                    writer.writeEnd();
                }
            }

        } catch (IOException error) {
            throw new JspException(error);
        }

        return EVAL_PAGE;
    }

    /** {@link LayoutTag} utility methods. */
    public static final class Static {

        private Static() {
        }

        /**
         * Writes all grid CSS found in the given {@code context} to the
         * given {@code writer} unless it's already been written within the
         * given {@code request}.
         *
         * @param writer Can't be {@code null}.
         * @param context Can't be {@code null}.
         * @param request Can't be {@code null}.
         */
        public static void writeGridCss(HtmlWriter writer, ServletContext context, HttpServletRequest request) throws IOException {
            if (request.getAttribute(GRID_CSS_WRITTEN_ATTRIBUTE) == null) {
                writer.writeStart("style", "type", "text/css");
                    writer.writeAllGridCss(context, request);
                writer.writeEnd();
                request.setAttribute(GRID_CSS_WRITTEN_ATTRIBUTE, Boolean.TRUE);
            }
        }

        /**
         * Writes all grid CSS related to the given {@code cssClasses found
         * in the given {@code context} to the given {@code writer} unless
         * it's already been written within the given {@code request}.
         *
         * @param writer Can't be {@code null}.
         * @param context Can't be {@code null}.
         * @param request Can't be {@code null}.
         * @param cssClasses May be {@code null}.
         */
        public static void writeGridCssByClasses(
                HtmlWriter writer,
                ServletContext context,
                HttpServletRequest request,
                Collection<String> cssClasses)
                throws IOException {

            if (request.getAttribute(GRID_CSS_WRITTEN_ATTRIBUTE) != null) {
                return;
            }

            boolean styleStarted = false;

            if (request.getAttribute(GRID_CSS_COMMON_WRITTEN_ATTRIBUTE) == null) {
                request.setAttribute(GRID_CSS_COMMON_WRITTEN_ATTRIBUTE, Boolean.TRUE);

                if (!styleStarted) {
                    styleStarted = true;
                    writer.writeStart("style", "type", "text/css");
                }

                writer.writeCommonGridCss();
            }

            if (cssClasses != null) {
                Map<String, HtmlGrid> grids = HtmlGrid.Static.findAll(context, request);

                for (String cssClass : cssClasses) {
                    for (Map.Entry<String, HtmlGrid> entry : grids.entrySet()) {
                        String selector = entry.getKey();

                        if (selector.contains(cssClass)) {
                            String attr = GRID_CSS_CLASS_WRITTEN_ATTRIBUTE_PREFIX + selector;

                            if (request.getAttribute(attr) == null) {
                                request.setAttribute(attr, Boolean.TRUE);

                                if (!styleStarted) {
                                    styleStarted = true;
                                    writer.writeStart("style", "type", "text/css");
                                }

                                writer.writeGridCss(selector, entry.getValue());
                            }
                        }
                    }
                }
            }

            if (styleStarted) {
                writer.writeEnd();
            }
        }

        /**
         * Writes all grid JavaScript found in the given {@code context} to the
         * given {@code writer} unless it's already been written within the
         * given {@code request}.
         *
         * @param writer Can't be {@code null}.
         * @param context Can't be {@code null}.
         * @param request Can't be {@code null}.
         */
        public static void writeGridJavaScript(HtmlWriter writer, ServletContext context, HttpServletRequest request) throws IOException {
            if (request.getAttribute(GRID_JAVASCRIPT_WRITTEN_ATTRIBUTE) == null) {
                writer.writeStart("script", "type", "text/javascript");
                    writer.writeAllGridJavaScript(context, request);
                writer.writeEnd();
                request.setAttribute(GRID_JAVASCRIPT_WRITTEN_ATTRIBUTE, Boolean.TRUE);
            }
        }
    }

    private static class CssClassHtmlGrid {

        public final String cssClass;
        public final HtmlGrid grid;

        public CssClassHtmlGrid(String cssClass, HtmlGrid grid) {
            this.cssClass = cssClass;
            this.grid = grid;
        }
    }

    private class GridItem implements HtmlObject {

        private final Object item;

        public GridItem(Object item) {
            this.item = item;
        }

        @Override
        public void format(HtmlWriter writer) throws IOException {
            if (item != null) {
                writer.write(item.toString());
            }
        }
    }
}
