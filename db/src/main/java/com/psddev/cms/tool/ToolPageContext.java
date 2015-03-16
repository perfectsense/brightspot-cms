package com.psddev.cms.tool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import com.psddev.cms.db.ToolUiLayoutElement;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.Content;
import com.psddev.cms.db.ContentField;
import com.psddev.cms.db.ContentType;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.History;
import com.psddev.cms.db.ImageTag;
import com.psddev.cms.db.LayoutTag;
import com.psddev.cms.db.Page;
import com.psddev.cms.db.Renderer;
import com.psddev.cms.db.ResizeOption;
import com.psddev.cms.db.Schedule;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.StandardImageSize;
import com.psddev.cms.db.Template;
import com.psddev.cms.db.ToolFormWriter;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.Trash;
import com.psddev.cms.db.Variation;
import com.psddev.cms.db.WorkStream;
import com.psddev.cms.db.Workflow;
import com.psddev.cms.db.WorkflowLog;
import com.psddev.cms.db.WorkflowState;
import com.psddev.cms.db.WorkflowTransition;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.CompoundPredicate;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectFieldComparator;
import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.db.ObjectStruct;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Singleton;
import com.psddev.dari.db.State;
import com.psddev.dari.db.StateStatus;
import com.psddev.dari.db.ValidationException;
import com.psddev.dari.util.CodeUtils;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.DebugFilter;
import com.psddev.dari.util.DependencyResolver;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.ImageEditor;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeReference;
import com.psddev.dari.util.Utf8Filter;
import com.psddev.dari.util.WebPageContext;

/**
 * {@link WebPageContext} with extra methods that work well with
 * pages in {@link Tool}.
 */
public class ToolPageContext extends WebPageContext {

    /**
     * Settings key for tool URL prefix when creating a fully qualified
     * version of a path.
     */
    public static final String TOOL_URL_PREFIX_SETTING = "brightspot/toolUrlPrefix";

    public static final String TYPE_ID_PARAMETER = "typeId";
    public static final String OBJECT_ID_PARAMETER = "id";
    public static final String DRAFT_ID_PARAMETER = "draftId";
    public static final String ORIGINAL_DRAFT_VALUE = "original";
    public static final String HISTORY_ID_PARAMETER = "historyId";
    public static final String VARIATION_ID_PARAMETER = "variationId";
    public static final String RETURN_URL_PARAMETER = "returnUrl";

    private static final String ATTRIBUTE_PREFIX = ToolPageContext.class.getName() + ".";
    private static final String ERRORS_ATTRIBUTE = ATTRIBUTE_PREFIX + "errors";
    private static final String FORM_FIELDS_DISABLED_ATTRIBUTE = ATTRIBUTE_PREFIX + "formFieldsDisabled";
    private static final String TOOL_ATTRIBUTE = ATTRIBUTE_PREFIX + "tool";
    private static final String TOOL_BY_CLASS_ATTRIBUTE = ATTRIBUTE_PREFIX + "toolByClass";
    private static final String TOOL_BY_PATH_ATTRIBUTE = ATTRIBUTE_PREFIX + "toolByPath";

    private static final String EXTRA_PREFIX = "cms.tool.";
    private static final String OVERLAID_DRAFT_EXTRA = EXTRA_PREFIX + "overlaidDraft";
    private static final String OVERLAID_HISTORY_EXTRA = EXTRA_PREFIX + "overlaidHistory";

    /** Creates an instance based on the given {@code pageContext}. */
    public ToolPageContext(PageContext pageContext) {
        super(pageContext);
    }

    /** Creates an instance based on the given servlet parameters. */
    public ToolPageContext(
            ServletContext servletContext,
            HttpServletRequest request,
            HttpServletResponse response) {

        super(servletContext, request, response);
    }

    /**
     * Returns the parameter value as an instance of the given
     * {@code returnClass} associated with the given {@code name}, or if not
     * found, either the {@linkplain #getPageSetting page setting value} or
     * the given {@code defaultValue}.
     */
    @SuppressWarnings("unchecked")
    public <T> T pageParam(Class<T> returnClass, String name, T defaultValue) {
        Class<?> valueClass = PRIMITIVE_CLASSES.get(returnClass);

        if (valueClass == null) {
            valueClass = returnClass;
        }

        HttpServletRequest request = getRequest();
        String valueString = request.getParameter(name);
        Object value = ObjectUtils.to(valueClass, valueString);
        Object userValue = ObjectUtils.to(valueClass, AuthenticationFilter.Static.getPageSetting(request, name));

        if (valueString == null) {
            return ObjectUtils.isBlank(userValue) ? defaultValue : (T) userValue;

        } else {
            if (!ObjectUtils.equals(value, userValue)) {
                AuthenticationFilter.Static.putPageSetting(request, name, value);
            }

            return (T) value;
        }
    }

    /**
     * Returns the parameter value as an instance of the given
     * {@code returnClass} associated with the given {@code name}, or if not
     * found, either the {@linkplain #getPageSetting page setting value} or
     * the given {@code defaultValue}.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> pageParams(Class<T> returnClass, String name, List<T> defaultValue) {
        Class<?> valueClass = PRIMITIVE_CLASSES.get(returnClass);

        if (valueClass == null) {
            valueClass = returnClass;
        }

        HttpServletRequest request = getRequest();
        List<T> value = params(returnClass, name);
        List<Object> userValue = ObjectUtils.to(new TypeReference<List<Object>>() { }, AuthenticationFilter.Static.getPageSetting(request, name));

        if (value == null || value.isEmpty()) {
            return ObjectUtils.isBlank(userValue) ? defaultValue : (List<T>) userValue;

        } else {
            if (!ObjectUtils.equals(value, userValue)) {
                AuthenticationFilter.Static.putPageSetting(request, name, value);
            }

            return (List<T>) value;
        }
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_CLASSES; static {
        Map<Class<?>, Class<?>> m = new HashMap<Class<?>, Class<?>>();

        m.put(boolean.class, Boolean.class);
        m.put(byte.class, Byte.class);
        m.put(char.class, Character.class);
        m.put(double.class, Double.class);
        m.put(float.class, Float.class);
        m.put(int.class, Integer.class);
        m.put(long.class, Long.class);
        m.put(short.class, Short.class);

        PRIMITIVE_CLASSES = Collections.unmodifiableMap(m);
    }

    /**
     * Returns a label, or the given {@code defaultLabel} if one can't be
     * found, for the given {@code object}.
     */
    public String getObjectLabelOrDefault(Object object, String defaultLabel) {
        return Static.getObjectLabelOrDefault(object, defaultLabel);
    }

    /** Returns a label for the given {@code object}. */
    public String getObjectLabel(Object object) {
        return Static.getObjectLabel(object);
    }

    /**
     * Returns a label, or the given {@code defaultLabel} if one can't be
     * found, for the type of the given {@code object}.
     */
    public String getTypeLabelOrDefault(Object object, String defaultLabel) {
        return Static.getTypeLabelOrDefault(object, defaultLabel);
    }

    /** Returns a label for the type of the given {@code object}. */
    public String getTypeLabel(Object object) {
        return Static.getTypeLabel(object);
    }

    /**
     * Returns {@code true} is the given {@code object} is previewable.
     *
     * @param object If {@code null}, always returns {@code false}.
     */
    @SuppressWarnings("deprecation")
    public boolean isPreviewable(Object object) {
        if (object != null) {
            if (object instanceof Page &&
                    !(object instanceof Template)) {
                return true;

            } else if (object instanceof Renderer) {
                return true;

            } else {
                State state = State.getInstance(object);
                ObjectType type = state.getType();

                if (type != null) {
                    if (Template.Static.findUsedTypes(getSite()).contains(type)) {
                        return true;

                    } else {
                        Renderer.TypeModification rendererData = type.as(Renderer.TypeModification.class);

                        return !ObjectUtils.isBlank(rendererData.getPath()) ||
                                !ObjectUtils.isBlank(rendererData.getPaths());
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns the singleton instance of the given {@code toolClass}.
     * Note that this method caches the result, so it'll return the
     * exact same object every time within a single request.
     */
    @SuppressWarnings("unchecked")
    public <T extends Tool> T getToolByClass(Class<T> toolClass) {
        HttpServletRequest request = getRequest();
        Map<Class<?>, Tool> tools = (Map<Class<?>, Tool>) request.getAttribute(TOOL_BY_CLASS_ATTRIBUTE);

        if (tools == null) {
            tools = new HashMap<Class<?>, Tool>();
            request.setAttribute(TOOL_BY_CLASS_ATTRIBUTE, tools);
        }

        Tool tool = tools.get(toolClass);

        if (!toolClass.isInstance(tool)) {
            tool = Application.Static.getInstance(toolClass);
            tools.put(toolClass, tool);
        }

        return (T) tool;
    }

    /**
     * Returns the CMS tool.
     *
     * @see #getToolByClass
     */
    public CmsTool getCmsTool() {
        return getToolByClass(CmsTool.class);
    }

    /** Returns all embedded tools, keyed by their context paths. */
    @SuppressWarnings("unchecked")
    public Map<String, Tool> getEmbeddedTools() {
        HttpServletRequest request = getRequest();
        Map<String, Tool> tools = (Map<String, Tool>) request.getAttribute(TOOL_BY_PATH_ATTRIBUTE);

        if (tools == null) {
            tools = new LinkedHashMap<String, Tool>();

            for (Map.Entry<String, Properties> entry : JspUtils.getEmbeddedSettings(getServletContext()).entrySet()) {
                String toolClassName = entry.getValue().getProperty(Application.MAIN_CLASS_SETTING);
                Class<?> objectClass = ObjectUtils.getClassByName(toolClassName);

                if (objectClass != null &&
                        Tool.class.isAssignableFrom(objectClass)) {
                    tools.put(entry.getKey(), getToolByClass((Class<Tool>) objectClass));
                }
            }

            if (!tools.containsKey("")) {
                Application app = Application.Static.getMain();
                if (app instanceof Tool) {
                    tools.put("", (Tool) app);
                }
            }

            request.setAttribute(TOOL_BY_PATH_ATTRIBUTE, tools);
        }

        return tools;
    }

    /** Returns the tool that's currently in use. */
    public Tool getTool() {
        ServletContext context = getServletContext();
        HttpServletRequest request = getRequest();
        Tool tool = (Tool) request.getAttribute(TOOL_ATTRIBUTE);

        if (tool == null) {
            String contextPath = JspUtils.getEmbeddedContextPath(context, request.getServletPath());

            tool = getEmbeddedTools().get(contextPath);
            request.setAttribute(TOOL_ATTRIBUTE, tool);
        }

        return tool;
    }

    private class AreaUrl implements Comparable<AreaUrl> {

        private final Area area;
        private final String url;

        public AreaUrl(Area area, String url) {
            this.area = area;
            this.url = url;
        }

        public Area getArea() {
            return area;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public int compareTo(AreaUrl other) {
            return other.url.length() - url.length();
        }
    }

    /**
     * Returns the area that's currently in use.
     *
     * @return May be {@code null}.
     */
    public Area getArea() {
        List<AreaUrl> areaUrls = new ArrayList<AreaUrl>();

        for (Area area : Tool.Static.getPluginsByClass(Area.class)) {
            String url = area.getUrl();

            if (!ObjectUtils.isBlank(url)) {
                Tool tool = area.getTool();

                if (tool != null) {
                    areaUrls.add(new AreaUrl(area, toolUrl(tool, url)));
                }
            }
        }

        Collections.sort(areaUrls);

        String path = getRequest().getServletPath();

        if (path.endsWith("/index.jsp")) {
            path = path.substring(0, path.length() - 9);
        }

        for (AreaUrl areaUrl : areaUrls) {
            if (path.startsWith(areaUrl.getUrl())) {
                return areaUrl.getArea();
            }
        }

        return null;
    }

    private Object[] pushToArray(Object[] array, Object... newItems) {
        int os = array.length;
        int ns = newItems.length;
        Object[] newArray = new Object[os + ns];

        if (os > 0) {
            System.arraycopy(array, 0, newArray, 0, os);
        }

        if (ns > 0) {
            System.arraycopy(newItems, 0, newArray, os, ns);
        }

        return newArray;
    }

    public String toolPath(Tool tool, String path, Object... parameters) {
        String toolPath = null;
        String appName = tool.getApplicationName();

        if (appName != null) {
            toolPath = RoutingFilter.Static.getApplicationPath(appName);

        } else {
            for (Map.Entry<String, Tool> entry : getEmbeddedTools().entrySet()) {
                if (entry.getValue().equals(tool)) {
                    toolPath = entry.getKey();
                    break;
                }
            }

            if (toolPath == null) {
                throw new IllegalStateException(String.format(
                        "Can't find tool path for [%s]", tool.getName()));
            }
        }

        toolPath = toolPath + StringUtils.ensureStart(path, "/");

        return StringUtils.addQueryParameters(toolPath, parameters);
    }

    public String toolPath(Class<? extends Tool> toolClass, String path, Object... parameters) {
        return toolPath(getToolByClass(toolClass), path, parameters);
    }

    /**
     * Returns an absolute version of the given {@code path} in context
     * of the given {@code tool}, modified by the given {@code parameters}.
     *
     * @param tool Can't be {@code null}.
     * @param path May be {@code null}.
     * @param parameters May be {@code null}.
     */
    @SuppressWarnings("deprecation")
    public String toolUrl(Tool tool, String path, Object... parameters) {
        String url = null;
        String appName = tool.getApplicationName();

        if (appName != null) {
            url = getServletContext().getContextPath() + RoutingFilter.Static.getApplicationPath(appName);

        } else {
            for (Map.Entry<String, Tool> entry : getEmbeddedTools().entrySet()) {
                if (entry.getValue().equals(tool)) {
                    url = entry.getKey();
                    break;
                }
            }

            if (url == null) {
                url = tool.getUrl();

                if (ObjectUtils.isBlank(url)) {
                    url = getServletContext().getContextPath();
                }

            } else {
                url = getServletContext().getContextPath() + url;
            }
        }

        url = url + StringUtils.ensureStart(path, "/");

        return StringUtils.addQueryParameters(url, parameters);
    }

    /**
     * Returns an absolute version of the given {@code path} in context
     * of the instance of the given {@code toolClass}, modified by the given
     * {@code parameters}.
     *
     * @param toolClass Can't be {@code null}.
     * @param path May be {@code null}.
     * @param parameters May be {@code null}.
     */
    public String toolUrl(Class<? extends Tool> toolClass, String path, Object... parameters) {
        return toolUrl(getToolByClass(toolClass), path, parameters);
    }

    /**
     * Returns a fully qualified, absolute version of the given {@code path}
     * in context of the instance of the given {@code toolClass}, modified by
     * the given {@code parameters}.
     *
     * @param toolClass Can't be {@code null}.
     * @param path May be {@code null}.
     * @param parameters May be {@code null}.
     */
    public String fullyQualifiedToolUrl(Class<? extends Tool> toolClass, String path, Object... parameters) {
        String toolUrl = toolUrl(toolClass, path, parameters);
        String prefix = Settings.get(String.class, TOOL_URL_PREFIX_SETTING);

        if (!ObjectUtils.isBlank(prefix)) {
            toolUrl = StringUtils.removeEnd(prefix , "/") + toolUrl;
        }

        return toolUrl;
    }

    /**
     * Returns an absolute version of the given {@code path} in context
     * of the CMS, modified by the given {@code parameters}.
     *
     * @param path May be {@code null}.
     * @param parameters May be {@code null}.
     */
    public String cmsUrl(String path, Object... parameters) {
        return toolUrl(getCmsTool(), path, parameters);
    }

    public String typeUrl(String path, UUID typeId, Object... parameters) {
        return url(path, pushToArray(parameters,
                TYPE_ID_PARAMETER, typeId,
                OBJECT_ID_PARAMETER, null,
                DRAFT_ID_PARAMETER, null,
                HISTORY_ID_PARAMETER, null));
    }

    public String typeUrl(String path, Class<?> objectClass, Object... parameters) {
        UUID typeId = ObjectType.getInstance(objectClass).getId();

        return typeUrl(path, typeId, parameters);
    }

    public String objectUrl(String path, Object object, Object... parameters) {
        if (object instanceof Draft) {
            Draft draft = (Draft) object;

            parameters = pushToArray(parameters,
                    OBJECT_ID_PARAMETER, draft.getObjectId(),
                    DRAFT_ID_PARAMETER, draft.getId(),
                    HISTORY_ID_PARAMETER, null);

        } else if (object instanceof History) {
            History history = (History) object;

            parameters = pushToArray(parameters,
                    OBJECT_ID_PARAMETER, history.getObjectId(),
                    DRAFT_ID_PARAMETER, null,
                    HISTORY_ID_PARAMETER, history.getId());

        } else {
            State state = State.getInstance(object);
            ObjectType type = state.getType();
            UUID objectId = state.getId();
            Draft draft = getOverlaidDraft(object);
            History history = getOverlaidHistory(object);

            parameters = pushToArray(parameters,
                    OBJECT_ID_PARAMETER, objectId,
                    TYPE_ID_PARAMETER, type != null ? type.getId() : null,
                    DRAFT_ID_PARAMETER, draft != null ? draft.getId() : null,
                    HISTORY_ID_PARAMETER, history != null ? history.getId() : null);
        }

        return url(path, parameters);
    }

    public String originalUrl(String path, Object object, Object... parameters) {
        return url(path, pushToArray(parameters,
                OBJECT_ID_PARAMETER, State.getInstance(object).getId(),
                DRAFT_ID_PARAMETER, ORIGINAL_DRAFT_VALUE,
                HISTORY_ID_PARAMETER, null));
    }

    /**
     * Returns an URL for returning to the current page from the request
     * at the given {@code path}, modified by the given {@code parameters}.
     */
    public String returnableUrl(String path, Object... parameters) {
        HttpServletRequest request = getRequest();

        return url(path, pushToArray(parameters,
                RETURN_URL_PARAMETER, JspUtils.getAbsolutePath(request, "")
                .substring(JspUtils.getEmbeddedContextPath(getServletContext(), request.getServletPath()).length())));
    }

    /**
     * Returns an URL to the return to the page specified by a previous
     * call to {@link #returnableUrl(String, Object...)}, modified by the
     * given {@code parameters}.
     */
    public String returnUrl(Object... parameters) {
        String returnUrl = param(String.class, RETURN_URL_PARAMETER);

        if (ObjectUtils.isBlank(returnUrl)) {
            throw new IllegalArgumentException(String.format(
                    "The [%s] parameter is required!", RETURN_URL_PARAMETER));
        }

        return url(returnUrl, parameters);
    }

    /** Returns a modifiable list of all the errors in this page. */
    public List<Throwable> getErrors() {
        @SuppressWarnings("unchecked")
        List<Throwable> errors = (List<Throwable>) getRequest().getAttribute(ERRORS_ATTRIBUTE);

        if (errors == null) {
            errors = new ArrayList<Throwable>();
            getRequest().setAttribute(ERRORS_ATTRIBUTE, errors);
        }

        return errors;
    }

    /**
     * Renders the form inputs appropriate for the given {@code field}
     * using the data from the given {@code object}.
     */
    public void renderField(Object object, ObjectField field) throws IOException {
        @SuppressWarnings("all")
        ToolFormWriter writer = new ToolFormWriter(this);

        writer.inputs(State.getInstance(object), field.getInternalName());
    }

    /**
     * Processes the form inputs for the given {@code field}, rendered in
     * {@link #renderField(Object, ObjectField)}, using the data from the
     * given {@code object}.
     */
    public void processField(Object object, ObjectField field) throws Throwable {
        @SuppressWarnings("all")
        ToolFormWriter writer = new ToolFormWriter(this);

        writer.update(State.getInstance(object), getRequest(), field.getInternalName());
    }

    /** Finds an existing object or reserve one. */
    public Object findOrReserve(Collection<ObjectType> validTypes) {
        UUID objectId = param(UUID.class, OBJECT_ID_PARAMETER);
        Object object;
        WorkStream workStream = Query.findById(WorkStream.class, param(UUID.class, "workStreamId"));

        UUID draftId = param(UUID.class, DRAFT_ID_PARAMETER);
        if (!isFormPost() && workStream != null) {
            object = workStream.next(getUser());
            if (object instanceof Draft) {
                objectId = ((Draft) object).getObjectId();
                draftId = ((Draft) object).getId();
                object = Query.fromAll().where("_id = ?", objectId).resolveInvisible().first();
            }

        } else {
            object = Query.fromAll().where("_id = ?", objectId).resolveInvisible().first();
        }

        if (object != null) {
            if (workStream == null) {
                ObjectType objectType = State.getInstance(object).getType();

                if (!ObjectUtils.isBlank(validTypes) &&
                        !validTypes.contains(objectType)) {
                    StringBuilder tb = new StringBuilder();

                    for (ObjectType type : validTypes) {
                        tb.append(type.getLabel());
                        tb.append(", ");
                    }

                    tb.setLength(tb.length() - 2);

                    throw new IllegalArgumentException(String.format(
                            "Expected one of [%s] types for [%s] object but it is of [%s] type",
                            tb,
                            objectId,
                            objectType != null ? objectType.getLabel() : "unknown"));
                }
            }

        } else if (!ObjectUtils.isBlank(validTypes)) {
            ObjectType selectedType = ObjectType.getInstance(param(UUID.class, TYPE_ID_PARAMETER));

            if (selectedType == null) {
                for (ObjectType type : validTypes) {
                    selectedType = type;
                    break;
                }
            }

            if (selectedType != null) {
                if (selectedType.getSourceDatabase() != null) {
                    object = Query.fromType(selectedType).where("_id = ?", objectId).resolveInvisible().first();
                }

                if (object == null) {
                    if (selectedType.getGroups().contains(Singleton.class.getName())) {
                        object = Query.fromType(selectedType).resolveInvisible().first();
                    }

                    if (object == null) {
                        object = selectedType.createObject(objectId);
                        State.getInstance(object).as(Site.ObjectModification.class).setOwner(getSite());
                    }
                }
            }
        }

        if (object == null) {
            Object draftObject = Query.fromAll().where("_id = ?", draftId).first();

            if (draftObject instanceof Draft) {
                Draft draft = (Draft) draftObject;
                object = draft.getObject();

                State.getInstance(object).getExtras().put(OVERLAID_DRAFT_EXTRA, draft);
            }

        } else {
            State state = State.getInstance(object);

            History history = Query.
                    from(History.class).
                    where("id = ?", param(UUID.class, HISTORY_ID_PARAMETER)).
                    and("objectId = ?", objectId).
                    first();

            if (history != null) {
                state.getExtras().put(OVERLAID_HISTORY_EXTRA, history);
                state.setValues(history.getObjectOriginals());
                state.setStatus(StateStatus.SAVED);

            } else if (objectId != null) {
                Object draftObject = Query.
                        fromAll().
                        where("id = ?", draftId).
                        and("com.psddev.cms.db.Draft/objectId = ?", objectId).
                        first();

                if (draftObject instanceof Draft) {
                    Draft draft = (Draft) draftObject;

                    state.getExtras().put(OVERLAID_DRAFT_EXTRA, draft);
                    state.getValues().putAll(draft.getObjectChanges());
                }
            }

            UUID variationId = param(UUID.class, VARIATION_ID_PARAMETER);

            if (variationId != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> variationValues = (Map<String, Object>) state.getByPath("variations/" + variationId.toString());

                if (variationValues != null) {
                    state.putAll(variationValues);
                }
            }
        }

        if (object != null) {
            State.getInstance(object).setResolveInvisible(true);
        }

        Template template = Query.from(Template.class).where("_id = ?", param(UUID.class, "templateId")).first();

        if (template != null) {
            if (object == null) {
                Set<ObjectType> contentTypes = template.getContentTypes();

                if (!contentTypes.isEmpty()) {
                    object = contentTypes.iterator().next().createObject(objectId);
                    State.getInstance(object).as(Site.ObjectModification.class).setOwner(getSite());
                }
            }

            if (object != null) {
                State.getInstance(object).as(Template.ObjectModification.class).setDefault(template);
            }

        } else if (object != null) {
            State state = State.getInstance(object);

            if (state.isNew()) {
                List<Template> templates = Template.Static.findUsable(object);

                if (!templates.isEmpty()) {
                    state.as(Template.ObjectModification.class).setDefault(templates.iterator().next());
                }
            }
        }

        if (object != null) {
            State state = State.getInstance(object);
            Content.ObjectModification contentData = state.as(Content.ObjectModification.class);

            if (contentData.isDraft()) {
                Draft draft = Query.from(Draft.class).where("objectId = ?", state.getId()).first();

                if (draft != null) {
                    state.getExtras().put(OVERLAID_DRAFT_EXTRA, draft);
                }
            }
        }

        return object;
    }

    /** Finds an existing object or reserve one. */
    public Object findOrReserve(UUID... validTypeIds) {
        Set<ObjectType> validTypes = null;

        if (!ObjectUtils.isBlank(validTypeIds)) {
            validTypes = new LinkedHashSet<ObjectType>();

            for (UUID typeId : validTypeIds) {
                ObjectType type = ObjectType.getInstance(typeId);

                if (type != null) {
                    validTypes.add(type);
                }
            }
        }

        return findOrReserve(validTypes);
    }

    /** Finds an existing object or reserve one. */
    public Object findOrReserve(Class<?>... validObjectClasses) {
        Set<ObjectType> validTypes = null;

        if (!ObjectUtils.isBlank(validObjectClasses)) {
            validTypes = new LinkedHashSet<ObjectType>();

            for (Class<?> validObjectClass : validObjectClasses) {
                ObjectType type = ObjectType.getInstance(validObjectClass);

                if (type != null) {
                    validTypes.add(type);
                }
            }
        }

        return findOrReserve(validTypes);
    }

    /** Finds an existing object or reserve one. */
    public Object findOrReserve() {
        UUID selectedTypeId = param(UUID.class, TYPE_ID_PARAMETER);

        return findOrReserve(selectedTypeId != null ?
                new UUID[] { selectedTypeId } :
                new UUID[0]);
    }

    /**
     * Returns the draft that was overlaid on top of the given
     * {@code object}.
     */
    public Draft getOverlaidDraft(Object object) {
        return (Draft) State.getInstance(object).getExtra(OVERLAID_DRAFT_EXTRA);
    }

    /**
     * Returns the past revision that was overlaid on top of the
     * {@code object}.
     */
    public History getOverlaidHistory(Object object) {
        return (History) State.getInstance(object).getExtra(OVERLAID_HISTORY_EXTRA);
    }

    public Predicate siteItemsPredicate() {
        ToolUser user = getUser();

        if (user != null) {
            Site site = user.getCurrentSite();

            if (site != null) {
                return site.itemsPredicate();
            }
        }

        return null;
    }

    public Predicate siteItemsSearchPredicate() {
        Predicate predicate = siteItemsPredicate();

        if (predicate != null) {
            predicate = CompoundPredicate.combine(
                    PredicateParser.AND_OPERATOR,
                    predicate,
                    PredicateParser.Static.parse("* matches *"));
        }

        return predicate;
    }

    private String cmsResource(String path, Object... parameters) {
        ServletContext context = getServletContext();
        path = cmsUrl(path);
        long lastModified = 0;

        try {
            URL resource = context.getResource(path);

            if (resource != null) {
                URLConnection resourceConnection = resource.openConnection();
                InputStream resourceInput = resourceConnection.getInputStream();

                try {
                    lastModified = resourceConnection.getLastModified();
                } finally {
                    resourceInput.close();
                }
            }

        } catch (IOException error) {
            throw new IllegalStateException(error);
        }

        if (lastModified == 0) {
            lastModified = (long) (Math.random() * Long.MAX_VALUE);
        }

        return StringUtils.addQueryParameters(
                StringUtils.addQueryParameters(path, parameters),
                "_", lastModified);
    }

    /**
     * Returns the URL to the preview thumbnail of the given {@code object}.
     *
     * @return May be {@code null}.
     */
    public String getPreviewThumbnailUrl(Object object) {
        if (object != null) {
            StorageItem preview = State.getInstance(object).getPreview();

            if (preview != null) {
                if (ImageEditor.Static.getDefault() != null) {
                    return new ImageTag.Builder(preview).
                            setHeight(300).
                            setResizeOption(ResizeOption.ONLY_SHRINK_LARGER).
                            toUrl();

                } else {
                    return preview.getPublicUrl();
                }
            }
        }

        return null;
    }

    /**
     * Writes a descriptive label HTML for the given {@code object}.
     *
     * @param object If {@code null}, writes {@code N/A}.
     */
    public void writeObjectLabel(Object object) throws IOException {
        if (object == null) {
            writeHtml("N/A");

        } else {
            State state = State.getInstance(object);
            String visibilityLabel = object instanceof Draft ? "Update" : state.getVisibilityLabel();
            String label = state.getLabel();

            if (!ObjectUtils.isBlank(visibilityLabel)) {
                writeStart("span", "class", "visibilityLabel");
                    writeHtml(visibilityLabel);
                writeEnd();

                writeHtml(" ");
            }

            writeHtml(ObjectUtils.isBlank(label) ?
                    state.getId() :
                    state.getLabel());
        }
    }

    /**
     * Writes a descriptive label HTML for the type of the given
     * {@code object}.
     *
     * @param object If it or its type is {@code null}, writes {@code N/A}.
     */
    public void writeTypeLabel(Object object) throws IOException {
        ObjectType type = null;

        if (object != null) {
            if (object instanceof Draft) {
                type = ((Draft) object).getObjectType();

            } else {
                type = State.getInstance(object).getType();
            }
        }

        writeObjectLabel(type);
    }

    /**
     * Writes a descriptive label HTML that contains the type information for
     * the given {@code object}.
     *
     * @param object If {@code null}, writes {@code N/A}.
     */
    public void writeTypeObjectLabel(Object object) throws IOException {
        if (object == null) {
            writeHtml("N/A");

        } else {
            State state = State.getInstance(object);
            ObjectType type = state.getType();
            String visibilityLabel = state.getVisibilityLabel();
            String label = state.getLabel();

            if (!ObjectUtils.isBlank(visibilityLabel)) {
                writeStart("span", "class", "visibilityLabel");
                    writeHtml(visibilityLabel);
                writeEnd();

                writeHtml(" ");
            }

            String typeLabel;

            if (type == null) {
                typeLabel = "Unknown Type";

            } else {
                typeLabel = type.getLabel();

                if (ObjectUtils.isBlank(typeLabel)) {
                    typeLabel = type.getId().toString();
                }
            }

            if (ObjectUtils.isBlank(label)) {
                label = state.getId().toString();
            }

            writeHtml(typeLabel);

            if (!typeLabel.equals(label)) {
                writeHtml(": ");
                writeHtml(label);
            }
        }
    }

    /**
     * Returns the user's time zone.
     *
     * @return Never {@code null}.
     */
    public DateTimeZone getUserDateTimeZone() {
        DateTimeZone timeZone = null;
        ToolUser user = getUser();

        if (user != null) {
            String timeZoneId = user.getTimeZone();

            if (!ObjectUtils.isBlank(timeZoneId)) {
                try {
                    timeZone = DateTimeZone.forID(timeZoneId);
                } catch (IllegalArgumentException error) {
                    // Ignore unparseable time zone IDs.
                }
            }
        }

        return timeZone == null ?
                DateTimeZone.getDefault() :
                timeZone;
    }

    /**
     * Converts the given {@code dateTime} to the user's time zone.
     *
     * @param dateTime If {@code null}, returns {@code null}.
     * @return May be {@code null}.
     */
    public DateTime toUserDateTime(Object dateTime) {
        return dateTime != null ?
                new DateTime(dateTime, getUserDateTimeZone()) :
                null;
    }

    /**
     * Formats the given {@code dateTime} according to the given
     * {@code format}.
     *
     * @param dateTime If {@code null}, returns {@code N/A}.
     * @return Never {@code null}.
     */
    public String formatUserDateTimeWith(Object dateTime, String format) throws IOException {
        return dateTime != null ?
                toUserDateTime(dateTime).toString(format) :
                "N/A";
    }

    /**
     * Formats the given {@code dateTime} according to the default format.
     *
     * @param dateTime If {@code null}, returns {@code N/A}.
     * @return Never {@code null}.
     */
    public String formatUserDateTime(Object dateTime) throws IOException {
        return formatUserDateTimeWith(
                dateTime,
                new DateTime(dateTime).getYear() == new DateTime().getYear() ?
                    "EEE MMM dd hh:mm aa" :
                    "EEE MMM dd yyyy hh:mm aa");
    }

    /**
     * Formats the date part of the given {@code dateTime} according to the
     * default format.
     *
     * @param dateTime If {@code null}, returns {@code N/A}.
     * @return Never {@code null}.
     */
    public String formatUserDate(Object dateTime) throws IOException {
        return formatUserDateTimeWith(
                dateTime,
                new DateTime(dateTime).getYear() == new DateTime().getYear() ?
                    "EEE MMM dd" :
                    "EEE MMM dd yyyy");
    }

    /**
     * Formats the time part of the given {@code dateTime} according to the
     * default format.
     *
     * @param dateTime If {@code null}, returns {@code N/A}.
     * @return Never {@code null}.
     */
    public String formatUserTime(Object dateTime) throws IOException {
        return formatUserDateTimeWith(dateTime, "hh:mm aa");
    }

    /**
     * Writes the tool header with the given {@code title}.
     *
     * @param title If {@code null}, uses the default title.
     * @param requireToolUser If {@code true}, calls {@link #requireUser}.
     */
    public void writeHeader(String title, boolean requireToolUser) throws IOException {
        if (requireToolUser && requireUser()) {
            throw new IllegalStateException();
        }

        if (isAjaxRequest() || param(boolean.class, "_frame")) {
            return;
        }

        CmsTool cms = getCmsTool();
        Area area = getArea();
        String companyName = cms.getCompanyName();
        String environment = cms.getEnvironment();
        ToolUser user = getUser();

        if (ObjectUtils.isBlank(companyName)) {
            companyName = "Brightspot";
        }

        Site site = getSite();
        StorageItem companyLogo = site != null ? site.getCmsLogo() : null;

        if (companyLogo == null) {
            companyLogo = cms.getCompanyLogo();
        }

        writeTag("!doctype html");
        writeTag("html", "class", site != null ? site.getCmsCssClass() : null);
            writeStart("head");
                writeStart("title");
                    if (!ObjectUtils.isBlank(title)) {
                        writeHtml(title);
                        writeHtml(" | ");

                    } else if (area != null) {
                        writeObjectLabel(area);
                        writeHtml(" | ");
                    }

                    writeHtml("CMS | ");
                    writeHtml(companyName);
                writeEnd();

                writeElement("meta", "name", "robots", "content", "noindex");
                writeElement("meta", "name", "viewport", "content", "width=device-width, initial-scale=1");
                writeStylesAndScripts();
            writeEnd();

            Schedule currentSchedule = getUser() != null ? getUser().getCurrentSchedule() : null;
            String broadcastMessage = cms.getBroadcastMessage();
            Date broadcastExpiration = cms.getBroadcastExpiration();
            boolean hasBroadcast = !ObjectUtils.isBlank(broadcastMessage) &&
                    (broadcastExpiration == null ||
                    broadcastExpiration.after(new Date()));

            writeTag("body", "class",
                    (currentSchedule != null || hasBroadcast ? "hasToolBroadcast " : "") +
                    (user != null ? "" : "noToolUser "));
                if (currentSchedule != null || hasBroadcast) {
                    writeStart("div", "class", "toolBroadcast");
                        if (currentSchedule != null) {
                            writeHtml("All editorial changes will be scheduled for: ");

                            writeStart("a",
                                    "href", cmsUrl("/scheduleEdit", "id", currentSchedule.getId()),
                                    "target", "scheduleEdit");
                                writeHtml(getObjectLabel(currentSchedule));
                            writeEnd();

                            writeHtml(" - ");

                            writeStart("form",
                                    "method", "post",
                                    "style", "display: inline;",
                                    "action", cmsUrl("/misc/updateUserSettings",
                                            "action", "scheduleSet",
                                            "returnUrl", url("")));
                                writeStart("button",
                                        "class", "link icon icon-action-cancel");
                                    writeHtml("Stop Scheduling");
                                writeEnd();
                            writeEnd();
                        }

                        if (hasBroadcast) {
                            writeHtml(" - ");
                            writeHtml(broadcastMessage);
                        }
                    writeEnd();
                }

                writeStart("div", "class", "toolHeader" + (!ObjectUtils.isBlank(environment) ? " toolHeader-hasEnvironment" : ""));

                    writeStart("h1", "class", "toolTitle");
                        writeStart("a", "href", cmsUrl("/"));
                            if (companyLogo != null) {
                                writeElement("img",
                                        "alt", companyName,
                                        "src", JspUtils.isSecure(getRequest()) ?
                                                companyLogo.getSecurePublicUrl() :
                                                companyLogo.getPublicUrl());

                            } else {
                                writeHtml(companyName);
                            }
                        writeEnd();
                    writeEnd();

                    if (!ObjectUtils.isBlank(environment)) {
                        writeStart("div", "class", "toolEnv");
                            writeHtml(environment);
                        writeEnd();
                    }

                    if (user != null) {
                        StorageItem avatar = user.getAvatar();
                        String name = user.getName().trim();
                        String[] nameParts = name.split("\\s+");
                        String firstName = nameParts[0];

                        writeStart("div", "class", "toolUserDisplay");
                            writeStart("div", "class", "toolUserWelcome");
                                writeHtml("Welcome, ");

                                if (firstName.equals(firstName.toLowerCase(Locale.ENGLISH))) {
                                    writeHtml(firstName.substring(0, 1).toUpperCase(Locale.ENGLISH));
                                    writeHtml(firstName.substring(1));

                                } else {
                                    writeHtml(firstName);
                                }
                            writeEnd();

                            writeStart("div", "class", "toolUserControls");
                                writeStart("ul", "class", "piped");
                                    writeStart("li");
                                        writeStart("a",
                                                "href", cmsUrl("/toolUserDashboard"),
                                                "target", "toolUserDashboard");
                                            writeHtml("Profile");
                                        writeEnd();
                                    writeEnd();

                                    writeStart("li");
                                        writeStart("a",
                                                "href", cmsUrl("/misc/logOut.jsp"));
                                            writeHtml("Log Out");
                                        writeEnd();
                                    writeEnd();
                                writeEnd();
                            writeEnd();

                            writeStart("span", "class", "toolUserAvatar");
                                writeStart("a",
                                        "href", cmsUrl("/toolUserDashboard"),
                                        "target", "toolUserDashboard");
                                    if (avatar != null) {
                                        writeElement("img",
                                                "src", ImageEditor.Static.resize(ImageEditor.Static.getDefault(), avatar, null, 100, 100).getPublicUrl());
                                    }
                                writeEnd();
                            writeEnd();
                        writeEnd();

                        int nowHour = new DateTime().getHourOfDay();

                        writeStart("div", "class", "toolProfile");
                            writeHtml("Good ");
                            writeHtml(nowHour >= 2 && nowHour < 12 ? "Morning" : (nowHour >= 12 && nowHour < 18 ? "Afternoon" : "Evening"));
                            writeHtml(", ");
                            writeHtml(getObjectLabel(user));

                            writeStart("ul");
                                if (!Site.Static.findAll().isEmpty()) {
                                    Site currentSite = user.getCurrentSite();

                                    writeStart("li");
                                        writeHtml("Site: ");
                                        writeStart("a", "href", cmsUrl("/misc/sites.jsp"), "target", "misc");
                                            writeHtml(currentSite != null ? currentSite.getLabel() : "Global");
                                        writeEnd();
                                    writeEnd();
                                }

                                writeStart("li");
                                    writeStart("a",
                                            "class", "icon icon-object-history",
                                            "href", cmsUrl("/toolUserHistory"),
                                            "target", "toolUserHistory");
                                        writeHtml("History");
                                    writeEnd();
                                writeEnd();

                                writeStart("li");
                                    writeStart("a",
                                            "class", "icon icon-object-toolUser",
                                            "href", cmsUrl("/misc/settings.jsp"),
                                            "target", "misc");
                                        writeHtml("Profile");
                                    writeEnd();
                                writeEnd();

                                writeStart("li");
                                    writeStart("a",
                                            "class", "action-logOut",
                                            "href", cmsUrl("/misc/logOut.jsp"));
                                        writeHtml("Log Out");
                                    writeEnd();
                                writeEnd();
                            writeEnd();
                        writeEnd();
                    }

                    if (hasPermission("area/dashboard")) {
                        writeStart("form",
                                "class", "toolSearch",
                                "method", "get",
                                "action", cmsUrl("/misc/search.jsp"),
                                "target", "miscSearch");

                            writeElement("input", "type", "hidden", "name", Utf8Filter.CHECK_PARAMETER, "value", Utf8Filter.CHECK_VALUE);
                            writeElement("input", "type", "hidden", "name", Search.NAME_PARAMETER, "value", "global");

                            writeStart("span", "class", "searchInput");
                                writeStart("label", "for", createId()).writeHtml("Search").writeEnd();
                                writeElement("input", "type", "text", "id", getId(), "name", "q");
                                writeStart("button").writeHtml("Go").writeEnd();
                            writeEnd();

                        writeEnd();
                    }

                    if (user != null) {
                        String servletPath = JspUtils.getEmbeddedServletPath(getServletContext(), getRequest().getServletPath());

                        writeStart("ul", "class", "toolNav");
                            for (Area top : Tool.Static.getTopAreas()) {
                                if (!hasPermission(top.getPermissionId())) {
                                    continue;
                                }

                                String topUrl = top.getUrl();
                                String topLabel = getObjectLabel(top);

                                writeStart("li",
                                        "class", (top.hasChildren() ? " isNested" : "") + (area != null && area.getHierarchy().startsWith(top.getHierarchy()) ? " selected" : ""));
                                    writeStart("a", "href", topUrl == null ? "#" : toolUrl(top.getTool(), topUrl));
                                        writeHtml(topLabel);
                                    writeEnd();

                                    if (top.hasChildren()) {
                                        writeStart("ul");
                                            for (Area child : top.getChildren()) {
                                                if (!hasPermission(child.getPermissionId())) {
                                                    continue;
                                                }

                                                writeStart("li", "class", area != null && area.getInternalName().equals(child.getInternalName()) ? "selected" : null);
                                                    writeStart("a", "href", toolUrl(child.getTool(), child.getUrl()));
                                                        writeHtml(getObjectLabel(child));
                                                    writeEnd();
                                                writeEnd();
                                            }
                                        writeEnd();
                                    }
                                writeEnd();
                            }
                        writeEnd();
                    }

                writeEnd();

                writeTag("div", "class", "toolContent");

                    StorageItem backgroundImage = cms.getBackgroundImage();

                    if (backgroundImage != null) {
                        writeStart("div",
                                "class", "toolBackground",
                                "style", cssString(
                                        "background-image", "url(" + backgroundImage.getPublicUrl() + ")"));
                        writeEnd();
                    }
    }

    public void writeStylesAndScripts() throws IOException {
        List<Tool> tools = new ArrayList<Tool>();

        for (ObjectType type : Database.Static.getDefault().getEnvironment().getTypesByGroup(Tool.class.getName())) {
            if (!type.isConcrete()) {
                continue;
            }

            try {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                Class<? extends Tool> toolClass = (Class) type.getObjectClass();

                if (toolClass != null) {
                    tools.add(Application.Static.getInstance(toolClass));
                }

            } catch (ClassCastException error) {
                // Ignore tool instances without backing Java classes.
            }
        }

        CmsTool cms = getCmsTool();
        String extraCss = cms.getExtraCss();
        String extraJavaScript = cms.getExtraJavaScript();
        String theme = "v3";

        if (getCmsTool().isUseNonMinifiedCss()) {
            writeElement("link", "rel", "stylesheet/less", "type", "text/less", "href", cmsResource("/style/" + theme + ".less"));

        } else {
            writeElement("link", "rel", "stylesheet", "type", "text/css", "href", cmsResource("/style/" + theme + ".min.css"));
        }

        for (Tool tool : tools) {
            tool.writeHeaderAfterStyles(this);
        }

        String scriptPrefix = getCmsTool().isUseNonMinifiedJavaScript() ? "/script/" : "/script.min/";

        if (getCmsTool().isUseNonMinifiedCss()) {
            writeStart("script", "type", "text/javascript", "src", cmsResource(scriptPrefix + "less-dev.js"));
            writeEnd();

            writeStart("script", "type", "text/javascript");
                writeHtml("window.less.relativeUrls = true;");
            writeEnd();

            writeStart("script", "type", "text/javascript", "src", cmsResource(scriptPrefix + "husl.js"));
            writeEnd();

            writeStart("script", "type", "text/javascript", "src", cmsResource(scriptPrefix + "husl-less.js"));
            writeEnd();

            writeStart("script", "type", "text/javascript", "src", cmsResource(scriptPrefix + "less.js"));
            writeEnd();
        }

        if (!ObjectUtils.isBlank(extraCss)) {
            writeStart("style", "type", "text/css");
                write(extraCss);
            writeEnd();
        }

        List<Map<String, Object>> cssClassGroups  = new ArrayList<Map<String, Object>>();

        for (CmsTool.CssClassGroup group : cms.getTextCssClassGroups()) {
            Map<String, Object> groupDef = new HashMap<String, Object>();
            cssClassGroups.add(groupDef);

            groupDef.put("internalName", group.getInternalName());
            groupDef.put("displayName", group.getDisplayName());
            groupDef.put("dropDown", group.isDropDown());

            List<Map<String, String>> cssClasses = new ArrayList<Map<String, String>>();
            groupDef.put("cssClasses", cssClasses);

            for (CmsTool.CssClass cssClass : group.getCssClasses()) {
                Map<String, String> cssDef = new HashMap<String, String>();
                cssClasses.add(cssDef);

                cssDef.put("internalName", cssClass.getInternalName());
                cssDef.put("displayName", cssClass.getDisplayName());
                cssDef.put("tag", cssClass.getTag());
            }
        }

        List<Map<String, String>> standardImageSizes = new ArrayList<Map<String, String>>();

        for (StandardImageSize size : StandardImageSize.findAll()) {
            Map<String, String> sizeMap = new CompactMap<String, String>();

            sizeMap.put("internalName", size.getInternalName());
            sizeMap.put("displayName", size.getDisplayName());
            standardImageSizes.add(sizeMap);
        }

        List<Map<String, Object>> commonTimes = new ArrayList<Map<String, Object>>();

        for (CmsTool.CommonTime commonTime : getCmsTool().getCommonTimes()) {
            Map<String, Object> commonTimeMap = new CompactMap<String, Object>();

            commonTimeMap.put("displayName", commonTime.getDisplayName());
            commonTimeMap.put("hour", commonTime.getHour());
            commonTimeMap.put("minute", commonTime.getMinute());
            commonTimes.add(commonTimeMap);
        }

        writeStart("script", "type", "text/javascript");
            write("var CONTEXT_PATH = '", cmsUrl("/"), "';");
            write("var CSS_CLASS_GROUPS = ", ObjectUtils.toJson(cssClassGroups), ";");
            write("var STANDARD_IMAGE_SIZES = ", ObjectUtils.toJson(standardImageSizes), ";");
            write("var RTE_LEGACY_HTML = ", getCmsTool().isLegacyHtml(), ';');
            write("var RTE_ENABLE_ANNOTATIONS = ", getCmsTool().isEnableAnnotations(), ';');
            write("var DISABLE_TOOL_CHECKS = ", getCmsTool().isDisableToolChecks(), ';');
            write("var COMMON_TIMES = ", ObjectUtils.toJson(commonTimes));
        writeEnd();

        writeStart("script", "type", "text/javascript", "src", "//www.google.com/jsapi");
        writeEnd();

        writeStart("script", "type", "text/javascript", "src", cmsResource(scriptPrefix + "jquery.js"));
        writeEnd();

        writeStart("script", "type", "text/javascript", "src", cmsResource(scriptPrefix + "jquery.extra.js"));
        writeEnd();

        writeStart("script", "type", "text/javascript", "src", cmsResource(scriptPrefix + "jquery.handsontable.full.js"));
        writeEnd();

        writeStart("script", "type", "text/javascript", "src", cmsResource(scriptPrefix + "d3.js"));
        writeEnd();

        writeStart("script", "type", "text/javascript");
            writeRaw("var require = ");
            writeRaw(ObjectUtils.toJson(ImmutableMap.of(
                    "baseUrl", cmsUrl(scriptPrefix),
                    "urlArgs", "_=" + System.currentTimeMillis())));
            writeRaw(";");
        writeEnd();

        writeStart("script", "type", "text/javascript", "src", cmsResource(scriptPrefix + "require.js"));
        writeEnd();

        writeStart("script", "type", "text/javascript", "src", cmsResource(scriptPrefix + theme + ".js"));
        writeEnd();

        String dropboxAppKey = getCmsTool().getDropboxApplicationKey();

        if (!ObjectUtils.isBlank(dropboxAppKey)) {
            writeStart("script",
                    "type", "text/javascript",
                    "src", "https://www.dropbox.com/static/api/1/dropins.js",
                    "id", "dropboxjs",
                    "data-app-key", dropboxAppKey);
            writeEnd();
        }

        for (Tool tool : tools) {
            tool.writeHeaderAfterScripts(this);
        }

        if (!ObjectUtils.isBlank(extraJavaScript)) {
            writeStart("script", "type", "text/javascript");
                write(extraJavaScript);
            writeEnd();
        }
    }

    /**
     * Writes the tool header with the given {@code title}.
     *
     * @param title If {@code null}, uses the default title.
     */
    public void writeHeader(String title) throws IOException {
        writeHeader(title, true);
    }

    /**
     * Writes the tool header with the default title.
     */
    public void writeHeader() throws IOException {
        writeHeader(null, true);
    }

    /** Writes the tool footer. */
    public void writeFooter() throws IOException {
        if (isAjaxRequest() || param(boolean.class, "_frame")) {
            return;
        }

                writeTag("/div");

                writeStart("div", "class", "toolFooter");
                    writeStart("a",
                            "target", "_blank",
                            "href", "http://www.brightspot.com/");
                        writeElement("img",
                                "src", cmsUrl("/style/brightspot.png"),
                                "alt", "Brightspot",
                                "width", 104,
                                "height", 14);
                    writeEnd();
                writeEnd();

                if (getCmsTool().isEnableCrossDomainInlineEditing() &&
                        !Query.from(Site.class).hasMoreThan(100)) {
                    Set<String> siteUrls = new HashSet<String>();

                    for (Site s : Query.from(Site.class).selectAll()) {
                        for (String url : s.getUrls()) {
                            try {
                                siteUrls.add(new URL(url).toURI().resolve("/").toString());
                            } catch (MalformedURLException error) {
                                // Ignore invalid site URL.
                            } catch (URISyntaxException error) {
                                // Ignore invalid site URL.
                            }
                        }
                    }

                    ToolUser user = getUser();
                    String userId = user != null ? user.getId().toString() : UUID.randomUUID().toString();
                    String signature = StringUtils.hex(StringUtils.hmacSha1(Settings.getSecret(), userId));
                    String cookiePath = StringUtils.addQueryParameters(
                            cmsUrl("/inlineEditorCookie"),
                            "userId", userId,
                            "signature", signature).
                            substring(1);

                    for (String siteUrl : siteUrls) {
                        writeStart("img", "src", siteUrl + cookiePath, "style", cssString(
                                "height", "1px",
                                "width", "1px",
                                "visibility", "hidden"));
                        writeEnd();
                    }
                }
            writeTag("/body");
        writeTag("/html");
    }

    /**
     * Writes a {@code <select>} tag that allows the user to pick multiple
     * content types.
     *
     * @param types Types that the user is allowed to select from.
     * If {@code null}, all content types will be available.
     * @param selectedTypes Types that should be initially selected.
     * @param attributes Attributes for the {@code <select>} tag.
     */
    public void writeMultipleTypeSelect(
            Iterable<ObjectType> types,
            Collection<ObjectType> selectedTypes,
            Object... attributes) throws IOException {

        writeTypeSelectReally(
                true,
                types,
                selectedTypes != null ? selectedTypes : Collections.<ObjectType>emptySet(),
                null,
                attributes);
    }

    /**
     * Writes a {@code <select>} tag that allows the user to pick a content
     * type.
     *
     * @param types Types that the user is allowed to select from.
     * If {@code null}, all content types will be available.
     * @param selectedType Type that should be initially selected.
     * @param allLabel Label for the option that selects all types.
     * If {@code null}, the option won't be available.
     * @param attributes Attributes for the {@code <select>} tag.
     */
    public void writeTypeSelect(
            Iterable<ObjectType> types,
            ObjectType selectedType,
            String allLabel,
            Object... attributes) throws IOException {

        writeTypeSelectReally(
                false,
                types,
                selectedType != null ? Arrays.asList(selectedType) : Collections.<ObjectType>emptySet(),
                allLabel,
                attributes);
    }

    private void writeTypeSelectReally(
            boolean multiple,
            Iterable<ObjectType> types,
            Collection<ObjectType> selectedTypes,
            String allLabel,
            Object... attributes) throws IOException {

        if (types == null) {
            types = Database.Static.getDefault().getEnvironment().getTypes();
        }

        List<ObjectType> typesList = ObjectUtils.to(new TypeReference<List<ObjectType>>() { }, types);

        for (Iterator<ObjectType> i = typesList.iterator(); i.hasNext();) {
            ObjectType type = i.next();

            if (!type.isConcrete() ||
                    !hasPermission("type/" + type.getId() + "/write") ||
                    (!getCmsTool().isDisplayTypesNotAssociatedWithJavaClasses() &&
                    type.getObjectClass() == null) ||
                    Draft.class.equals(type.getObjectClass()) ||
                    (type.isDeprecated() &&
                    !Query.fromType(type).hasMoreThan(0))) {
                i.remove();
            }
        }

        for (ObjectType type : Database.Static.getDefault().getEnvironment().getTypes()) {
            if (Boolean.FALSE.equals(type.as(ToolUi.class).getHidden()) && !type.isConcrete()) {
                if (typesList.containsAll(type.findConcreteTypes())) {
                    typesList.add(type);
                }
            }
        }

        Map<String, List<ObjectType>> typeGroups = new LinkedHashMap<String, List<ObjectType>>();
        List<ObjectType> mainTypes = Template.Static.findUsedTypes(getSite());

        mainTypes.retainAll(typesList);
        typesList.removeAll(mainTypes);
        typeGroups.put("Main Content Types", mainTypes);
        typeGroups.put("Misc Content Types", typesList);

        for (Iterator<List<ObjectType>> i = typeGroups.values().iterator(); i.hasNext();) {
            List<ObjectType> typeGroup = i.next();

            if (typeGroup.isEmpty()) {
                i.remove();

            } else {
                Collections.sort(typeGroup);
            }
        }

        writeStart("select",
                "multiple", multiple ? "multiple" : null,
                attributes);

            if (allLabel != null) {
                writeStart("option", "value", "").writeHtml(allLabel).writeEnd();
            }

            if (typeGroups.size() == 1) {
                writeTypeSelectGroup(selectedTypes, typeGroups.values().iterator().next());

            } else {
                for (Map.Entry<String, List<ObjectType>> entry : typeGroups.entrySet()) {
                    writeStart("optgroup", "label", entry.getKey());
                        writeTypeSelectGroup(selectedTypes, entry.getValue());
                    writeEnd();
                }
            }

        writeEnd();
    }

    private void writeTypeSelectGroup(Collection<ObjectType> selectedTypes, List<ObjectType> types) throws IOException {
        String previousLabel = null;

        for (ObjectType type : types) {
            String label = Static.getObjectLabel(type);

            writeStart("option",
                    "selected", selectedTypes.contains(type) ? "selected" : null,
                    "value", type.getId());
                writeHtml(label);
                if (label.equals(previousLabel)) {
                    writeHtml(" (");
                    writeHtml(type.getInternalName());
                    writeHtml(")");
                }
            writeEnd();

            previousLabel = label;
        }
    }

    public List<?> findDropDownItems(ObjectField field, Search dropDownSearch) {
        List<?> items;
        if (field.getTypes().contains(ObjectType.getInstance(ObjectType.class))) {
            List<ObjectType> types = new ArrayList<ObjectType>();
            Predicate predicate = dropDownSearch.toQuery(getSite()).getPredicate();

            for (ObjectType t : Database.Static.getDefault().getEnvironment().getTypes()) {
                if (t.is(predicate)) {
                    types.add(t);
                }
            }
            items = new ArrayList<Object>(types);
        } else {
            items = dropDownSearch.toQuery(getSite()).selectAll();
        }
        return items;
    }

    /**
     * Writes a {@code <select>} or {@code <input>} tag that allows the user
     * to pick a content.
     *
     * @param field Can't be {@code null}.
     * @param value Initial value. May be {@code null}.
     * @param attributes Extra attributes for the HTML tag.
     */
    public void writeObjectSelect(ObjectField field, Object value, Object... attributes) throws IOException {
        ErrorUtils.errorIfNull(field, "field");

        ToolUi ui = field.as(ToolUi.class);
        String placeholder = ObjectUtils.firstNonNull(ui.getPlaceholder(), "");

        if (field.isRequired()) {
            placeholder += " (Required)";
        }

        if (isObjectSelectDropDown(field)) {
            Search dropDownSearch = new Search(field);
            dropDownSearch.setParentId(param(UUID.class, OBJECT_ID_PARAMETER));
            dropDownSearch.setParentTypeId(param(UUID.class, TYPE_ID_PARAMETER));

            List<?> items = findDropDownItems(field, dropDownSearch);

            Collections.sort(items, new ObjectFieldComparator("_label", false));

            writeStart("select",
                    "data-searchable", "true",
                    "data-dynamic-placeholder", ui.getPlaceholderDynamicText(),
                    "data-dynamic-field-name", field.getInternalName(),
                    attributes);
                writeStart("option", "value", "");
                    writeHtml(placeholder);
                writeEnd();

                for (Object item : items) {
                    State itemState = State.getInstance(item);
                    writeStart("option",
                            "selected", item.equals(value) ? "selected" : null,
                            "value", itemState.getId());
                        writeObjectLabel(item);
                    writeEnd();
                }
            writeEnd();

        } else {
            State state = State.getInstance(value);
            StringBuilder typeIds = new StringBuilder();

            for (ObjectType type : field.getTypes()) {
                typeIds.append(type.getId());
                typeIds.append(',');
            }

            if (typeIds.length() > 0) {
                typeIds.setLength(typeIds.length() - 1);
            }

            writeElement("input",
                    "type", "text",
                    "class", "objectId",
                    "data-additional-query", field.getPredicate(),
                    "data-generic-argument-index", field.getGenericArgumentIndex(),
                    "data-dynamic-placeholder", ui.getPlaceholderDynamicText(),
                    "data-dynamic-field-name", field.getInternalName(),
                    "data-label", value != null ? getObjectLabel(value) : null,
                    "data-pathed", ToolUi.isOnlyPathed(field),
                    "data-preview", getPreviewThumbnailUrl(value),
                    "data-searcher-path", ui.getInputSearcherPath(),
                    "data-suggestions", ui.isEffectivelySuggestions(),
                    "data-typeIds", typeIds,
                    "data-visibility", value != null ? state.getVisibilityLabel() : null,
                    "value", value != null ? state.getId() : null,
                    "placeholder", placeholder,
                    attributes);
        }
    }

    /**
     * Writes a {@code <select>} tag that allows the user to pick a
     * visibility status.
     *
     * @param type May be {@code null}.
     * @param values Initial values. May be {@code null}.
     * @param attributes May be {@code null}.
     */
    public void writeMultipleVisibilitySelect(
            ObjectType type,
            Collection<String> values,
            Object... attributes) throws IOException {

        if (values == null) {
            values = Collections.emptySet();
        }

        Map<String, String> statuses = new HashMap<String, String>();

        statuses.put("p", "Published");

        boolean hasWorkflow = false;

        for (Workflow w : (type == null ?
                Query.from(Workflow.class) :
                Query.from(Workflow.class).where("contentTypes = ?", type)).
                selectAll()) {

            for (WorkflowState s : w.getStates()) {
                hasWorkflow = true;

                statuses.put("w." + s.getName(), s.getDisplayName());
            }
        }

        if (hasWorkflow) {
            statuses.put("w", "In Workflow");
        }

        addVisibilityStatuses(statuses, Database.Static.getDefault().getEnvironment());
        addVisibilityStatuses(statuses, type);

        List<Map.Entry<String, String>> sortedStatuses = new ArrayList<Map.Entry<String, String>>(statuses.entrySet());

        Collections.sort(sortedStatuses, new Comparator<Map.Entry<String, String>>() {

            @Override
            public int compare(Map.Entry<String, String> x, Map.Entry<String, String> y) {
                return x.getValue().compareTo(y.getValue());
            }
        });

        writeStart("select",
                "multiple", "multiple",
                "placeholder", "Status (Published)",
                attributes);

            for (Map.Entry<String, String> entry : sortedStatuses) {
                String key = entry.getKey();

                writeStart("option",
                        "selected", values.contains(key) ? "selected" : null,
                        "value", key);
                    writeHtml(entry.getValue());
                writeEnd();
            }
        writeEnd();
    }

    private void addVisibilityStatuses(Map<String, String> statuses, ObjectStruct struct) {
        if (struct == null) {
            return;
        }

        for (ObjectIndex index : struct.getIndexes()) {
            if (index.isVisibility()) {
                for (String fieldName : index.getFields()) {
                    ObjectField field = struct.getField(fieldName);

                    if (field != null) {
                        String type = field.getInternalItemType();

                        if (ObjectField.BOOLEAN_TYPE.equals(type)) {
                            String displayName = field.getDisplayName();

                            if (displayName.endsWith("?")) {
                                displayName = displayName.substring(0, displayName.length() - 1);
                            }

                            statuses.put("b." + field.getUniqueName(), displayName);

                        } else if (ObjectField.TEXT_TYPE.equals(type)) {
                            Set<ObjectField.Value> values = field.getValues();

                            if (values != null && !values.isEmpty()) {
                                for (ObjectField.Value value : values) {
                                    statuses.put("t." + field.getUniqueName() + "=" + value.getValue(), field.getDisplayName() + ": " + value.getLabel());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns {@code true} if the {@code <select>} tag would be used to allow
     * the user to pick a content for the given {@code field}.
     *
     * @param field Can't be {@code null}.
     */
    public boolean isObjectSelectDropDown(ObjectField field) {
        ErrorUtils.errorIfNull(field, "field");

        if (field.as(ToolUi.class).isDropDown()) {
            long dropDownMaximum = Settings.getOrDefault(long.class, "cms/tool/dropDownMaximum", 250L);

            if (field.getTypes().contains(ObjectType.getInstance(ObjectType.class))) {
                Set<ObjectType> types = Database.Static.getDefault().getEnvironment().getTypes();

                if (types.size() <= dropDownMaximum) {
                    return true;

                } else if (field.getPredicate() != null) {
                    long numFilteredTypes = 0;
                    for (ObjectType type : types) {
                        if (type.is(field.getPredicate())) {
                            if (++numFilteredTypes > dropDownMaximum) {
                                break;
                            }
                        }
                    }

                    return (numFilteredTypes <= dropDownMaximum);
                }

            } else {
                return !new Search(field).toQuery(getSite()).hasMoreThan(dropDownMaximum);
            }

        }

        return false;

    }

    /** Writes all grid CSS, or does nothing if it's already written. */
    public ToolPageContext writeGridCssOnce() throws IOException {
        LayoutTag.Static.writeGridCss(this, getServletContext(), getRequest());
        return this;
    }

    /**
     * Writes the heading that precedes the form to create or update the
     * given {@code object}.
     *
     * @param attributes Extra attributes for the heading element.
     */
    public void writeFormHeading(Object object, Object... attributes) throws IOException {
        State state = State.getInstance(object);
        ObjectType type = state.getType();
        String typeLabel = getTypeLabel(object);
        String iconName = null;

        if (type != null) {
            iconName = type.as(ToolUi.class).getIconName();
        }

        if (ObjectUtils.isBlank(iconName)) {
            iconName = "object";
        }

        writeStart("h1",
                "class", "icon icon-" + iconName,
                attributes);
            if (state.isNew()) {
                writeHtml("New ");
                writeHtml(typeLabel);

            } else {
                writeHtml("Edit ");
                writeHtml(typeLabel);
            }
        writeEnd();
    }

    /**
     * Disables all form fields after this call so that they're displayed but
     * not processed on update.
     */
    public void disableFormFields() {
        HttpServletRequest request = getRequest();
        Integer disabled = (Integer) request.getAttribute(FORM_FIELDS_DISABLED_ATTRIBUTE);

        request.setAttribute(FORM_FIELDS_DISABLED_ATTRIBUTE, disabled != null ? disabled + 1 : 1);
    }

    /**
     * Enables all form fields after this call so that they're both displayed
     * and processed on update.
     */
    public void enableFormFields() {
        HttpServletRequest request = getRequest();
        Integer disabled = (Integer) request.getAttribute(FORM_FIELDS_DISABLED_ATTRIBUTE);

        if (disabled != null) {
            request.setAttribute(FORM_FIELDS_DISABLED_ATTRIBUTE, disabled - 1);
        }
    }

    /**
     * Returns {@code true} if the form fields are enabled to be both
     * displayed and processed on update.
     */
    public boolean isFormFieldsDisabled() {
        Integer disabled = (Integer) getRequest().getAttribute(FORM_FIELDS_DISABLED_ATTRIBUTE);
        return disabled != null && disabled > 0;
    }

    /**
     * Writes a contextual message if the given {@code object} is in trash.
     *
     * @param object Can't be {@code null}.
     * @return {@code true} if the message was written.
     */
    public boolean writeTrashMessage(Object object) throws IOException {
        State state = State.getInstance(object);
        Content.ObjectModification contentData = state.as(Content.ObjectModification.class);

        if (!contentData.isTrash()) {
            return false;
        }

        writeStart("div", "class", "message message-warning");
            writeStart("p");
                writeHtml("Archived ");
                writeHtml(formatUserDateTime(contentData.getUpdateDate()));
                writeHtml(" by ");
                writeObjectLabel(contentData.getUpdateUser());
                writeHtml(".");
            writeEnd();

            writeStart("div", "class", "actions");
                writeStart("button",
                        "class", "link icon icon-action-restore",
                        "name", "action-restore",
                        "value", "true");
                    writeHtml("Restore");
                writeEnd();

                writeStart("button",
                        "class", "link icon icon-action-delete",
                        "name", "action-delete",
                        "value", "true");
                    writeHtml("Delete Permanently");
                writeEnd();
            writeEnd();
        writeEnd();

        return true;
    }

    private void includeFromCms(String path, Object... attributes) throws IOException, ServletException {
        JspUtils.include(getRequest(), getResponse(), getWriter(), toolPath(CmsTool.class, path), attributes);
    }

    /**
     * Writes some form fields for the given {@code object}.
     *
     * @param object Can't be {@code null}.
     * @param includeGlobals {@true} to include global fields.
     * @param includeFields {@code null} to include all fields.
     * @param excludeFields {@code null} to exclude no fields.
     */
    public void writeSomeFormFields(
            Object object,
            boolean includeGlobals,
            Collection<String> includeFields,
            Collection<String> excludeFields)
            throws IOException, ServletException {

        State state = State.getInstance(object);
        ObjectType type = state.getType();
        List<ObjectField> fields = new ArrayList<>();

        if (type != null) {
            fields.addAll(type.getFields());
        }

        if (includeGlobals && !fields.isEmpty()) {
            writeElement("input",
                    "type", "hidden",
                    "name", state.getId() + "/_includeGlobals",
                    "value", true);

            for (ObjectField field : state.getDatabase().getEnvironment().getFields()) {
                if (Boolean.FALSE.equals(field.getState().get("cms.ui.hidden"))) {
                    fields.add(field);
                }
            }
        }

        HttpServletRequest request = getRequest();
        Object oldContainer = request.getAttribute("containerObject");

        try {
            if (oldContainer == null) {
                request.setAttribute("containerObject", object);
            }

            List<ToolUiLayoutElement> layoutPlaceholders = type.as(ToolUi.class).getLayoutPlaceholders();
            String layoutPlaceholdersJson = null;

            if (!layoutPlaceholders.isEmpty()) {
                List<Map<String, Object>> jsons = new ArrayList<Map<String, Object>>();

                for (ToolUiLayoutElement element : layoutPlaceholders) {
                    jsons.add(element.toMap());
                }

                layoutPlaceholdersJson = ObjectUtils.toJson(jsons);
            }

            writeStart("div",
                    "class", "objectInputs",
                    "data-type", type != null ? type.getInternalName() : null,
                    "data-id", state.getId(),
                    "data-object-id", state.getId(),
                    "data-layout-placeholders", layoutPlaceholdersJson);

                Object original = Query.fromAll().where("_id = ?", state.getId()).master().noCache().first();

                if (original != null) {
                    Date updateDate = State.getInstance(original).as(Content.ObjectModification.class).getUpdateDate();

                    if (updateDate != null) {
                        writeElement("input",
                                "type", "hidden",
                                "name", state.getId() + "/_updateDate",
                                "value", updateDate.getTime());
                    }
                }

                if (type != null) {
                    String noteHtml = type.as(ToolUi.class).getEffectiveNoteHtml(object);

                    if (!ObjectUtils.isBlank(noteHtml)) {
                        write("<div class=\"message message-info\">");
                        write(noteHtml);
                        write("</div>");
                    }
                }

                if (object instanceof Query) {
                    writeStart("div", "class", "queryField");
                        writeElement("input",
                                "type", "text",
                                "name", state.getId() + "/_query",
                                "value", ObjectUtils.toJson(state.getSimpleValues()));
                    writeEnd();

                } else if (!fields.isEmpty()) {
                    ContentType ct = type != null ? Query.from(ContentType.class).where("internalName = ?", type.getInternalName()).first() : null;

                    if (ct != null) {
                        List<ObjectField> firsts = new ArrayList<ObjectField>();

                        for (ContentField cf : ct.getFields()) {
                            for (Iterator<ObjectField> i = fields.iterator(); i.hasNext();) {
                                ObjectField field = i.next();

                                if (field.getInternalName().equals(cf.getInternalName())) {
                                    firsts.add(field);
                                    i.remove();
                                    break;
                                }
                            }
                        }

                        fields.addAll(0, firsts);

                    } else {
                        List<ObjectField> firsts = new ArrayList<ObjectField>();
                        List<ObjectField> lasts = new ArrayList<ObjectField>();

                        for (Iterator<ObjectField> i = fields.iterator(); i.hasNext();) {
                            ObjectField field = i.next();
                            ToolUi ui = field.as(ToolUi.class);

                            if (ui.isDisplayFirst()) {
                                firsts.add(field);
                                i.remove();

                            } else if (ui.isDisplayLast()) {
                                lasts.add(field);
                                i.remove();
                            }
                        }

                        fields.addAll(0, firsts);
                        fields.addAll(lasts);
                    }

                    boolean draftCheck = false;

                    try {
                        if (request.getAttribute("firstDraft") == null) {
                            draftCheck = true;

                            request.setAttribute("firstDraft", state.isNew());
                            request.setAttribute("finalDraft", !state.isNew() &&
                                    !state.as(Content.ObjectModification.class).isDraft() &&
                                    state.as(Workflow.Data.class).getCurrentState() == null &&
                                    getOverlaidDraft(object) == null);
                        }

                        for (ObjectField field : fields) {
                            String name = field.getInternalName();

                            if ((includeFields == null ||
                                    includeFields.contains(name)) &&
                                    (excludeFields == null ||
                                    !excludeFields.contains(name))) {

                                renderField(object, field);
                            }
                        }

                    } finally {
                        if (draftCheck) {
                            request.setAttribute("firstDraft", null);
                            request.setAttribute("finalDraft", null);
                        }
                    }

                } else {
                    writeStart("div", "class", "inputContainer");
                        writeStart("div", "class", "inputLabel");
                            writeStart("label", "for", createId());
                                writeHtml("Data");
                            writeEnd();
                        writeEnd();

                        writeStart("div", "class", "inputSmall");
                            writeStart("textarea",
                                    "data-code-type", "text/json",
                                    "id", getId(),
                                    "name", "data");
                                writeHtml(ObjectUtils.toJson(state.getSimpleValues(), true));
                            writeEnd();
                        writeEnd();
                    writeEnd();
                }
            writeEnd();

        } finally {
            if (oldContainer == null) {
                request.setAttribute("containerObject", null);
            }
        }
    }

    /**
     * Writes all form fields for the given {@code object}.
     *
     * @param object Can't be {@code null}.
     */
    public void writeFormFields(Object object) throws IOException, ServletException {
        writeSomeFormFields(object, false, null, null);
    }

    /**
     * Writes a standard form for the given {@code object}.
     *
     * @param object Can't be {@code null}.
     * @param displayTrashAction If {@code null}, displays the trash action
     * instead of the delete action.
     */
    public void writeStandardForm(Object object, boolean displayTrashAction) throws IOException, ServletException {
        State state = State.getInstance(object);
        ObjectType type = state.getType();

        writeFormHeading(object);

        writeStart("div", "class", "widgetControls");
            includeFromCms("/WEB-INF/objectVariation.jsp", "object", object);
        writeEnd();

        includeFromCms("/WEB-INF/objectMessage.jsp", "object", object);

        writeStart("form",
                "class", "standardForm",
                "method", "post",
                "enctype", "multipart/form-data",
                "action", url("", "id", state.getId()),
                "autocomplete", "off",
                "data-type", type != null ? type.getInternalName() : null);
            boolean trash = writeTrashMessage(object);

            writeFormFields(object);

            if (!trash) {
                writeStart("div", "class", "actions");
                    writeStart("button",
                            "class", "icon icon-action-save",
                            "name", "action-save",
                            "value", "true");
                        writeHtml("Save");
                    writeEnd();

                    if (!state.isNew() &&
                            (type == null ||
                            (!type.getGroups().contains(Singleton.class.getName()) &&
                            !type.getGroups().contains(Tool.class.getName())))) {
                        if (displayTrashAction) {
                            writeStart("button",
                                    "class", "icon icon-action-trash action-pullRight link",
                                    "name", "action-trash",
                                    "value", "true");
                                writeHtml("Archive");
                            writeEnd();

                        } else {
                            writeStart("button",
                                    "class", "icon icon-action-delete action-pullRight link",
                                    "name", "action-delete",
                                    "value", "true");
                                writeHtml("Delete");
                            writeEnd();
                        }
                    }
                writeEnd();
            }
        writeEnd();
    }

    /**
     * Writes a standard form for the given {@code object} with the trash
     * action.
     *
     * @param object Can't be {@code null}.
     * @see #writeStandardForm(Object, boolean)
     */
    public void writeStandardForm(Object object) throws IOException, ServletException {
        writeStandardForm(object, true);
    }

    /**
     * Writes a link that points to either the Javadoc or the source for the
     * given {@code objectClass}.
     *
     * @param objectClass Can't be {@code null}.
     */
    public void writeJavaClassLink(Class<?> objectClass) throws IOException {
        String objectClassName = objectClass.getName();
        String javadocUrlPrefix;

        if (objectClassName.startsWith("com.psddev.cms.db.")) {
            javadocUrlPrefix = "http://public.psddev.com/javadoc/brightspot-cms/";

        } else if (objectClassName.startsWith("com.psddev.dari.db.")) {
            javadocUrlPrefix = "http://public.psddev.com/javadoc/dari/";

        } else {
            javadocUrlPrefix = null;
        }

        if (ObjectUtils.isBlank(javadocUrlPrefix)) {
            File source = CodeUtils.getSource(objectClassName);

            if (source != null) {
                writeStart("a",
                        "target", "_blank",
                        "href", DebugFilter.Static.getServletPath(getRequest(), "code",
                                "file", source));
                    writeStart("code");
                        writeHtml(objectClassName);
                    writeEnd();
                writeEnd();

            } else {
                writeStart("code");
                    writeHtml(objectClassName);
                writeEnd();
            }

        } else {
            writeStart("a",
                    "target", "_blank",
                    "href", javadocUrlPrefix + objectClassName.replace('.', '/').replace('$', '.') + ".html");
                writeStart("code");
                    writeHtml(objectClassName);
                writeEnd();
            writeEnd();
        }
    }

    /**
     * Updates the given {@code object} using all request parameters.
     *
     * @param object Can't be {@code null}.
     */
    public void updateUsingParameters(Object object) throws IOException, ServletException {
        includeFromCms("/WEB-INF/objectPost.jsp", "object", object);
    }

    /**
     * Updates the given {@code object} using all widgets with the data from
     * the current request.
     *
     * @param object Can't be {@code null}.
     */
    @SuppressWarnings("deprecation")
    public void updateUsingAllWidgets(Object object) throws Exception {
        ErrorUtils.errorIfNull(object, "object");

        State state = State.getInstance(object);
        List<String> requestWidgets = params(String.class, state.getId() + "/_widget");

        if (requestWidgets.isEmpty()) {
            return;
        }

        DependencyResolver<Widget> widgets = new DependencyResolver<Widget>();

        for (Widget widget : Tool.Static.getPluginsByClass(Widget.class)) {
            widgets.addRequired(widget, widget.getUpdateDependencies());
        }

        for (Widget widget : widgets.resolve()) {
            for (String requestWidget : requestWidgets) {
                if (widget.getInternalName().equals(requestWidget)) {
                    widget.update(this, object);
                    break;
                }
            }
        }

        Page.Layout layout = (Page.Layout) getRequest().getAttribute("layoutHack");

        if (layout != null) {
            ((Page) object).setLayout(layout);
        }
    }

    private void redirectOnSave(String url, Object... parameters) throws IOException {
        if (getUser().isReturnToDashboardOnSave()) {
            getResponse().sendRedirect(cmsUrl("/"));

        } else {
            getResponse().sendRedirect(StringUtils.addQueryParameters(url(url, parameters), "editAnyway", null));
        }
    }

    /**
     * Tries to delete the given {@code object} if the user has asked for it
     * in the current request.
     *
     * @param object Can't be {@code null}.
     * @return {@code true} if the delete is tried.
     */
    public boolean tryDelete(Object object) {
        if (!isFormPost() ||
                param(String.class, "action-delete") == null) {
            return false;
        }

        try {
            State state = State.getInstance(object);

            if (param(UUID.class, "draftId") != null) {
                Draft draft = getOverlaidDraft(object);

                if (draft != null) {
                    draft.delete();

                    if (state.as(Content.ObjectModification.class).isDraft()) {
                        state.delete();
                    }

                    Schedule schedule = draft.getSchedule();

                    if (schedule != null &&
                            ObjectUtils.isBlank(schedule.getName())) {
                        schedule.delete();
                    }
                }

            } else {
                state.delete();
            }

            redirectOnSave("");
            return true;

        } catch (Exception error) {
            getErrors().add(error);
            return false;
        }
    }

    /**
     * Returns the publish date from the content form.
     *
     * @return May be {@code null}.
     */
    public Date getContentFormPublishDate() {
        Date publishDate = param(Date.class, "publishDate");

        if (publishDate != null) {
            DateTimeZone timeZone = getUserDateTimeZone();
            publishDate = new Date(DateTimeFormat.
                    forPattern("yyyy-MM-dd HH:mm:ss").
                    withZone(timeZone).
                    parseMillis(new DateTime(publishDate).toString("yyyy-MM-dd HH:mm:ss")));

            if (publishDate.before(new Date(new DateTime(timeZone).getMillis()))) {
                publishDate = null;
            }
        }

        return publishDate;
    }

    /**
     * Sets the publish date from the content form as the schedule date
     * on the given {@code object}.
     *
     * @param object Can't be {@code null}.
     */
    public void setContentFormScheduleDate(Object object) {
        State state = State.getInstance(object);
        Content.ObjectModification contentData = state.as(Content.ObjectModification.class);
        Date publishDate = getContentFormPublishDate();

        if (publishDate != null) {
            contentData.setPublishDate(publishDate);
        }

        contentData.setScheduleDate(publishDate);
    }

    /**
     * Tries to save the given {@code object} as a draft if the user has
     * asked for it in the current request.
     *
     * @param object Can't be {@code null}.
     * @return {@code true} if the save is tried.
     */
    public boolean tryDraft(Object object) {
        if (!isFormPost() ||
                param(String.class, "action-draft") == null) {
            return false;
        }

        setContentFormScheduleDate(object);

        State state = State.getInstance(object);
        Draft draft = getOverlaidDraft(object);
        Site site = getSite();

        try {
            updateUsingParameters(object);
            updateUsingAllWidgets(object);

            if (state.isNew() &&
                    site != null &&
                    site.getDefaultVariation() != null) {
                state.as(Variation.Data.class).setInitialVariation(site.getDefaultVariation());
            }

            if (draft == null) {
                if (state.isNew() ||
                        state.as(Content.ObjectModification.class).isDraft()) {
                    state.as(Content.ObjectModification.class).setDraft(true);
                    publish(state);
                    redirectOnSave("",
                            "_frame", param(boolean.class, "_frame") ? Boolean.TRUE : null,
                            "id", state.getId(),
                            "copyId", null);
                    return true;

                } else if (state.as(Workflow.Data.class).getCurrentState() != null) {
                    publish(state);
                    redirectOnSave("",
                            "_frame", param(boolean.class, "_frame") ? Boolean.TRUE : null);
                    return true;
                }

                draft = new Draft();
                draft.setOwner(getUser());
                draft.setObject(object);

            } else {
                draft.setObject(object);
            }

            publish(draft);
            redirectOnSave("",
                    "_frame", param(boolean.class, "_frame") ? Boolean.TRUE : null,
                    ToolPageContext.DRAFT_ID_PARAMETER, draft.getId(),
                    ToolPageContext.HISTORY_ID_PARAMETER, null);
            return true;

        } catch (Exception error) {
            getErrors().add(error);
            return false;
        }
    }

    /**
     * Tries to publish or schedule the given {@code object} if the user has
     * asked for it in the current request.
     *
     * @param object Can't be {@code null}.
     * @return {@code true} if the restore is tried.
     */
    public boolean tryPublish(Object object) {
        if (!isFormPost() ||
                param(String.class, "action-publish") == null) {
            return false;
        }

        State state = State.getInstance(object);
        Content.ObjectModification contentData = state.as(Content.ObjectModification.class);
        ToolUser user = getUser();

        if (state.isNew() ||
                object instanceof Draft ||
                contentData.isDraft() ||
                state.as(Workflow.Data.class).getCurrentState() != null) {
            if (getContentFormPublishDate() != null) {
                setContentFormScheduleDate(object);

            } else {
                contentData.setPublishDate(new Date());
                contentData.setPublishUser(user);
            }
        }

        Draft draft = getOverlaidDraft(object);
        UUID variationId = param(UUID.class, "variationId");
        Site site = getSite();

        try {
            state.beginWrites();
            state.as(Workflow.Data.class).changeState(null, user, (WorkflowLog) null);

            if (variationId == null ||
                    (site != null &&
                    ((state.isNew() && site.getDefaultVariation() != null) ||
                    ObjectUtils.equals(site.getDefaultVariation(), state.as(Variation.Data.class).getInitialVariation())))) {
                if (state.isNew() && site != null && site.getDefaultVariation() != null) {
                    state.as(Variation.Data.class).setInitialVariation(site.getDefaultVariation());
                }

                getRequest().setAttribute("original", object);
                includeFromCms("/WEB-INF/objectPost.jsp", "object", object, "original", object);
                updateUsingAllWidgets(object);

                if (variationId != null &&
                        variationId.equals(state.as(Variation.Data.class).getInitialVariation())) {
                    state.putByPath("variations/" + variationId.toString(), null);
                }

            } else {
                Object original = Query.
                        from(Object.class).
                        where("_id = ?", state.getId()).
                        noCache().
                        first();
                Map<String, Object> oldStateValues = State.getInstance(original).getSimpleValues();

                getRequest().setAttribute("original", original);
                includeFromCms("/WEB-INF/objectPost.jsp", "object", object, "original", original);
                updateUsingAllWidgets(object);

                Map<String, Object> newStateValues = state.getSimpleValues();
                Set<String> stateKeys = new LinkedHashSet<String>();
                Map<String, Object> stateValues = new LinkedHashMap<String, Object>();

                stateKeys.addAll(oldStateValues.keySet());
                stateKeys.addAll(newStateValues.keySet());

                for (String key : stateKeys) {
                    Object value = newStateValues.get(key);
                    if (!ObjectUtils.equals(oldStateValues.get(key), value)) {
                        stateValues.put(key, value);
                    }
                }

                State.getInstance(original).putByPath("variations/" + variationId.toString(), stateValues);
                State.getInstance(original).getExtras().put("cms.variedObject", object);
                object = original;
                state = State.getInstance(object);
            }

            Schedule schedule = user.getCurrentSchedule();
            Date publishDate = null;

            if (schedule == null) {
                publishDate = getContentFormPublishDate();

            } else if (draft == null) {
                draft = Query.
                        from(Draft.class).
                        where("schedule = ?", schedule).
                        and("objectId = ?", object).
                        first();
            }

            if (schedule != null || publishDate != null) {
                if (!state.validate()) {
                    throw new ValidationException(Arrays.asList(state));
                }

                if (draft == null || param(boolean.class, "newSchedule")) {
                    draft = new Draft();
                    draft.setOwner(user);
                }

                draft.setObject(object);

                if (state.isNew() || contentData.isDraft()) {
                    contentData.setDraft(true);
                    publish(state);
                    draft.setObjectChanges(null);
                }

                if (schedule == null) {
                    schedule = draft.getSchedule();
                }

                if (schedule == null) {
                    schedule = new Schedule();
                    schedule.setTriggerSite(site);
                    schedule.setTriggerUser(user);
                }

                if (publishDate != null) {
                    schedule.setTriggerDate(publishDate);
                    schedule.save();
                }

                draft.setSchedule(schedule);
                publish(draft);
                state.commitWrites();
                redirectOnSave("",
                        "_frame", param(boolean.class, "_frame") ? Boolean.TRUE : null,
                        ToolPageContext.DRAFT_ID_PARAMETER, draft.getId());

            } else {
                if (draft != null) {
                    draft.delete();
                }

                if (draft != null || contentData.isDraft()) {
                    contentData.setDraft(false);
                }

                if (!state.isVisible()) {
                    contentData.setPublishDate(null);
                    contentData.setPublishUser(null);
                }

                publish(object);
                state.commitWrites();
                redirectOnSave("",
                        "_frame", param(boolean.class, "_frame") ? Boolean.TRUE : null,
                        "typeId", state.getTypeId(),
                        "id", state.getId(),
                        "historyId", null,
                        "copyId", null,
                        "ab", null,
                        "published", System.currentTimeMillis());
            }

            return true;

        } catch (Exception error) {
            getErrors().add(error);
            return false;

        } finally {
            state.endWrites();
        }
    }

    /**
     * Tries to restore the given {@code object} if the user has asked for it
     * in the current request.
     *
     * @param object Can't be {@code null}.
     * @return {@code true} if the restore is tried.
     */
    public boolean tryRestore(Object object) {
        if (!isFormPost() ||
                param(String.class, "action-restore") == null) {
            return false;
        }

        try {
            Draft draft = getOverlaidDraft(object);
            State state = State.getInstance(draft != null ? draft : object);

            state.as(Content.ObjectModification.class).setTrash(false);
            publish(state);
            redirectOnSave("");
            return true;

        } catch (Exception error) {
            getErrors().add(error);
            return false;
        }
    }

    /**
     * Tries to save the given {@code object} if the user has asked for it
     * in the current request.
     *
     * @param object Can't be {@code null}.
     * @return {@code true} if the trash is tried.
     */
    public boolean trySave(Object object) {
        if (!isFormPost() ||
                param(String.class, "action-save") == null) {
            return false;
        }

        State state = State.getInstance(object);

        try {
            updateUsingParameters(object);
            state.save();
            redirectOnSave("",
                    "_frame", param(boolean.class, "_frame") ? Boolean.TRUE : null,
                    "id", state.getId());
            return true;

        } catch (Exception error) {
            getErrors().add(error);
            return false;
        }
    }

    /**
     * Tries to apply a standard set of updates to the given {@code object}
     * if the user has asked for any in the current request.
     *
     * <p>This method calls the following methods in order:</p>
     *
     * <ul>
     * <li>{@link #tryDelete}</li>
     * <li>{@link #tryRestore}</li>
     * <li>{@link #trySave}</li>
     * <li>{@link #tryTrash}</li>
     * </ul>
     *
     * @param object Can't be {@code null}.
     * @return {@code true} if the trash is tried.
     */
    public boolean tryStandardUpdate(Object object) {
        return tryDelete(object) ||
                tryRestore(object) ||
                trySave(object) ||
                tryTrash(object);
    }

    /**
     * Tries to trash the given {@code object} if the user has asked for it
     * in the current request.
     *
     * @param object Can't be {@code null}.
     * @return {@code true} if the trash is tried.
     */
    public boolean tryTrash(Object object) {
        if (!isFormPost() ||
                param(String.class, "action-trash") == null) {
            return false;
        }

        try {
            Draft draft = getOverlaidDraft(object);

            trash(draft != null ? draft : object);
            redirectOnSave("");
            return true;

        } catch (Exception error) {
            getErrors().add(error);
            return false;
        }
    }

    /**
     * Tries to apply a workflow action to the given {@code object} if the
     * user has asked for it in the current request.
     *
     * @param object Can't be {@code null}.
     * @param {@code true} if the application of a workflow action is tried.
     */
    public boolean tryWorkflow(Object object) {
        if (!isFormPost()) {
            return false;
        }

        String action = param(String.class, "action-workflow");

        if (ObjectUtils.isBlank(action)) {
            return false;
        }

        setContentFormScheduleDate(object);

        State state = State.getInstance(object);
        Draft draft = getOverlaidDraft(object);
        Workflow.Data workflowData = state.as(Workflow.Data.class);
        String oldWorkflowState = workflowData.getCurrentState();

        try {
            state.beginWrites();

            Workflow workflow = Query.from(Workflow.class).where("contentTypes = ?", state.getType()).first();

            if (workflow != null) {
                WorkflowTransition transition = workflow.getTransitions().get(action);

                if (transition != null) {
                    WorkflowLog log = new WorkflowLog();

                    updateUsingParameters(object);
                    updateUsingAllWidgets(object);
                    state.as(Content.ObjectModification.class).setDraft(false);
                    log.getState().setId(param(UUID.class, "workflowLogId"));
                    updateUsingParameters(log);
                    workflowData.changeState(transition, getUser(), log);

                    if (draft == null) {
                        publish(object);

                    } else {
                        draft.as(Workflow.Data.class).changeState(transition, getUser(), log);
                        draft.setObject(object);
                        publish(draft);
                    }

                    state.commitWrites();
                }
            }

            redirectOnSave("", "id", state.getId());
            return true;

        } catch (Exception error) {
            if (draft != null) {
                draft.as(Workflow.Data.class).revertState(oldWorkflowState);
            }

            workflowData.revertState(oldWorkflowState);
            getErrors().add(error);
            return false;

        } finally {
            state.endWrites();
        }
    }

    // --- AuthenticationFilter bridge ---

    /** @see AuthenticationFilter.Static#requireUser */
    public boolean requireUser() throws IOException {
        return AuthenticationFilter.Static.requireUser(getServletContext(), getRequest(), getResponse());
    }

    /**
     * Returns the current user accessing the tool.
     *
     * @see AuthenticationFilter.Static#getUser
     */
    public ToolUser getUser() {
        return AuthenticationFilter.Static.getUser(getRequest());
    }

    /**
     * Returns the current tool user setting value associated with the given
     * {@code key}.
     *
     * @see AuthenticationFilter.Static#getUserSetting
     */
    public Object getUserSetting(String key) {
        return AuthenticationFilter.Static.getUserSetting(getRequest(), key);
    }

    /**
     * Puts the given setting {@code value} at the given {@code key} for
     * the current tool user.
     *
     * @see AuthenticationFilter.Static#putUserSetting
     */
    public void putUserSetting(String key, Object value) {
        AuthenticationFilter.Static.putUserSetting(getRequest(), key, value);
    }

    /**
     * Returns the page setting value associated with the given {@code key}.
     *
     * @see AuthenticationFilter.Static#getPageSetting
     */
    public Object getPageSetting(String key) {
        return AuthenticationFilter.Static.getPageSetting(getRequest(), key);
    }

    /**
     * Puts the page setting {@code value} at the given {@code key}.
     *
     * @see AuthenticationFilter.Static#putPageSetting
     */
    public void putPageSetting(String key, Object value) {
        AuthenticationFilter.Static.putPageSetting(getRequest(), key, value);
    }

    /**
     * Returns the site that the {@linkplain #getUser current user}
     * is accessing.
     */
    public Site getSite() {
        ToolUser user = getUser();
        return user != null ? user.getCurrentSite() : null;
    }

    /**
     * Returns {@code true} if the {@linkplain #getUser current user}
     * is allowed access to the resources identified by the given
     * {@code permissionId}.
     *
     * @param If {@code null}, returns {@code true}.
     */
    public boolean hasPermission(String permissionId) {
        ToolUser user = getUser();

        return user != null &&
                (permissionId == null ||
                user.hasPermission(permissionId));
    }

    public boolean requirePermission(String permissionId) throws IOException {
        if (requireUser()) {
            return true;

        } else {
            if (hasPermission(permissionId)) {
                return false;

            } else {
                getResponse().sendError(Settings.isProduction() ?
                        HttpServletResponse.SC_NOT_FOUND :
                        HttpServletResponse.SC_FORBIDDEN);
                return true;
            }
        }
    }

    // --- Content.Static bridge ---

    /**
     * @see Content.Static#deleteSoftly
     * @deprecated Use {@link #trash} instead.
     */
    @Deprecated
    public Trash deleteSoftly(Object object) {
        return Content.Static.deleteSoftly(object, getSite(), getUser());
    }

    /** @see Content.Static#publish */
    public History publish(Object object) {
        History history = Content.Static.publish(object, getSite(), getUser());

        if (history != null &&
                param(boolean.class, "editAnyway")) {
            history.setLockIgnored(true);
            history.save();
        }

        return history;
    }

    /**
     * @see Content.Static#trash
     */
    public void trash(Object object) {
        Content.Static.trash(object, getSite(), getUser());
    }

    /** @see Content.Static#purge */
    public void purge(Object object) {
        Content.Static.purge(object, getSite(), getUser());
    }

    // --- WebPageContext support ---

    @Deprecated
    private PageWriter pageWriter;

    @Deprecated
    @Override
    public PageWriter getWriter() throws IOException {
        if (pageWriter == null) {
            pageWriter = new PageWriter(super.getWriter());
        }

        return pageWriter;
    }

    /** {@link ToolPageContext} utility methods. */
    public static final class Static {

        private Static() {
        }

        private static String notTooShort(String word) {
            char[] letters = word.toCharArray();
            StringBuilder not = new StringBuilder();
            int index = 0;
            int length = letters.length;

            for (; index < 5 && index < length; ++ index) {
                char letter = letters[index];

                if (Character.isWhitespace(letter)) {
                    not.append('\u00a0');
                } else {
                    not.append(letter);
                }
            }

            if (index < length) {
                not.append(letters, index, length - index);
            }

            return not.toString();
        }

        /**
         * Returns a label, or the given {@code defaultLabel} if one can't be
         * found, for the given {@code object}.
         */
        public static String getObjectLabelOrDefault(Object object, String defaultLabel) {
            State state = State.getInstance(object);

            if (state != null) {
                String label = state.getLabel();

                if (ObjectUtils.to(UUID.class, label) == null) {
                    return notTooShort(label);
                }
            }

            return notTooShort(defaultLabel);
        }

        /** Returns a label for the given {@code object}. */
        public static String getObjectLabel(Object object) {
            State state = State.getInstance(object);
            String label = null;

            if (state != null) {
                label = state.getLabel();
            }

            if (ObjectUtils.isBlank(label)) {
                label = "Not Available";
            }

            return notTooShort(label);
        }

        /**
         * Returns a label, or the given {@code defaultLabel} if one can't be
         * found, for the type of the given {@code object}.
         */
        public static String getTypeLabelOrDefault(Object object, String defaultLabel) {
            State state = State.getInstance(object);

            if (state != null) {
                ObjectType type = state.getType();

                if (type != null) {
                    return getObjectLabel(type);
                }
            }

            return notTooShort(defaultLabel);
        }

        /** Returns a label for the type of the given {@code object}. */
        public static String getTypeLabel(Object object) {
            return getTypeLabelOrDefault(object, "Unknown Type");
        }
    }

    // --- Deprecated ---

    /** @deprecated Use {@link ToolPageContext(ServletContext, HttpServletRequest, HttpServletResponse} instead. */
    @Deprecated
    public ToolPageContext(
            Servlet servlet,
            HttpServletRequest request,
            HttpServletResponse response) {

        super(servlet, request, response);
    }

    /** @deprecated Use {@link Database.Static#getDefault} instead. */
    @Deprecated
    public Database getDatabase() {
        return Database.Static.getDefault();
    }

    /** @deprecated Use {@link Query#from} instead. */
    @Deprecated
    public <T> Query<T> queryFrom(Class<T> objectClass) {
        Query<T> query = Query.from(objectClass);
        query.setDatabase(getDatabase());
        return query;
    }

    /**
     * Returns an HTML-escaped label, or the given {@code defaultLabel} if
     * one can't be found, for the given {@code object}.
     *
     * @deprecated Use {@link #getObjectLabelOrDefault} and {@link #h} instead.
     */
    @Deprecated
    public String objectLabel(Object object, String defaultLabel) {
        return h(getObjectLabelOrDefault(object, defaultLabel));
    }

    /**
     * Returns an HTML-escaped label for the given {@code object}.
     *
     * @deprecated Use {@link getObjectLabel} and {@link #h} instead.
     */
    @Deprecated
    public String objectLabel(Object object) {
        return h(getObjectLabel(object));
    }

    /**
     * Returns an HTML-escaped label, or the given {@code defaultLabel} if
     * one can't be found, for the type of the given {@code object}.
     *
     * @deprecated Use {@link #getTypeLabelOrDefault} and {@link #h} instead.
     */
    @Deprecated
    public String typeLabel(Object object, String defaultLabel) {
        return h(getTypeLabelOrDefault(object, defaultLabel));
    }

    /**
     * Returns an HTML-escaped label for the type of the given
     * {@code object}.
     *
     * @deprecated Use {@link #getTypeLabel} and {@link #h} instead.
     */
    @Deprecated
    public String typeLabel(Object object) {
        return h(getTypeLabel(object));
    }

    /** @deprecated Use {@link writeTypeSelect} instead. */
    @Deprecated
    public void typeSelect(
            Iterable<ObjectType> types,
            ObjectType selectedType,
            String allLabel,
            Object... attributes) throws IOException {

        writeTypeSelect(types, selectedType, allLabel, attributes);
    }

    /** @deprecated Use {@link writeObjectSelect} instead. */
    @Deprecated
    public void objectSelect(ObjectField field, Object value, Object... attributes) throws IOException {
        writeObjectSelect(field, value, attributes);
    }
}
