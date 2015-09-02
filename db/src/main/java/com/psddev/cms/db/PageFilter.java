package com.psddev.cms.db;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.RemoteWidgetFilter;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.view.JsonViewRenderer;
import com.psddev.cms.view.PageViewClass;
import com.psddev.cms.view.ViewOutput;
import com.psddev.cms.view.ViewRequest;
import com.psddev.cms.view.ViewRenderer;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.ApplicationFilter;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.JspBufferFilter;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PageContextFilter;
import com.psddev.dari.util.Profiler;
import com.psddev.dari.util.PullThroughCache;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;

public class PageFilter extends AbstractFilter {

    /** @deprecated No replacement. */
    @Deprecated
    public static final String WIREFRAME_PARAMETER = "_wireframe";

    private static final Logger LOGGER = LoggerFactory.getLogger(PageFilter.class);

    private static final String PARAMETER_PREFIX = "_cms.db.";

    public static final String DEBUG_PARAMETER = PARAMETER_PREFIX + "debug";

    /**
     * @deprecated No replacement.
     */
    @Deprecated
    public static final String OVERLAY_PARAMETER = PARAMETER_PREFIX + "overlay";

    public static final String PREVIEW_DATA_PARAMETER = PARAMETER_PREFIX + "previewData";
    public static final String PREVIEW_ID_PARAMETER = PARAMETER_PREFIX + "previewId";
    public static final String PREVIEW_SITE_ID_PARAMETER = "_previewSiteId";
    public static final String PREVIEW_TYPE_ID_PARAMETER = PARAMETER_PREFIX + "previewTypeId";
    public static final String PREVIEW_OBJECT_PARAMETER = "_previewObject";

    private static final String ATTRIBUTE_PREFIX = PageFilter.class.getName();
    private static final String FIXED_PATH_ATTRIBUTE = ATTRIBUTE_PREFIX + ".fixedPath";
    private static final String NEW_REQUEST_ATTRIBUTE = ATTRIBUTE_PREFIX + ".newRequest";
    private static final String PATH_ATTRIBUTE = ATTRIBUTE_PREFIX + ".path";
    private static final String PATH_MATCHES_ATTRIBUTE = ATTRIBUTE_PREFIX + ".matches";
    private static final String PREVIEW_ATTRIBUTE = ".preview";
    private static final String PERSISTENT_PREVIEW_ATTRIBUTE = ".persistentPreview";

    public static final String ABORTED_ATTRIBUTE = ATTRIBUTE_PREFIX + ".aborted";
    public static final String CURRENT_SECTION_ATTRIBUTE = ATTRIBUTE_PREFIX + ".currentSection";
    public static final String MAIN_OBJECT_ATTRIBUTE = ATTRIBUTE_PREFIX + ".mainObject";
    public static final String MAIN_OBJECT_CHECKED_ATTRIBUTE = ATTRIBUTE_PREFIX + ".mainObjectChecked";
    private static final String OBJECTS_ATTRIBUTE = ATTRIBUTE_PREFIX + ".objects";
    public static final String PAGE_ATTRIBUTE = ATTRIBUTE_PREFIX + ".page";
    public static final String PAGE_CHECKED_ATTRIBUTE = ATTRIBUTE_PREFIX + ".pageChecked";
    public static final String PARENT_SECTIONS_ATTRIBUTE = ATTRIBUTE_PREFIX + ".parentSections";
    public static final String PROFILE_ATTRIBUTE = ATTRIBUTE_PREFIX + ".profile";
    public static final String PROFILE_CHECKED_ATTRIBUTE = ATTRIBUTE_PREFIX + ".profileChecked";
    public static final String RENDERED_OBJECTS_ATTRIBUTE = ATTRIBUTE_PREFIX + ".renderedObjects";
    public static final String SITE_ATTRIBUTE = ATTRIBUTE_PREFIX + ".site";
    public static final String SITE_CHECKED_ATTRIBUTE = ATTRIBUTE_PREFIX + ".siteChecked";
    public static final String SUBSTITUTIONS_ATTRIBUTE = ATTRIBUTE_PREFIX + ".substitutions";

    public static final String MAIN_OBJECT_RENDERER_CONTEXT = "_main";
    public static final String EMBED_OBJECT_RENDERER_CONTEXT = "_embed";

    /**
     * Returns {@code true} if rendering the given {@code request} has
     * been aborted.
     */
    public static boolean isAborted(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(ABORTED_ATTRIBUTE));
    }

    /** Aborts rendering the given {@code request}. */
    public static void abort(HttpServletRequest request) {
        request.setAttribute(ABORTED_ATTRIBUTE, Boolean.TRUE);
    }

    /**
     * Returns the section currently being rendered in the given
     * {@code request}.
     */
    public static Section getCurrentSection(HttpServletRequest request) {
        return (Section) request.getAttribute(CURRENT_SECTION_ATTRIBUTE);
    }

    /**
     * Sets the section currently being rendered in the given
     * {@code request}.
     */
    protected static void setCurrentSection(HttpServletRequest request, Section section) {
        request.setAttribute(CURRENT_SECTION_ATTRIBUTE, section);
        request.setAttribute("section", section);
    }

    /**
     * Returns an unmodifiable list of all the parent sections used to render
     * the given {@code request} so far.
     */
    public static List<Section> getParentSections(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        List<Section> parents = (List<Section>) request.getAttribute(PARENT_SECTIONS_ATTRIBUTE);
        return parents != null ? Collections.unmodifiableList(parents) : Collections.<Section>emptyList();
    }

    /**
     * Adds the given {@code section} to the list of sections used to render
     * the given {@code request} so far.
     */
    protected static void addParentSection(HttpServletRequest request, Section section) {
        @SuppressWarnings("unchecked")
        List<Section> parents = (List<Section>) request.getAttribute(PARENT_SECTIONS_ATTRIBUTE);
        if (parents == null) {
            parents = new ArrayList<Section>();
            request.setAttribute(PARENT_SECTIONS_ATTRIBUTE, parents);
        }
        parents.add(section);

        @SuppressWarnings("unchecked")
        Map<String, Boolean> isInside = (Map<String, Boolean>) request.getAttribute("inside");
        if (isInside == null) {
            isInside = new HashMap<String, Boolean>();
            request.setAttribute("inside", isInside);
        }
        isInside.put(section.getDisplayName(), Boolean.TRUE);
        isInside.put(section.getInternalName(), Boolean.TRUE);
    }

    /**
     * Removes the last parent section used to render the given
     * {@code request} so far.
     */
    @SuppressWarnings("unchecked")
    protected static void removeLastParentSection(HttpServletRequest request) {
        List<Section> parents = (List<Section>) request.getAttribute(PARENT_SECTIONS_ATTRIBUTE);
        Section section = parents.remove(parents.size() - 1);
        ((Map<String, Boolean>) request.getAttribute("inside")).remove(section.getDisplayName());
        ((Map<String, Boolean>) request.getAttribute("inside")).remove(section.getInternalName());
    }

    /**
     * Returns a modifiable set of all the objects used to render the given
     * {@code request}.
     */
    public static Set<Object> getRenderedObjects(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Set<Object> rendered = (Set<Object>) request.getAttribute(RENDERED_OBJECTS_ATTRIBUTE);
        if (rendered == null) {
            rendered = new LinkedHashSet<Object>();
            request.setAttribute(RENDERED_OBJECTS_ATTRIBUTE, rendered);
        }
        return rendered;
    }

    /**
     * Returns the map of object substitutions for the given
     * {@code request}.
     */
    public static Map<UUID, Object> getSubstitutions(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<UUID, Object> substitutions = (Map<UUID, Object>) request.getAttribute(SUBSTITUTIONS_ATTRIBUTE);
        if (substitutions == null) {
            substitutions = new HashMap<UUID, Object>();
            request.setAttribute(SUBSTITUTIONS_ATTRIBUTE, substitutions);
        }
        return substitutions;
    }

    // --- AbstractFilter support ---

    @Override
    public Iterable<Class<? extends Filter>> dependencies() {
        List<Class<? extends Filter>> dependencies = new ArrayList<Class<? extends Filter>>();
        dependencies.add(FrameFilter.class);
        dependencies.add(ApplicationFilter.class);
        dependencies.add(RemoteWidgetFilter.class);
        dependencies.add(AuthenticationFilter.class);
        dependencies.add(com.psddev.cms.tool.ScheduleFilter.class);
        dependencies.add(com.psddev.dari.util.FormFilter.class);
        dependencies.add(com.psddev.dari.util.FrameFilter.class);
        dependencies.add(com.psddev.dari.util.RoutingFilter.class);
        dependencies.add(FieldAccessFilter.class);
        return dependencies;
    }

    private static boolean redirectIfFixedPath(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = (String) request.getAttribute(FIXED_PATH_ATTRIBUTE);
        if (path == null) {
            return false;
        } else {
            String queryString = request.getQueryString();
            if (queryString != null) {
                path += "?" + queryString;
            }
            JspUtils.redirectPermanently(request, response, path);
            return true;
        }
    }

    @Override
    protected void doError(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        doForward(request, response, chain);
    }

    @Override
    protected void doForward(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        request.removeAttribute(MAIN_OBJECT_CHECKED_ATTRIBUTE);
        request.removeAttribute(PAGE_CHECKED_ATTRIBUTE);
        request.removeAttribute(PROFILE_CHECKED_ATTRIBUTE);
        request.removeAttribute(SITE_CHECKED_ATTRIBUTE);

        doRequest(request, response, chain);
    }

    @Override
    protected void doInclude(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws Exception {

        if (Static.isInlineEditingAllContents(request)) {
            response = new LazyWriterResponse(request, response);
        }

        try {
            super.doInclude(request, response, chain);
        } finally {
            if (response instanceof LazyWriterResponse) {
                ((LazyWriterResponse) response).getLazyWriter().writePending();
            }
        }
    }

    @Override
    protected void doRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        if (request.getMethod().equalsIgnoreCase("HEAD")
                && ObjectUtils.to(boolean.class, request.getHeader("Brightspot-Main-Object-Id-Query"))) {
            Object mainObject = Static.getMainObject(request);

            if (mainObject != null) {
                response.setHeader("Brightspot-Main-Object-Id", State.getInstance(mainObject).getId().toString());
            }

            return;
        }

        if (Static.isInlineEditingAllContents(request)) {
            try {
                JspBufferFilter.Static.overrideBuffer(0);
                doRequestForReal(request, response, chain);

            } finally {
                JspBufferFilter.Static.restoreBuffer();
            }

        } else {
            doRequestForReal(request, response, chain);
        }
    }

    @SuppressWarnings("deprecation")
    private void doRequestForReal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        Profile profile = Static.getProfile(request);
        Variation.Static.applyAll(TypeDefinition.getInstance(Record.class).newInstance(), profile);

        if (redirectIfFixedPath(request, response)) {
            return;
        }

        ToolUser user = AuthenticationFilter.Static.getInsecureToolUser(request);
        request.setAttribute("toolUser", user);

        VaryingDatabase varying = new VaryingDatabase();
        varying.setDelegate(Database.Static.getDefault());
        varying.setRequest(request);
        varying.setProfile(profile);
        Database.Static.overrideDefault(varying);

        Writer writer = null;

        try {
            String servletPath = request.getServletPath();
            String externalUrl = Directory.extractExternalUrl(servletPath);

            if (externalUrl != null) {
                response.sendRedirect(externalUrl);
                return;
            }

            // Serve a special robots.txt file for non-production.
            if (servletPath.equals("/robots.txt") && !Settings.isProduction()) {
                response.setContentType("text/plain");
                writer = response.getWriter();
                writer.write("User-agent: *\n");
                writer.write("Disallow: /\n");
                return;

            // Render a single section.
            } else if (servletPath.startsWith("/_render")) {
                UUID sectionId = ObjectUtils.to(UUID.class, request.getParameter("_sectionId"));
                Section section = Query.findById(Section.class, sectionId);
                if (section != null) {
                    writeSection(request, response, response.getWriter(), section);
                }
                return;

            // Strip the special directory suffix.
            } else if (servletPath.endsWith("/index")) {
                JspUtils.redirectPermanently(request, response, servletPath.substring(0, servletPath.length() - 5));
                return;
            }

            // Global prefix?
            String prefix = Settings.get(String.class, "cms/db/directoryPrefix");
            if (!ObjectUtils.isBlank(prefix)) {
                Static.setPath(request, StringUtils.ensureEnd(prefix, "/") + servletPath);
            }

            Site site = Static.getSite(request);
            if (redirectIfFixedPath(request, response)) {
                return;
            }

            Object mainObject = Static.getMainObject(request);
            if (redirectIfFixedPath(request, response)) {
                return;
            } else {
                HttpServletRequest newRequest = (HttpServletRequest) request.getAttribute(NEW_REQUEST_ATTRIBUTE);
                if (newRequest != null) {
                    request = newRequest;
                }
            }

            // Not handled by the CMS.
            if (mainObject == null) {
                chain.doFilter(request, response);
                return;
            }

            // If mainObject has a redirect path AND a permalink and the
            // current request is the redirect path, then redirect to the
            // permalink.
            Directory.Path redirectPath = null;
            boolean isRedirect = false;

            for (Directory.Path p : State.getInstance(mainObject).as(Directory.Data.class).getPaths()) {

                if (p.getType() == Directory.PathType.REDIRECT
                        && ObjectUtils.equals(p.getSite(), site)) {
                    String path = p.getPath();
                    String requestPath = Static.getPath(request);

                    if (requestPath.equalsIgnoreCase(path)) {
                        isRedirect = true;

                    } else {
                        // handle wildcards
                        int wildcardIndex = path.indexOf("/*");

                        if (wildcardIndex > -1) {
                            String purePath = path.substring(0, wildcardIndex);

                            if (requestPath.length() >= purePath.length()) {
                                String requestPurePath = requestPath.substring(0, wildcardIndex);

                                if (requestPurePath.equalsIgnoreCase(purePath)) {
                                    String requestPathLeftover = requestPath.substring(wildcardIndex, requestPath.length());

                                    if (requestPathLeftover.isEmpty()) {
                                        isRedirect = true;

                                    } else {
                                        String wildcard = path.substring(wildcardIndex, path.length());

                                        if (wildcard.equals("/**")
                                                || (wildcard.equals("/*") && requestPathLeftover.split("/").length < 3)) {
                                            isRedirect = true;
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else if (p.getType() == Directory.PathType.PERMALINK
                        && ObjectUtils.equals(p.getSite(), site)) {
                    redirectPath = p;
                }
            }

            if (isRedirect && redirectPath != null) {
                String rp = StringUtils.removeEnd(redirectPath.getPath(), "*");

                JspUtils.redirectPermanently(request, response, site != null
                        ? site.getPrimaryUrl() + rp
                        : rp);
                return;
            }

            Static.pushObject(request, mainObject);

            if (Static.isPreview(request) || user != null) {
                response.setHeader("Cache-Control", "private, no-cache");
                response.setHeader("Brightspot-Cache", "none");
            }

            final State mainState = State.getInstance(mainObject);

            // Fake the request path in preview mode in case the servlets
            // depend on it.
            if (Static.isPreview(request)) {
                String previewPath = request.getParameter("_previewPath");

                if (!ObjectUtils.isBlank(previewPath)) {
                    int colonAt = previewPath.indexOf(':');

                    if (colonAt > -1) {
                        Site previewSite = Query
                                .from(Site.class)
                                .where("_id = ?", ObjectUtils.to(UUID.class, previewPath.substring(0, colonAt)))
                                .first();

                        if (previewSite != null) {
                            Static.setSite(request, previewSite);
                        }

                        previewPath = previewPath.substring(colonAt + 1);
                    }

                    final String finalPreviewPath = previewPath;

                    request = new HttpServletRequestWrapper(request) {

                        @Override
                        public String getRequestURI() {
                            return getContextPath() + getServletPath();
                        }

                        @Override
                        public StringBuffer getRequestURL() {
                            return new StringBuffer(getRequestURI());
                        }

                        @Override
                        public String getServletPath() {
                            return finalPreviewPath;
                        }
                    };
                }
            }

            if (!Static.isPreview(request)
                    && !mainState.isVisible()) {
                SCHEDULED: {
                    if (user != null) {
                        Schedule currentSchedule = user.getCurrentSchedule();

                        if (currentSchedule != null
                                && Query.from(Draft.class).where("schedule = ? and objectId = ?", currentSchedule, mainState.getId()).first() != null) {
                            break SCHEDULED;
                        }

                    } else {
                        if (Settings.isProduction()) {
                            chain.doFilter(request, response);
                            return;

                        } else {
                            throw new IllegalStateException(String.format(
                                    "[%s] isn't visible!", mainState.getId()));
                        }
                    }
                }
            }

            // If showing an invisible item, make sure all nested invisible
            // items show up too.
            if (!mainState.isVisible() || Static.isPreview(request)) {
                mainState.setResolveInvisible(true);
            }

            ObjectType mainType = mainState.getType();
            Page page = Static.getPage(request);

            if (page == null
                    && mainType != null
                    && !ObjectUtils.isBlank(mainType.as(Renderer.TypeModification.class).getPath())) {
                page = Application.Static.getInstance(CmsTool.class).getModulePreviewTemplate();
            }

            // SEO and <head>.
            Map<String, Object> seo = new HashMap<String, Object>();
            Seo.ObjectModification seoData = mainState.as(Seo.ObjectModification.class);
            String seoTitle = seoData.findTitle();
            String seoDescription = seoData.findDescription();
            String seoRobots = seoData.findRobotsString();
            Set<String> seoKeywords = seoData.findKeywords();
            String seoKeywordsString = null;

            request.setAttribute("seo", seo);
            seo.put("title", seoTitle);
            seo.put("description", seoDescription);
            seo.put("robots", seoRobots);

            if (seoKeywords != null) {
                seoKeywordsString = seoKeywords.toString();

                seo.put("keywords", seoKeywords);
                seo.put("keywordsString", seoKeywordsString);
            }

            PageStage stage = new PageStage(getServletContext(), request);

            request.setAttribute("stage", stage);
            stage.setMetaProperty("og:type", mainType.as(Seo.TypeModification.class).getOpenGraphType());

            if (mainType != null
                    && !ObjectUtils.isBlank(mainType.as(Renderer.TypeModification.class).getEmbedPath())) {
                stage.findOrCreateHeadElement("link",
                        "rel", "alternate",
                        "type", "application/json+oembed")
                        .getAttributes()
                        .put("href", JspUtils.getAbsoluteUrl(request, "",
                                "_embed", true,
                                "_format", "oembed"));
            }

            stage.setTitle(seoTitle);
            stage.setDescription(seoDescription);
            stage.setMetaName("robots", seoRobots);
            stage.setMetaName("keywords", seoKeywordsString);
            stage.update(mainObject);

            // Try to set the right content type based on the extension.
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(servletPath);
            response.setContentType((ObjectUtils.isBlank(contentType) ? "text/html" : contentType) + ";charset=UTF-8");

            // Disable Webkit's XSS Auditor since it often interferes with
            // how preview works.
            if (Static.isPreview(request)) {
                response.setHeader("X-XSS-Protection", "0");
            }

            // Render the page.
            if (Static.isInlineEditingAllContents(request)) {
                response = new LazyWriterResponse(request, response);
            }

            writer = new HtmlWriter(response.getWriter());

            ((HtmlWriter) writer).putAllStandardDefaults();

            request.setAttribute("sections", new PullThroughCache<String, Section>() {
                @Override
                protected Section produce(String name) {
                    return Query.from(Section.class).where("internalName = ?", name).first();
                }
            });

            beginPage(request, response, writer, page);

            if (!response.isCommitted()) {
                request.getSession();
            }

            String context = request.getParameter("_context");
            boolean contextNotBlank = !ObjectUtils.isBlank(context);
            boolean embed = ObjectUtils.coalesce(
                    ObjectUtils.to(Boolean.class, request.getParameter("_embed")),
                    contextNotBlank);

            String layoutPath = findLayoutPath(mainObject, embed);

            if (page != null && ObjectUtils.isBlank(layoutPath)) {
                layoutPath = findLayoutPath(page, embed);

                if (!embed && ObjectUtils.isBlank(layoutPath)) {
                    layoutPath = page.getRendererPath();
                }
            }

            if (ObjectUtils.isBlank(layoutPath)
                    && Static.isPreview(request)) {
                layoutPath = findLayoutPath(mainObject, true);
            }

            String typePath = mainType.as(Renderer.TypeModification.class).getPath();
            boolean rendered = false;

            try {
                ContextTag.Static.pushContext(request, contextNotBlank ? context
                        : (embed ? EMBED_OBJECT_RENDERER_CONTEXT : MAIN_OBJECT_RENDERER_CONTEXT));

                if (!ObjectUtils.isBlank(layoutPath)) {
                    rendered = true;
                    JspUtils.include(request, response, writer, StringUtils.ensureStart(layoutPath, "/"));

                } else if (page != null) {
                    Page.Layout layout = page.getLayout();

                    if (layout != null) {
                        rendered = true;
                        renderSection(request, response, writer, layout.getOutermostSection());
                    }
                }

                if (!rendered && !embed && !ObjectUtils.isBlank(typePath)) {
                    rendered = true;
                    JspUtils.include(request, response, writer, StringUtils.ensureStart(typePath, "/"));
                }

                if (!rendered && mainObject instanceof Renderer) {
                    rendered = true;
                    ((Renderer) mainObject).renderObject(request, response, (HtmlWriter) writer);
                }

                if (!rendered && tryRenderView(request, response, writer, mainObject)) {
                    rendered = true;
                }

            } finally {
                ContextTag.Static.popContext(request);
            }

            if (!rendered) {
                if (Settings.isProduction()) {
                    chain.doFilter(request, response);
                    return;

                } else {
                    StringBuilder message = new StringBuilder();

                    if (embed) {
                        message.append("@Renderer.EmbedPath required on [");
                        message.append(mainObject.getClass().getName());
                        message.append("] to render it in [");
                        message.append(ObjectUtils.isBlank(context) ? "_embed" : context);
                        message.append("] context");

                    } else {
                        message.append("@Renderer.Path or @Renderer.LayoutPath required on [");
                        message.append(mainObject.getClass().getName());
                        message.append("] to render it");
                    }

                    message.append(" (Object: [");
                    message.append(mainState.getLabel());
                    message.append("], ID: [");
                    message.append(mainState.getId().toString().replaceAll("-", ""));
                    message.append("])!");

                    throw new IllegalStateException(message.toString());
                }
            }

            endPage(request, response, writer, page);

            if (Static.isInlineEditingAllContents(request)) {
                LazyWriterResponse lazyResponse = (LazyWriterResponse) response;
                Map<String, String> map = new HashMap<String, String>();
                State state = State.getInstance(mainObject);
                StringBuilder marker = new StringBuilder();

                map.put("id", state.getId().toString());
                map.put("label", state.getLabel());
                map.put("typeLabel", state.getType().getLabel());

                marker.append("<span class=\"cms-mainObject\" style=\"display: none;\" data-object=\"");
                marker.append(StringUtils.escapeHtml(ObjectUtils.toJson(map)));
                marker.append("\"></span>");

                lazyResponse.getLazyWriter().writeLazily(marker.toString());
            }

        } finally {
            Database.Static.restoreDefault();

            if (response instanceof LazyWriterResponse) {
                ((LazyWriterResponse) response).getLazyWriter().writePending();
            }
        }

        if (Settings.isDebug()
                || (Static.isPreview(request)
                && !Boolean.TRUE.equals(request.getAttribute(PERSISTENT_PREVIEW_ATTRIBUTE)))) {
            return;
        }

        String contentType = response.getContentType();

        if (contentType == null
                || !StringUtils.ensureEnd(contentType, ";").startsWith("text/html;")) {
            return;
        }

        Object mainObject = PageFilter.Static.getMainObject(request);

        if (mainObject != null && user != null) {
            if (!JspUtils.isError(request)) {
                user.saveAction(request, mainObject);
            }

            @SuppressWarnings("all")
            ToolPageContext page = new ToolPageContext(getServletContext(), request, response);
            State mainState = State.getInstance(mainObject);

            page.setDelegate(writer instanceof HtmlWriter ? (HtmlWriter) writer : new HtmlWriter(writer));

            page.writeStart("style", "type", "text/css");
                page.writeCss(".bsp-inlineEditorMain",
                        "background", "rgba(32, 52, 63, 0.8) !important",
                        "color", "white !important",
                        "font-family", "'Helvetica Neue', sans-serif !important",
                        "font-size", "13px !important",
                        "font-weight", "normal !important",
                        "left", "0 !important",
                        "line-height", "21px !important",
                        "list-style", "none !important",
                        "margin", "0 !important",
                        "padding", "0 !important",
                        "position", "fixed !important",
                        "top", "0 !important",
                        "z-index", "1000001 !important");

                page.writeCss(".bsp-inlineEditorMain a",
                        "color", "#54d1f0 !important",
                        "display", "block !important",
                        "float", "left !important",
                        "max-width", "250px",
                        "overflow", "hidden",
                        "padding", "5px !important",
                        "text-overflow", "ellipsis",
                        "white-space", "nowrap");

                page.writeCss(".bsp-inlineEditorMain a:hover",
                        "background", "#54d1f0 !important",
                        "color", "black !important");

                page.writeCss(".bsp-inlineEditorMain .bsp-inlineEditorMain_logo",
                        "padding", "8px 10px !important");

                page.writeCss(".bsp-inlineEditorMain .bsp-inlineEditorMain_logo:hover",
                        "background", "transparent !important");

                page.writeCss(".bsp-inlineEditorMain .bsp-inlineEditorMain_logo img",
                        "display", "block !important");

                page.writeCss(".bsp-inlineEditorMain .bsp-inlineEditorMain_remove",
                        "color", "#ff0e40 !important",
                        "font-size", "21px");
            page.writeEnd();

            page.writeStart("div", "class", "bsp-inlineEditorMain");
                page.writeStart("a",
                        "class", "bsp-inlineEditorMain_logo",
                        "target", "_blank",
                        "href", page.fullyQualifiedToolUrl(CmsTool.class, "/"));
                    page.writeElement("img",
                            "src", page.cmsUrl("/style/brightspot.png"),
                            "alt", "Brightspot",
                            "width", 104,
                            "height", 14);
                page.writeEnd();

                Schedule currentSchedule = user.getCurrentSchedule();

                if (currentSchedule != null) {
                    page.writeStart("a",
                            "target", "_blank",
                            "href", page.fullyQualifiedToolUrl(CmsTool.class, "/scheduleEdit", "id", currentSchedule.getId()));
                        page.writeHtml("Current Schedule: ");
                        page.writeObjectLabel(currentSchedule);
                    page.writeEnd();
                }

                page.writeStart("a",
                        "target", "_blank",
                        "href", page.fullyQualifiedToolUrl(CmsTool.class, "/content/edit.jsp", "id", State.getInstance(mainObject).getId()));
                    page.writeHtml("Edit ");
                    page.writeTypeObjectLabel(mainObject);
                page.writeEnd();

                if (Boolean.TRUE.equals(request.getAttribute(PERSISTENT_PREVIEW_ATTRIBUTE))) {
                    page.writeStart("a",
                            "target", "_blank",
                            "href", page.url("", "_clearPreview", true));
                        page.writeHtml("(Previewing - View Live Instead)");
                    page.writeEnd();
                }

                page.writeStart("a",
                        "class", "bsp-inlineEditorMain_remove",
                        "href", "#",
                        "onclick",
                                "var main = this.parentNode,"
                                        + "contents = this.ownerDocument.getElementById('bsp-inlineEditorContents');"

                                + "main.parentNode.removeChild(main);"

                                + "if (contents) {"
                                    + "contents.parentNode.removeChild(contents);"
                                + "}"

                                + "return false;");
                    page.writeHtml("\u00d7");
                page.writeEnd();
            page.writeEnd();

            if (user.getInlineEditing() != ToolUser.InlineEditing.DISABLED) {
                page.writeStart("iframe",
                        "class", "cms-inlineEditor",
                        "id", "bsp-inlineEditorContents",
                        "onload", "this.style.visibility = 'visible';",
                        "scrolling", "no",
                        "src", page.cmsUrl("/inlineEditor", "id", mainState.getId()),
                        "style", page.cssString(
                                "border", "none",
                                "height", 0,
                                "left", 0,
                                "margin", 0,
                                "pointer-events", "none",
                                "position", "absolute",
                                "top", 0,
                                "visibility", "hidden",
                                "width", "100%",
                                "z-index", 1000000));
                page.writeEnd();
            }
        }

        if (response instanceof LazyWriterResponse) {
            ((LazyWriterResponse) response).getLazyWriter().writePending();
        }
    }

    private String findLayoutPath(Object object, boolean embed) {
        ObjectType type = State.getInstance(object).getType();

        if (type != null) {
            Renderer.TypeModification rendererData = type.as(Renderer.TypeModification.class);

            return embed ? rendererData.getEmbedPath() : rendererData.getLayoutPath();

        } else {
            return null;
        }
    }

    /** Renders the beginning of the given {@code page}. */
    protected static void beginPage(
            HttpServletRequest request,
            HttpServletResponse response,
            Writer writer,
            Page page)
            throws IOException, ServletException {
    }

    /** Renders the end of the given {@code page}. */
    protected static void endPage(
            HttpServletRequest request,
            HttpServletResponse response,
            Writer writer,
            Page page)
            throws IOException, ServletException {
    }

    /** Renders the given {@code section}. */
    public static void renderSection(
            HttpServletRequest request,
            HttpServletResponse response,
            Writer writer,
            Section section)
            throws IOException, ServletException {

        if (isAborted(request)) {
            return;
        }

        try {
            Profiler.Static.startThreadEvent("Render", section);

            if (section != null) {
                Object substitution = getSubstitutions(request).get(section.getId());
                if (substitution != null) {
                    if (substitution instanceof Section) {
                        section = (Section) substitution;
                    } else {
                        ContentSection substitutionSection = new ContentSection();
                        substitutionSection.setContent(substitution);
                        section = substitutionSection;
                    }
                }
            }

            long cacheDuration = section != null ? section.getCacheDuration() : 0;
            if (cacheDuration > 0) {
                SectionCacheKey key = new SectionCacheKey();
                key.sectionId = section.getId();
                key.cacheDuration = cacheDuration;
                key.request = request;
                key.response = response;
                key.section = section;
                writer.write(SECTION_CACHE.get(key));

            } else {
                Section previousSection = getCurrentSection(request);
                try {
                    setCurrentSection(request, section);
                    writeSection(request, response, writer, section);
                } finally {
                    setCurrentSection(request, previousSection);
                }
            }

        } finally {
            Profiler.Static.stopThreadEvent();
        }
    }

    /**
     * Processes and writes the given {@code section} to the given
     * {@code writer}.
     */
    @SuppressWarnings("all")
    private static void writeSection(
            HttpServletRequest request,
            HttpServletResponse response,
            Writer writer,
            Section section)
            throws IOException, ServletException {

        // Container section - begin, child sections, then end.
        if (section instanceof ContainerSection) {
            ContainerSection container = (ContainerSection) section;
            List<Section> children = container.getChildren();

            try {
                addParentSection(request, container);
                beginContainer(request, response, writer, container);

                for (Section child : children) {
                    renderSection(request, response, writer, child);
                    if (isAborted(request)) {
                        return;
                    }
                }

                endContainer(request, response, writer, container);

            } finally {
                removeLastParentSection(request);
            }

        // Script section may be associated with an object.
        } else if (section instanceof ScriptSection) {
            Object object;
            if (section instanceof MainSection) {
                object = getMainObject(request);
            } else if (section instanceof ContentSection) {
                object = ((ContentSection) section).getContent();
            } else {
                object = null;
            }
            renderObjectWithSection(request, response, writer, object, (ScriptSection) section);
        }
    }

    /**
     * Key for {@link #SECTION_CACHE} that contains extra information
     * like the request object.
     */
    private static class SectionCacheKey {

        public UUID sectionId;
        public long cacheDuration;
        public HttpServletRequest request;
        public HttpServletResponse response;
        public Section section;

        @Override
        public boolean equals(Object other) {
            return this == other
                    || (other instanceof SectionCacheKey
                    && sectionId.equals(((SectionCacheKey) other).sectionId));
        }

        @Override
        public int hashCode() {
            return sectionId.hashCode();
        }
    }

    /** Cache that contains output of each sections. */
    private static final PullThroughCache<SectionCacheKey, String>
            SECTION_CACHE = new PullThroughCache<SectionCacheKey, String>() {

        @Override
        protected boolean isExpired(SectionCacheKey key, Date lastProduced) {
            return System.currentTimeMillis() - lastProduced.getTime() > key.cacheDuration;
        }

        @Override
        protected String produce(SectionCacheKey key) throws IOException, ServletException {
            try {
                StringWriter writer = new StringWriter();
                writeSection(key.request, key.response, writer, key.section);
                return writer.toString();

            } finally {
                key.request = null;
                key.response = null;
                key.section = null;
            }
        }
    };

    private static boolean tryRenderView(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Writer writer,
                                        Object object)
                                        throws IOException, ServletException {

        ViewRequest viewRequest = new ServletViewRequest(request);

        PageViewClass annotation = object.getClass().getAnnotation(PageViewClass.class);

        Class<?> layoutViewClass = annotation != null ? annotation.value() : null;
        if (layoutViewClass != null) {

            Object view = viewRequest.createView(layoutViewClass, object);
            if (view != null) {

                ViewRenderer renderer;
                if ("json".equals(request.getParameter("_renderer"))) {

                    JsonViewRenderer jsonViewRenderer = new JsonViewRenderer();
                    jsonViewRenderer.setIndented(!Settings.isProduction());
                    jsonViewRenderer.setIncludeClassNames(!Settings.isProduction());
                    renderer = jsonViewRenderer;

                    response.setContentType("application/json");

                } else {
                    renderer = ViewRenderer.createRenderer(view);
                }

                if (renderer != null) {

                    ViewOutput result = renderer.render(view);
                    String output = result.get();
                    if (output != null) {
                        writer.write(output);
                    }

                } else {
                    LOGGER.warn("Could not create renderer for view of type ["
                            + view.getClass().getName() + "]!");
                }

            } else {
                LOGGER.warn("Could not create view of type ["
                        + layoutViewClass.getName() + "] for object of type ["
                        + object.getClass() + "]!");
            }

            return true;

        } else {
            return false;
        }
    }

    /** Renders the given {@code object}. */
    public static void renderObject(
            HttpServletRequest request,
            HttpServletResponse response,
            Writer writer,
            Object object)
            throws IOException, ServletException {

        if (!tryRenderView(request, response, writer, object)) {

            if (object instanceof Section) {
                renderSection(request, response, writer, (Section) object);

            } else {
                renderObjectWithSection(request, response, writer, object, null);
            }
        }
    }

    /** Renders the given {@code object} using the given {@code section}. */
    @SuppressWarnings("all")
    private static void renderObjectWithSection(
            HttpServletRequest request,
            HttpServletResponse response,
            Writer writer,
            Object object,
            ScriptSection section)
            throws IOException, ServletException {

        String engine;
        String script;
        if (section != null) {
            engine = section.getEngine();
            script = section.getRendererPath();
        } else {
            engine = null;
            script = null;
        }

        if (object != null) {
            Object substitution = getSubstitutions(request).get(State.getInstance(object).getId());
            if (substitution != null) {
                object = substitution;
            }

            getRenderedObjects(request).add(object);

            // Engine not specified on section so fall back to the one
            // specified in the object type definition.
            if (ObjectUtils.isBlank(script)) {
                ObjectType type = State.getInstance(object).getType();
                if (type != null) {
                    Renderer.TypeModification typeRenderer = type.as(Renderer.TypeModification.class);
                    engine = typeRenderer.getEngine();
                    script = typeRenderer.findContextualPath(request);
                }
            }
        }

        LazyWriter lazyWriter;

        if (Static.isInlineEditingAllContents(request)) {
            lazyWriter = new LazyWriter(request, writer);
            writer = lazyWriter;

        } else {
            lazyWriter = null;
        }

        try {
            if (object != null) {
                Static.pushObject(request, object);
            }

            if (lazyWriter != null) {
                Map<String, String> map = new HashMap<String, String>();
                Object concrete = Static.peekConcreteObject(request);
                StringBuilder marker = new StringBuilder();

                if (section != null) {
                    map.put("sectionName", section.getName());
                    map.put("sectionId", section.getId().toString());
                }

                if (concrete != null) {
                    State state = State.getInstance(concrete);
                    ObjectType stateType = state.getType();

                    map.put("id", state.getId().toString());

                    if (stateType != null) {
                        map.put("typeLabel", stateType.getLabel());
                    }

                    try {
                        map.put("label", state.getLabel());

                    } catch (RuntimeException error) {
                        // Not a big deal if label can't be retrieved.
                    }
                }

                marker.append("<span class=\"cms-objectBegin\" style=\"display: none;\" data-object=\"");
                marker.append(StringUtils.escapeHtml(ObjectUtils.toJson(map)));
                marker.append("\"></span>");
                lazyWriter.writeLazily(marker.toString());
            }

            if (ObjectUtils.isBlank(script) && object instanceof Renderer) {
                ((Renderer) object).renderObject(
                        request,
                        response,
                        writer instanceof HtmlWriter ? (HtmlWriter) writer : new HtmlWriter(writer));

            } else {
                renderScript(request, response, writer, engine, script);
            }

        } finally {
            if (object != null) {
                Static.popObject(request);
            }

            if (lazyWriter != null) {
                lazyWriter.writeLazily("<span class=\"cms-objectEnd\" style=\"display: none;\"></span>");
                lazyWriter.writePending();
            }
        }
    }

    // Renders the given script using the given engine.
    private static void renderScript(
            HttpServletRequest request,
            HttpServletResponse response,
            Writer writer,
            String engine,
            String script)
            throws IOException, ServletException {

        try {
            if ("RawText".equals(engine)) {
                writer.write(script);
                return;

            } else if (!ObjectUtils.isBlank(script)) {
                JspUtils.include(request, response, writer, StringUtils.ensureStart(script, "/"));
                return;
            }

        // Always catch the error so the page never looks broken
        // in production.
        } catch (Throwable ex) {
            if (Settings.isProduction()) {
                LOGGER.warn(String.format("Can't render [%s]!", script), ex);

            } else if (ex instanceof IOException) {
                throw (IOException) ex;
            } else if (ex instanceof ServletException) {
                throw (ServletException) ex;
            } else if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else if (ex instanceof Error) {
                throw (Error) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    /** {@link PageFilter} utility methods. */
    public static final class Static {

        private Static() {
        }

        /** Returns the path used to find the main object. */
        public static String getPath(HttpServletRequest request) {
            getSite(request);
            String path = (String) request.getAttribute(PATH_ATTRIBUTE);
            return path != null ? path : request.getServletPath();
        }

        /** Sets the path used to find the main object. */
        public static void setPath(HttpServletRequest request, String path) {
            request.setAttribute(PATH_ATTRIBUTE, path);
        }

        private static void fixPath(HttpServletRequest request, String path) {
            request.setAttribute(FIXED_PATH_ATTRIBUTE, path);
        }

        /** Returns the site associated with the given {@code request}. */
        public static Site getSite(HttpServletRequest request) {
            if (Boolean.TRUE.equals(request.getAttribute(SITE_CHECKED_ATTRIBUTE))) {
                return (Site) request.getAttribute(SITE_ATTRIBUTE);
            }

            request.setAttribute(SITE_CHECKED_ATTRIBUTE, Boolean.TRUE);

            Site site = null;
            String servletPath = request.getServletPath();
            String absoluteUrl = JspUtils.getAbsoluteUrl(request, servletPath);
            Map.Entry<String, Site> entry = Site.Static.findByUrl(absoluteUrl);

            if (entry != null) {
                String path = absoluteUrl.substring(entry.getKey().length() - 1);
                if (path.length() == 0) {
                    fixPath(request, servletPath + "/");
                }

                site = Query.from(Site.class).where("_id = ?", entry.getValue()).first();
                setSite(request, site);
                setPath(request, path);
            }

            return site;
        }

        /** Sets the site associated with the given {@code request}. */
        public static void setSite(HttpServletRequest request, Site site) {
            request.setAttribute(SITE_CHECKED_ATTRIBUTE, Boolean.TRUE);
            request.setAttribute(SITE_ATTRIBUTE, site);
            request.setAttribute("site", site);
        }

        /**
         * Returns {@code true} if the given {@code request} is for a preview.
         *
         * @param request Can't be {@code null}.
         */
        public static boolean isPreview(HttpServletRequest request) {
            return Boolean.TRUE.equals(request.getAttribute(PREVIEW_ATTRIBUTE));
        }

        /** Returns the main object associated with the given {@code request}. */
        public static Object getMainObject(HttpServletRequest request) {
            if (Boolean.TRUE.equals(request.getAttribute(MAIN_OBJECT_CHECKED_ATTRIBUTE))) {
                return request.getAttribute(MAIN_OBJECT_ATTRIBUTE);
            }

            VaryingDatabase varying = new VaryingDatabase();
            varying.setDelegate(Database.Static.getDefault());
            varying.setRequest(request);
            varying.setProfile(getProfile(request));
            Database.Static.overrideDefault(varying);

            try {
                request.setAttribute(MAIN_OBJECT_CHECKED_ATTRIBUTE, Boolean.TRUE);

                Object mainObject = null;
                String servletPath = request.getServletPath();
                String path = getPath(request);
                Site site = getSite(request);

                // On preview request, manually create the main object based on
                // the post data.
                if (path.startsWith("/_preview")) {
                    request.setAttribute(PREVIEW_ATTRIBUTE, Boolean.TRUE);

                    UUID previewId = ObjectUtils.to(UUID.class, request.getParameter(PREVIEW_ID_PARAMETER));

                    if (previewId == null) {
                        String previewIdString = path.substring(10);
                        int slashAt = previewIdString.indexOf('/');

                        if (slashAt > -1) {
                            previewIdString = previewIdString.substring(0, slashAt);
                        }

                        previewId = ObjectUtils.to(UUID.class, previewIdString);
                    }

                    if (previewId != null) {
                        Map<UUID, Object> substitutions = getSubstitutions(request);

                        if (ObjectUtils.to(Date.class, request.getParameter("_date")) == null) {
                            String[] objectStrings = request.getParameterValues(PREVIEW_OBJECT_PARAMETER);

                            if (objectStrings != null) {
                                for (String objectString : objectStrings) {
                                    if (!ObjectUtils.isBlank(objectString)) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> objectMap = (Map<String, Object>) ObjectUtils.fromJson(objectString.trim());
                                        ObjectType type = ObjectType.getInstance(ObjectUtils.to(UUID.class, objectMap.remove("_typeId")));

                                        if (type != null) {
                                            Object object = type.createObject(ObjectUtils.to(UUID.class, objectMap.remove("_id")));
                                            State objectState = State.getInstance(object);

                                            objectState.setValues(objectMap);
                                            substitutions.put(objectState.getId(), object);
                                        }
                                    }
                                }
                            }
                        }

                        UUID mainObjectId = ObjectUtils.to(UUID.class, request.getParameter("_mainObjectId"));
                        Object preview = Query
                                .fromAll()
                                .where("_id = ?", mainObjectId)
                                .first();

                        if (preview == null) {
                            preview = Query
                                    .fromAll()
                                    .where("_id = ?", previewId)
                                    .first();
                        }

                        if (preview instanceof Draft) {
                            mainObject = ((Draft) preview).recreate();

                        } else if (preview instanceof History) {
                            mainObject = ((History) preview).getObject();

                        } else if (preview instanceof Preview) {
                            Preview previewPreview = (Preview) preview;
                            mainObject = previewPreview.getObject();
                            Site previewSite = previewPreview.getSite();

                            if (previewSite != null) {
                                site = previewSite;
                                setSite(request, site);
                            }

                            AuthenticationFilter.Static.setCurrentPreview(request, PageContextFilter.Static.getResponse(), previewPreview);

                        } else if (mainObjectId != null) {
                            mainObject = preview;

                        } else {
                            mainObject = substitutions.get(previewId);

                            if (mainObject == null) {
                                mainObject = preview;
                            }
                        }
                    }

                    if (mainObject != null) {
                        Directory.Data dirData = State.getInstance(mainObject).as(Directory.Data.class);

                        if (dirData.getRawPaths().isEmpty()) {
                            dirData.addPath(null, "/_preview-" + previewId, Directory.PathType.PERMALINK);
                        }

                        Site previewSite = Query
                                .from(Site.class)
                                .where("_id = ?", request.getParameter(PREVIEW_SITE_ID_PARAMETER))
                                .first();

                        if (previewSite != null) {
                            setSite(request, previewSite);

                        } else {
                            for (Directory.Path p : dirData.getPaths()) {
                                if (Directory.PathType.PERMALINK.equals(p.getType())) {
                                    setSite(request, p.getSite());
                                    break;
                                }
                            }
                        }
                    }

                } else {
                    mainObject = Directory.Static.findByPath(site, path);

                    if (mainObject == null) {
                        mainObject = Directory.Static.findByPath(site, path + "/index");

                        // Index pages should have a trailing slash.
                        if (mainObject != null) {

                            // Except when told not to.
                            if (Query.from(CmsTool.class).first().isRemoveTrailingSlashes()) {
                                if (path.length() > 1 && path.endsWith("/")) {
                                    fixPath(request, servletPath.substring(0, servletPath.length() - 1));
                                }

                            } else if (!path.endsWith("/")) {
                                fixPath(request, servletPath + "/");
                            }
                        }

                    // Normal pages shouldn't have a trailing slash.
                    } else if (path.endsWith("/")) {
                        fixPath(request, servletPath.substring(0, servletPath.length() - 1));
                    }
                }

                // Case-insensitive path look-up.
                for (int i = 0, length = path.length(); i < length; ++ i) {
                    if (Character.isUpperCase(path.charAt(i))) {
                        String pathLc = path.toLowerCase(Locale.ENGLISH);
                        if (Directory.Static.findObject(site, pathLc) != null) {
                            fixPath(request, pathLc);
                        }
                        break;
                    }
                }

                // Special fallback names. For example, given /path/to/file,
                // the following are checked:
                //
                // - /path/to/file/*
                // - /path/to/file/**
                // - /path/to/*
                // - /path/to/**
                // - /path/**
                // - /**
                String checkPath;
                int endMarker;

                if (path.endsWith("/")) {
                    checkPath = path;
                    endMarker = 0;

                } else {
                    checkPath = path + "/";
                    endMarker = 1;
                }

                for (int i = 0; mainObject == null; ++ i) {
                    int slashAt = checkPath.lastIndexOf("/");

                    if (slashAt < 0) {
                        break;
                    } else {
                        checkPath = checkPath.substring(0, slashAt);
                    }

                    if (i <= endMarker) {
                        mainObject = Directory.Static.findObject(site, checkPath + "/*");
                    }

                    if (mainObject == null) {
                        mainObject = Directory.Static.findObject(site, checkPath + "/**");
                    }

                    if (mainObject instanceof Directory) {
                        mainObject = null;
                    }

                    if (mainObject != null) {
                        final String pathInfo = path.substring(checkPath.length());

                        if (Query.from(CmsTool.class).first().isRemoveTrailingSlashes()) {
                            if ("/".equals(pathInfo)) {
                                fixPath(request, servletPath.substring(0, servletPath.length() - 1));
                            }

                        } else if (pathInfo.length() < 1) {
                            fixPath(request, servletPath + "/");
                        }

                        request.setAttribute(NEW_REQUEST_ATTRIBUTE, new HttpServletRequestWrapper(request) {
                            @Override
                            public String getPathInfo() {
                                return pathInfo;
                            }
                        });
                    }
                }

                if (!Static.isPreview(request) && mainObject != null) {
                    Preview preview = AuthenticationFilter.Static.getCurrentPreview(request);

                    if (preview != null) {
                        State mainState = State.getInstance(mainObject);

                        if (mainState.getId().equals(preview.getObjectId())) {
                            request.setAttribute(PREVIEW_ATTRIBUTE, Boolean.TRUE);
                            request.setAttribute(PERSISTENT_PREVIEW_ATTRIBUTE, Boolean.TRUE);
                            mainState.putAll(preview.getObjectValues());
                        }
                    }
                }

                setMainObject(request, mainObject);
                return mainObject;

            } finally {
                Database.Static.restoreDefault();
            }
        }

        /** Sets the main object associated with the given {@code request}. */
        public static void setMainObject(HttpServletRequest request, Object mainObject) {
            request.setAttribute(MAIN_OBJECT_CHECKED_ATTRIBUTE, Boolean.TRUE);
            request.setAttribute(MAIN_OBJECT_ATTRIBUTE, mainObject);
            request.setAttribute("mainObject", mainObject);
            request.setAttribute("mainRecord", mainObject);
            request.setAttribute("mainContent", mainObject);
        }

        /** Returns the page used to render the given {@code request}. */
        public static Page getPage(HttpServletRequest request) {
            if (Boolean.TRUE.equals(request.getAttribute(PAGE_CHECKED_ATTRIBUTE))) {
                return (Page) request.getAttribute(PAGE_ATTRIBUTE);
            }

            request.setAttribute(PAGE_CHECKED_ATTRIBUTE, Boolean.TRUE);

            Page page = null;
            Object mainObject = getMainObject(request);

            if (mainObject instanceof Page) {
                page = (Page) mainObject;
            } else {
                page = Template.Static.findRenderable(mainObject, getSite(request));
            }

            setPage(request, page);
            return page;
        }

        /** Sets the page used to render the given {@code request}. */
        public static void setPage(HttpServletRequest request, Page page) {
            request.setAttribute(PAGE_CHECKED_ATTRIBUTE, Boolean.TRUE);
            request.setAttribute(PAGE_ATTRIBUTE, page);
            request.setAttribute("template", page);
        }

        /** Returns the profile used to process the given {@code request}. */
        public static Profile getProfile(HttpServletRequest request) {
            if (Boolean.TRUE.equals(request.getAttribute(PROFILE_CHECKED_ATTRIBUTE))) {
                return (Profile) request.getAttribute(PROFILE_ATTRIBUTE);
            }

            request.setAttribute(PROFILE_CHECKED_ATTRIBUTE, Boolean.TRUE);

            Profile profile = new Profile();
            profile.setUserAgent(request.getHeader("User-Agent"));

            setProfile(request, profile);
            return profile;
        }

        /** Sets the profile used to process the given {@code request}. */
        public static void setProfile(HttpServletRequest request, Profile profile) {
            request.setAttribute(PROFILE_CHECKED_ATTRIBUTE, Boolean.TRUE);
            request.setAttribute(PROFILE_ATTRIBUTE, profile);
            request.setAttribute("profile", profile);
        }

        /**
         * Pushes the given {@code object} to the list of objects that
         * are currently being rendered.
         */
        public static void pushObject(HttpServletRequest request, Object object) {
            ErrorUtils.errorIfNull(object, "object");

            @SuppressWarnings("unchecked")
            List<Object> objects = (List<Object>) request.getAttribute(OBJECTS_ATTRIBUTE);

            if (objects == null) {
                objects = new ArrayList<Object>();
                request.setAttribute(OBJECTS_ATTRIBUTE, objects);
            }

            objects.add(object);
            request.setAttribute("content", object);
            request.setAttribute("record", object);
            request.setAttribute("object", object);
            request.setAttribute(CURRENT_OBJECT_ATTRIBUTE, object);
        }

        /**
         * Pops the last object from the list of objects that are currently
         * being rendered.
         */
        public static Object popObject(HttpServletRequest request) {
            @SuppressWarnings("unchecked")
            List<Object> objects = (List<Object>) request.getAttribute(OBJECTS_ATTRIBUTE);

            if (objects == null || objects.isEmpty()) {
                return null;

            } else {
                Object popped = objects.remove(objects.size() - 1);
                Object object = peekObject(request);
                request.setAttribute("content", object);
                request.setAttribute("record", object);
                request.setAttribute("object", object);
                return popped;
            }
        }

        /**
         * Returns the last object from the list of objects that are currently
         * being rendered.
         */
        public static Object peekObject(HttpServletRequest request) {
            @SuppressWarnings("unchecked")
            List<Object> objects = (List<Object>) request.getAttribute(OBJECTS_ATTRIBUTE);

            if (objects == null || objects.isEmpty()) {
                return null;

            } else {
                return objects.get(objects.size() - 1);
            }
        }

        /**
         * Returns the last concrete object from the list of objects that are
         * currently being rendered.
         */
        public static Object peekConcreteObject(HttpServletRequest request) {
            @SuppressWarnings("unchecked")
            List<Object> objects = (List<Object>) request.getAttribute(OBJECTS_ATTRIBUTE);

            if (objects != null) {
                for (int i = objects.size() - 1; i >= 0; -- i) {
                    Object object = objects.get(i);

                    if (object instanceof Recordable && !State.getInstance(object).isNew()) {
                        return object;
                    }
                }
            }

            return null;
        }

        /**
         * Returns {@code true} if the tool user has requested for inline
         * editing to be fully enabled.
         *
         * @param request Can't be {@code null}.
         * @return {@code false} if a tool user isn't logged in.
         */
        public static boolean isInlineEditingAllContents(HttpServletRequest request) {
            if (Settings.isDebug()) {
                return false;

            } else {
                ToolUser user = AuthenticationFilter.Static.getInsecureToolUser(request);

                return user != null && user.getInlineEditing() == null;
            }
        }

        /** @deprecated Use {@link ElFunctionUtils#plainResource} instead. */
        @Deprecated
        public static String getPlainResource(String servletPath) {
            return ElFunctionUtils.plainResource(servletPath);
        }

        /** @deprecated Use {@link ElFunctionUtils#resource} instead. */
        @Deprecated
        public static String getResource(String servletPath) {
            return ElFunctionUtils.resource(servletPath);
        }
    }

    public static class PathPattern extends Rule {

        private String pattern;

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        // --- Rule support ---

        @Override
        public boolean evaluate(Variation variation, Profile profile, Object object) {
            HttpServletRequest request = PageContextFilter.Static.getRequest();
            if (request == null) {
                return false;
            }

            String path = request.getServletPath();
            Matcher matcher = Pattern.compile(getPattern()).matcher(path);

            if (!matcher.matches()) {
                return false;
            }

            List<String> matches = new ArrayList<String>();
            StringBuilder pathBuilder = new StringBuilder();
            int lastEnd = 0;

            for (int i = 1, count = matcher.groupCount(); i <= count; ++ i) {
                matches.add(matcher.group(i));
                pathBuilder.append(path.substring(lastEnd, matcher.start(i)));
                lastEnd = matcher.end(i);
            }

            request.setAttribute(PATH_MATCHES_ATTRIBUTE, matches);
            request.setAttribute("pathMatches", matches);

            pathBuilder.append(path.substring(lastEnd));
            PageFilter.Static.setPath(request, pathBuilder.toString());
            return true;
        }
    }

    // --- Deprecated ---

    /** @deprecated No replacement. */
    @Deprecated
    public static final String EXCEPTION_CSS_INJECTED_ATTRIBUTE = ATTRIBUTE_PREFIX + ".exceptionCssInjected";

    /** @deprecated Use {@link Static#getSite} instead. */
    @Deprecated
    public static Site getSite(HttpServletRequest request) {
        return Static.getSite(request);
    }

    /** @deprecated Use {@link Static#setSite} instead. */
    @Deprecated
    public static void setSite(HttpServletRequest request, Site site) {
        Static.setSite(request, site);
    }

    /** @deprecated Use {@link Static#getMainObject} instead. */
    @Deprecated
    public static Object getMainObject(HttpServletRequest request) {
        return Static.getMainObject(request);
    }

    /** @deprecated Use {@link Static#setMainObject} instead. */
    @Deprecated
    public static void setMainObject(HttpServletRequest request, Object mainObject) {
        Static.setMainObject(request, mainObject);
    }

    /**
     * @deprecated Use {@link Static#getPage} instead. To maintain
     *             backward compatibility, this method will return a
     *             {@code null} instead of looking for the most
     *             appropriate page to render with, if it's called
     *             before {@link #doRequest}.
     */
    @Deprecated
    public static Page getPage(HttpServletRequest request) {
        return (Page) request.getAttribute(PAGE_ATTRIBUTE);
    }

    /** @deprecated Use {@link Static#setPage} instead. */
    @Deprecated
    public static void setPage(HttpServletRequest request, Page page) {
        Static.setPage(request, page);
    }

    /** @deprecated Use {@link Static#getProfile} instead. */
    @Deprecated
    public static Profile getProfile(HttpServletRequest request) {
        return Static.getProfile(request);
    }

    /** @deprecated Use {@link Static#setProfile} instead. */
    @Deprecated
    public static void setProfile(HttpServletRequest request, Profile profile) {
        Static.setProfile(request, profile);
    }

    /**
     * @deprecated Use {@link Query#from} and {@link Site#itemsPredicate}
     *             together instead.
     */
    @Deprecated
    public static <T> Query<T> queryFrom(HttpServletRequest request, Class<T> objectClass) {
        return Query.from(objectClass).where(Site.OWNER_FIELD + " = ?", getSite(request));
    }

    /**
     * @deprecated You should let the exception propagate up naturally
     *             instead of catching it and using this method.
     */
    @Deprecated
    public static void writeException(HttpServletRequest request, Writer writer, Exception exception) throws IOException {
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        } else if (exception instanceof IOException) {
            throw (IOException) exception;
        } else {
            throw new RuntimeException(exception);
        }
    }

    /** @deprecated Use {@link Static#getPlainResource} instead. */
    @Deprecated
    public static String getPlainResource(String servletPath) {
        return Static.getPlainResource(servletPath);
    }

    /** @deprecated Use {@link Static#getResource} instead. */
    @Deprecated
    public static String getResource(String servletPath) {
        return Static.getResource(servletPath);
    }

    /** Renders the beginning of the given {@code container}. */
    @Deprecated
    protected static void beginContainer(
            HttpServletRequest request,
            HttpServletResponse response,
            Writer writer,
            ContainerSection container)
            throws IOException, ServletException {

        renderScript(request, response, writer, container.getBeginEngine(), container.getBeginScript());
    }

    /** Renders the end of the given {@code container}. */
    @Deprecated
    protected static void endContainer(
            HttpServletRequest request,
            HttpServletResponse response,
            Writer writer,
            ContainerSection container)
            throws IOException, ServletException {

        renderScript(request, response, writer, container.getEndEngine(), container.getEndScript());
    }

    /** @deprecated No replacement. */
    @Deprecated
    public static final String CURRENT_OBJECT_ATTRIBUTE = ATTRIBUTE_PREFIX + ".currentObject";

    /** @deprecated Use {@link Static#peekObject} instead. */
    @Deprecated
    public static Object getCurrentObject(HttpServletRequest request) {
        return Static.peekObject(request);
    }

    /** @deprecated Use {@link Static#pushObject} instead. */
    @Deprecated
    public static void setCurrentObject(HttpServletRequest request, Object object) {
        request.setAttribute(OBJECTS_ATTRIBUTE, null);
        Static.pushObject(request, object);
    }
}
