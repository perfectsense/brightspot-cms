package com.psddev.cms.tool;

import java.io.StringWriter;

import javax.servlet.ServletContext;

import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;

/** Widget controlled by a JSP file. */
public class JspWidget extends Widget {

    public static final String REMOTE_DISPLAY_API = "/_jspWidget.display";
    public static final String REMOTE_UPDATE_API = "/_jspWidget.update";

    private static final String ATTRIBUTE_PREFIX = JspWidget.class.getName() + ".";
    private static final String IS_UPDATING_ATTRIBUTE = ATTRIBUTE_PREFIX + "isUpdating";
    private static final String OBJECT_ATTRIBUTE = ATTRIBUTE_PREFIX + "object";
    private static final String WIDGET_ATTRIBUTE = ATTRIBUTE_PREFIX + "widget";

    private String jsp;

    /**
     * Returns {@code true} if a widget is processing within the given
     * {@code page}.
     */
    public static boolean isUpdating(ToolPageContext page) {
        return Boolean.TRUE.equals(page.getRequest().getAttribute(IS_UPDATING_ATTRIBUTE));
    }

    /** Returns the object associated with the given {@code page}. */
    public static Object getObject(ToolPageContext page) {
        return page.getRequest().getAttribute(OBJECT_ATTRIBUTE);
    }

    /** Returns the widget associated with the given {@code page}. */
    public static JspWidget getWidget(ToolPageContext page) {
        return (JspWidget) page.getRequest().getAttribute(WIDGET_ATTRIBUTE);
    }

    /** Returns the original object associated with the given {@code page}. */
    public static Object getOriginal(ToolPageContext page) {
        Object object = page.getRequest().getAttribute("original");
        return object != null ? object : getObject(page);
    }

    /** Returns the JSP. */
    public String getJsp() {
        return jsp;
    }

    /** Sets the JSP. */
    public void setJsp(String jsp) {
        this.jsp = jsp;
    }

    // --- Widget support ---

    @SuppressWarnings("deprecation")
    private String findApplicationJsp() {
        Tool tool = getTool();
        String appName = tool.getApplicationName();
        String appPath = null;

        if (appName != null) {
            appPath = RoutingFilter.Static.getApplicationPath(appName);

        } else if (ObjectUtils.isBlank(tool.getUrl())) {
            appPath = "";
        }

        return appPath != null
                ? appPath + StringUtils.ensureStart(getJsp(), "/")
                : null;
    }

    private String includeJsp(
            ServletContext context,
            ToolPageContext page,
            Object object,
            String jsp,
            boolean updating)
            throws Exception {
        StringWriter writer = new StringWriter();

        JspUtils.includeEmbedded(
                context,
                page.getRequest(),
                page.getResponse(),
                writer,
                jsp,

                IS_UPDATING_ATTRIBUTE, updating,
                OBJECT_ATTRIBUTE, object,
                WIDGET_ATTRIBUTE, this);

        return writer.toString();
    }

    @Override
    public String display(ToolPageContext page, Object object) throws Exception {
        String jsp = findApplicationJsp();

        if (jsp != null) {
            return includeJsp(null, page, object, jsp, false);

        } else if (ObjectUtils.equals(getTool(), page.getTool())) {
            return includeJsp(page.getServletContext(), page, object, getJsp(), false);

        } else {
            return RemoteWidget.displayWidget(this, REMOTE_DISPLAY_API, page, object);
        }
    }

    @Override
    public void update(ToolPageContext page, Object object) throws Exception {
        String jsp = findApplicationJsp();

        if (jsp != null) {
            includeJsp(null, page, object, jsp, true);

        } else if (ObjectUtils.equals(getTool(), page.getTool())) {
            includeJsp(page.getServletContext(), page, object, getJsp(), true);

        } else {
            RemoteWidget.updateWithWidget(this, REMOTE_UPDATE_API, page, object);
        }
    }
}
