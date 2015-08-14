package com.psddev.cms.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectFieldComparator;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;

/** Brightspot application, typically used by the internal staff. */
@ToolUi.IconName("object-tool")
public abstract class Tool extends Application {

    public static final String CONTENT_BOTTOM_WIDGET_POSITION = "cms.contentBottom";
    public static final String CONTENT_RIGHT_WIDGET_POSITION = "cms.contentRight";
    public static final String DASHBOARD_WIDGET_POSITION = "cms.dashboard";

    /**
     * Returns this tool's application name.
     *
     * @return May be {@code null}.
     */
    public String getApplicationName() {
        return null;
    }

    /**
     * Returns plugins provided by this tool.
     *
     * @return May be {@code null} if this tool doesn't provide any plugins.
     */
    public List<Plugin> getPlugins() {
        return null;
    }

    /**
     * Writes arbitrary HTML in the header after the styles are defined.
     * Does nothing by default.
     */
    public void writeHeaderAfterStyles(ToolPageContext page) throws IOException {
    }

    /**
     * Writes arbitrary HTML in the header after the scripts are defined.
     * Does nothing by default.
     */
    public void writeHeaderAfterScripts(ToolPageContext page) throws IOException {
    }

    /**
     * Initializes the given {@code search} instance based on the information
     * from the given {@code page}.
     *
     * @param search Can't be {@code null}.
     * @param page Can't be {@code null}.
     */
    public void initializeSearch(Search search, ToolPageContext page) {
    }

    /**
     * Writes additional search filters UI using the given {@code search}
     * and {@code page}.
     *
     * @param search Can't be {@code null}.
     * @param page Can't be {@code null}.
     */
    public void writeSearchFilters(Search search, ToolPageContext page) throws IOException {
    }

    /**
     * Updates the given {@code query} using the information from the given
     * {@code search}.
     *
     * @param search Can't be {@code null}.
     * @param query Can't be {@code null}.
     */
    public void updateSearchQuery(Search search, Query<?> query) {
    }

    /**
     * Returns {@code true} if the given {@code item} should be displayed
     * in the given {@code search} result.
     *
     * @param search Can't be {@code null}.
     * @param item Can't be {@code null}.
     */
    public boolean isDisplaySearchResultItem(Search search, Object item) {
        return true;
    }

    /**
     * Returns the full URL to the given {@code path} with the given
     * {@code parameters}.
     *
     * @param path Can't be {@code null}.
     * @param parameters May be {@code null}.
     * @return Never {@code null}.
     * @throws IllegalArgumentException If the given {@code path} is
     * {@code null}, {@link #getApplicationName} returns {@code null}, or
     * {@link CmsTool#getDefaultToolUrl} returns blank.
     */
    public String fullUrl(String path, Object... parameters) {
        ErrorUtils.errorIfNull(path, "path");

        String appName = getApplicationName();
        String toolUrl = Application.Static.getInstance(CmsTool.class).getDefaultToolUrl();

        ErrorUtils.errorIfNull(appName, "getApplicationName()");
        ErrorUtils.errorIfBlank(toolUrl, "CmsTool#getDefaultToolUrl()");

        return StringUtils.addQueryParameters(
                StringUtils.removeEnd(toolUrl, "/")
                        + RoutingFilter.Static.getApplicationPath(appName)
                        + StringUtils.ensureStart(path, "/"),
                parameters);
    }

    /**
     * Creates an area with the given parameters.
     *
     * @return Never {@code null}.
     */
    protected Area createArea2(String displayName, String internalName, String hierarchy, String url) {
        Area area = new Area();
        area.setDisplayName(displayName);
        area.setInternalName(internalName);
        area.setHierarchy(hierarchy);
        area.setUrl(url);
        return area;
    }

    /**
     * Creates a JSP widget with the given parameters.
     *
     * @return Never {@code null}.
     */
    protected JspWidget createJspWidget(String displayName, String internalName, String jsp, String positionName, double positionColumn, double positionRow) {
        JspWidget widget = new JspWidget();
        widget.setDisplayName(displayName);
        widget.setInternalName(internalName);
        widget.setJsp(jsp);
        widget.addPosition(positionName, positionColumn, positionRow);
        return widget;
    }

    protected PageWidget createPageWidget(String displayName, String internalName, String path, String positionName, double positionColumn, double positionRow) {
        PageWidget widget = new PageWidget();
        widget.setDisplayName(displayName);
        widget.setInternalName(internalName);
        widget.setPath(path);
        widget.addPosition(positionName, positionColumn, positionRow);
        return widget;
    }

    /** {@link Tool} utility methods. */
    public static final class Static {

        private Static() {
        }

        /**
         * Returns all plugins across all tools.
         *
         * @return Never {@code null}. Sorted by {@code displayName}.
         */
        @SuppressWarnings("unchecked")
        public static List<Plugin> getPlugins() {
            List<Plugin> databasePlugins = Query.from(Plugin.class).selectAll();
            List<Plugin> plugins = new ArrayList<Plugin>();

            for (ObjectType type : Database.Static.getDefault().getEnvironment().getTypesByGroup(Tool.class.getName())) {
                if (type.isAbstract() || type.isEmbedded()) {
                    continue;
                }

                Class<?> objectClass = type.getObjectClass();

                if (objectClass == null || !Tool.class.isAssignableFrom(objectClass)) {
                    continue;
                }

                Tool tool = Application.Static.getInstance((Class<? extends Tool>) objectClass);
                List<Plugin> toolPlugins = tool.getPlugins();

                if (toolPlugins != null && !toolPlugins.isEmpty()) {
                    for (Plugin plugin : toolPlugins) {
                        plugin.setTool(tool);
                        plugins.add(plugin);
                    }

                } else {
                    for (Plugin plugin : databasePlugins) {
                        if (tool.equals(plugin.getTool())) {
                            plugins.add(plugin);
                        }
                    }
                }
            }

            CmsTool cms = Application.Static.getInstance(CmsTool.class);
            Set<String> disabled = cms.getDisabledPlugins();

            for (Iterator<Plugin> i = plugins.iterator(); i.hasNext();) {
                Plugin plugin = i.next();

                if (disabled.contains(plugin.getInternalName())) {
                    i.remove();
                }
            }

            Collections.sort(plugins, new ObjectFieldComparator("displayName", true));

            return plugins;
        }

        /**
         * Returns all plugins of the given {@code pluginClass} across all
         * tools.
         *
         * @return Never {@code null}. Sorted by {@code displayName}.
         */
        @SuppressWarnings("unchecked")
        public static <T extends Plugin> List<T> getPluginsByClass(Class<T> pluginClass) {
            List<Plugin> plugins = getPlugins();

            for (Iterator<Plugin> i = plugins.iterator(); i.hasNext();) {
                Plugin plugin = i.next();

                if (!pluginClass.isInstance(plugin)) {
                    i.remove();
                }
            }

            return (List<T>) plugins;
        }

        /** Returns all top-level areas. */
        public static List<Area> getTopAreas() {
            List<Area> topAreas = new ArrayList<Area>();
            Area first = null;
            Area last = null;

            for (Area area : getPluginsByClass(Area.class)) {
                if (area.getHierarchy().contains("/")) {
                    continue;
                }

                if (area.getTool() instanceof CmsTool) {
                    String internalName = area.getInternalName();
                    if ("dashboard".equals(internalName)) {
                        first = area;
                        continue;
                    } else if ("admin".equals(internalName)) {
                        last = area;
                        continue;
                    }
                }

                topAreas.add(area);
            }

            if (first != null) {
                topAreas.add(0, first);
            }

            if (last != null) {
                topAreas.add(last);
            }

            return topAreas;
        }

        /**
         * Returns a table of all widgets with the given
         * {@code positionName}.
         */
        public static List<List<Widget>> getWidgets(String positionName) {
            Map<Double, Map<Double, List<Widget>>> widgets = new HashMap<Double, Map<Double, List<Widget>>>();
            List<List<Widget>> widgetsTable = new ArrayList<List<Widget>>();

            for (Widget widget : getPluginsByClass(Widget.class)) {
                for (Widget.Position position : widget.getPositions()) {
                    if (ObjectUtils.equals(position.getName(), positionName)) {
                        double column = position.getColumn();
                        Map<Double, List<Widget>> widgetsColumn = widgets.get(column);

                        if (widgetsColumn == null) {
                            widgetsColumn = new HashMap<Double, List<Widget>>();
                            widgets.put(column, widgetsColumn);
                        }

                        double row = position.getRow();
                        List<Widget> widgetsRow = widgetsColumn.get(row);

                        if (widgetsRow == null) {
                            widgetsRow = new ArrayList<Widget>();
                            widgetsColumn.put(row, widgetsRow);
                        }

                        widgetsRow.add(widget);
                        break;
                    }
                }
            }

            List<Double> columns = new ArrayList<Double>(widgets.keySet());
            Collections.sort(columns);

            for (Double column : columns) {
                Map<Double, List<Widget>> widgetsColumn = widgets.get(column);
                List<Double> rows = new ArrayList<Double>(widgetsColumn.keySet());
                List<Widget> widgetsTableRow = new ArrayList<Widget>();

                Collections.sort(rows);
                widgetsTable.add(widgetsTableRow);

                for (Double row : rows) {
                    widgetsTableRow.addAll(widgetsColumn.get(row));
                }
            }

            return widgetsTable;
        }
    }

    // --- Deprecated ---

    /** @deprecated Use {@link Static#getPluginsByClass} instead. */
    @Deprecated
    @SuppressWarnings("unchecked")
    public <T extends Plugin> List<T> findPlugins(Class<T> pluginClass) {
        List<T> plugins = new ArrayList<T>();

        for (Plugin plugin : Static.getPlugins()) {
            if (pluginClass.isInstance(plugin) && plugin.getTool() != null) {
                plugins.add((T) plugin);
            }
        }

        return plugins;
    }

    /** @deprecated Use {@link Static#getTopAreas} instead. */
    @Deprecated
    public List<Area> findTopAreas() {
        return Static.getTopAreas();
    }

    /** @deprecated Use {@link Static#getWidgets} instead. */
    @Deprecated
    public List<List<Widget>> findWidgets(String positionName) {
        return Static.getWidgets(positionName);
    }

    // Synchronizes the given {@code plugin} with the existing one in the
    // database if it can be found.
    private void synchronizePlugin(Plugin plugin) {
        State pluginState = plugin.getState();
        Database database = getState().getDatabase();

        pluginState.setDatabase(database);

        UUID typeId = pluginState.getTypeId();
        Tool tool = plugin.getTool();
        String internalName = plugin.getInternalName();

        for (Plugin p : Static.getPlugins()) {
            if (ObjectUtils.equals(typeId, p.getState().getTypeId())
                    && ObjectUtils.equals(tool, p.getTool())
                    && ObjectUtils.equals(internalName, p.getInternalName())) {
                pluginState.setId(p.getId());
                break;
            }
        }
    }

    /** @deprecated Use {@link #getPlugins} instead. */
    @Deprecated
    public void introducePlugin(Plugin plugin) {
        synchronizePlugin(plugin);
        plugin.save();
    }

    /** @deprecated Use {@link #getPlugins} instead. */
    @Deprecated
    public void discontinuePlugin(Plugin plugin) {
        synchronizePlugin(plugin);
        plugin.delete();
    }

    /** @deprecated No replacement. */
    @Deprecated
    public Area createArea(String displayName, String internalName, Area parent, String url) {
        Area area = new Area();
        area.setTool(this);
        area.setDisplayName(displayName);
        area.setInternalName(internalName);
        area.setParent(parent);
        area.setUrl(url);
        synchronizePlugin(area);
        return area;
    }

    /** @deprecated No replacement. */
    @Deprecated
    public <T extends Widget> T createWidget(Class<T> widgetClass, String displayName, String internalName, String iconName) throws Exception {
        T widget = TypeDefinition.getInstance(widgetClass).newInstance();
        widget.setTool(this);
        widget.setDisplayName(displayName);
        widget.setInternalName(internalName);
        widget.setIconName(iconName);
        synchronizePlugin(widget);
        return widget;
    }
}
