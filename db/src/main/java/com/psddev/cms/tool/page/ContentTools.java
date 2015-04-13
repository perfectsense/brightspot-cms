package com.psddev.cms.tool.page;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.ContentLock;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Renderer;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.DebugFilter;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;

@RoutingFilter.Path(application = "cms", value = "contentTools")
@SuppressWarnings("serial")
public class ContentTools extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        ToolUser user = page.getUser();
        Collection<String> includeFields = Arrays.asList("returnToDashboardOnSave");
        Object object = Query.from(Object.class).where("_id = ?", page.param(UUID.class, "id")).first();
        State state = State.getInstance(object);
        ContentLock contentLock = null;

        if (object != null) {
            contentLock = ContentLock.Static.lock(object, null, user);
        }

        if (page.isFormPost()) {
            if (page.param(String.class, "action-edits") != null) {
                if (state != null) {
                    Date newPublishDate = page.param(Date.class, "publishDate");

                    if (newPublishDate != null) {
                        Content.ObjectModification contentData = state.as(Content.ObjectModification.class);
                        DateTimeZone timeZone = page.getUserDateTimeZone();
                        newPublishDate = new Date(DateTimeFormat.
                                forPattern("yyyy-MM-dd HH:mm:ss").
                                withZone(timeZone).
                                parseMillis(new DateTime(newPublishDate).toString("yyyy-MM-dd HH:mm:ss")));

                        contentData.setPublishUser(page.getUser());
                        contentData.setPublishDate(newPublishDate);
                        state.save();
                    }
                }

            } else if (page.param(String.class, "action-unlock") != null) {
                contentLock.delete();

                page.writeStart("script", "type", "text/javascript");
                    page.writeRaw("window.location.reload();");
                page.writeEnd();

            } else if (page.param(String.class, "action-settings") != null) {
                try {
                    page.include("/WEB-INF/objectPost.jsp", "object", user, "includeFields", includeFields);
                    user.save();

                } catch (Exception error) {
                    page.getErrors().add(error);
                }
            }
        }

        String returnUrl = page.param(String.class, "returnUrl");

        page.writeHeader();
            page.writeStart("style", "type", "text/css");
                page.writeCss(".cms-contentTools th",
                        "width", "25%;");
            page.writeEnd();

            page.writeStart("div", "class", "widget cms-contentTools");
                page.writeStart("h1", "class", "icon icon-wrench");
                    page.writeHtml("Tools");
                page.writeEnd();

                page.writeStart("div", "class", "tabbed");
                    page.writeStart("div",
                            "class", "fixedScrollable",
                            "data-tab", "For Editors");
                        if (object != null) {
                            Content.ObjectModification contentData = state.as(Content.ObjectModification.class);
                            Date publishDate = contentData.getPublishDate();
                            ToolUser publishUser = contentData.getPublishUser();
                            Date updateDate = contentData.getUpdateDate();
                            ToolUser updateUser = contentData.getUpdateUser();

                            page.writeStart("table", "class", "table-striped");
                                page.writeStart("tbody");
                                    if (publishDate != null || publishUser != null) {
                                        page.writeStart("tr");
                                            page.writeStart("th");
                                                page.writeHtml("Published");
                                            page.writeEnd();

                                            page.writeStart("td");
                                                if (publishDate != null) {
                                                    page.writeHtml(page.formatUserDateTime(publishDate));
                                                }
                                            page.writeEnd();

                                            page.writeStart("td");
                                                if (publishUser != null) {
                                                    page.writeObjectLabel(publishUser);
                                                }
                                            page.writeEnd();
                                        page.writeEnd();
                                    }

                                    if (updateDate != null || updateUser != null) {
                                        page.writeStart("tr");
                                            page.writeStart("th");
                                                page.writeHtml("Last Updated");
                                            page.writeEnd();

                                            page.writeStart("td");
                                                if (updateDate != null) {
                                                    page.writeHtml(page.formatUserDateTime(updateDate));
                                                }
                                            page.writeEnd();

                                            page.writeStart("td");
                                                if (updateUser != null) {
                                                    page.writeObjectLabel(updateUser);
                                                }
                                            page.writeEnd();
                                        page.writeEnd();
                                    }
                                page.writeEnd();
                            page.writeEnd();

                            page.writeStart("h2");
                                page.writeHtml("Advanced Edits");
                            page.writeEnd();

                            if (page.isFormPost() &&
                                    page.param(String.class, "action-edits") != null) {
                                if (page.getErrors().isEmpty()) {
                                    page.writeStart("div", "class", "message message-success");
                                        page.writeHtml("Advanced edits successfully saved.");
                                    page.writeEnd();

                                } else {
                                    page.include("/WEB-INF/errors.jsp");
                                }
                            }

                            page.writeStart("form",
                                    "method", "post",
                                    "action", page.url(""));

                                page.writeStart("div", "class", "inputContainer");
                                    page.writeStart("div", "class", "inputLabel");
                                        page.writeStart("label", "for", page.createId());
                                            page.writeHtml("New Publish Date");
                                        page.writeEnd();
                                    page.writeEnd();

                                    page.writeStart("div", "class", "inputSmall");
                                        page.writeElement("input",
                                                "type", "text",
                                                "class", "date",
                                                "name", "publishDate",
                                                "value", page.formatUserDateTime(publishDate));
                                    page.writeEnd();
                                page.writeEnd();

                                page.writeStart("div", "class", "actions");
                                    page.writeStart("button",
                                            "class", "icon icon-action-save",
                                            "name", "action-edits",
                                            "value", true);
                                        page.writeHtml("Save");
                                    page.writeEnd();
                                page.writeEnd();
                            page.writeEnd();

                            if (page.hasPermission("ui/contentLock") && !user.equals(contentLock.getOwner())) {
                                page.writeStart("h2");
                                    page.writeHtml("Content Lock");
                                page.writeEnd();

                                page.writeStart("div", "class", "message message-warning");
                                    page.writeStart("p");
                                        page.writeHtml("Locked by ");
                                        page.writeObjectLabel(contentLock.getOwner());
                                        page.writeHtml(" since ");
                                        page.writeHtml(page.formatUserDateTime(contentLock.getCreateDate()));
                                        page.writeHtml(".");
                                    page.writeEnd();
                                page.writeEnd();

                                page.writeStart("form",
                                        "method", "post",
                                        "action", page.url(""));
                                    page.writeStart("div", "class", "actions");
                                        page.writeStart("button",
                                                "class", "icon icon-unlock",
                                                "name", "action-unlock",
                                                "value", true);
                                            page.writeHtml("Unlock");
                                        page.writeEnd();
                                    page.writeEnd();
                                page.writeEnd();
                            }
                        }

                        page.writeStart("h2");
                            page.writeHtml("Settings");
                        page.writeEnd();

                        if (page.isFormPost() &&
                                page.param(String.class, "action-settings") != null) {
                            if (page.getErrors().isEmpty()) {
                                page.writeStart("div", "class", "message message-success");
                                    page.writeHtml("Settings successfully saved.");
                                page.writeEnd();

                            } else {
                                page.include("/WEB-INF/errors.jsp");
                            }
                        }

                        page.writeStart("form",
                                "method", "post",
                                "action", page.url(""),
                                "style", "margin-bottom:0;");
                            page.include("/WEB-INF/objectForm.jsp", "object", user, "includeFields", includeFields);

                            page.writeStart("div", "class", "actions");
                                page.writeStart("button",
                                        "class", "icon icon-action-save",
                                        "name", "action-settings",
                                        "value", true);
                                    page.writeHtml("Save");
                                page.writeEnd();
                            page.writeEnd();
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("div",
                            "class", "fixedScrollable",
                            "data-tab", "For Developers");
                        page.writeStart("ul");
                            if (object != null) {
                                page.writeStart("li");
                                    page.writeStart("a",
                                            "target", "_blank",
                                            "href", page.objectUrl("/contentRaw", object));
                                        page.writeHtml("View Raw Data");
                                    page.writeEnd();
                                page.writeEnd();
                            }

                            if (!ObjectUtils.isBlank(returnUrl)) {
                                page.writeStart("li");
                                    if (ObjectUtils.to(boolean.class, StringUtils.getQueryParameterValue(returnUrl, "deprecated"))) {
                                        page.writeStart("a",
                                                "target", "_top",
                                                "href", StringUtils.addQueryParameters(returnUrl,
                                                        "deprecated", null));
                                            page.writeHtml("Hide Deprecated Fields");
                                        page.writeEnd();

                                    } else {
                                        page.writeStart("a",
                                                "target", "_top",
                                                "href", StringUtils.addQueryParameters(returnUrl,
                                                        "deprecated", true));
                                            page.writeHtml("Show Deprecated Fields");
                                        page.writeEnd();
                                    }
                                page.writeEnd();
                            }
                        page.writeEnd();

                        if (object != null) {
                            ObjectType type = state.getType();

                            page.writeStart("table", "class", "table-striped");
                                page.writeStart("tbody");
                                    if (type != null) {
                                        Class<?> objectClass = type.getObjectClass();

                                        if (objectClass != null) {
                                            page.writeStart("tr");
                                                page.writeStart("th");
                                                    page.writeStart("label", "for", page.createId());
                                                        page.writeHtml("Class");
                                                    page.writeEnd();
                                                page.writeEnd();

                                                page.writeStart("td");
                                                    page.writeJavaClassLink(objectClass);
                                                page.writeEnd();
                                            page.writeEnd();
                                        }
                                    }

                                    page.writeStart("tr");
                                        page.writeStart("th");
                                            page.writeStart("label", "for", page.createId());
                                                page.writeHtml("ID");
                                            page.writeEnd();
                                        page.writeEnd();

                                        page.writeStart("td");
                                            page.writeElement("input",
                                                    "type", "text",
                                                    "id", page.getId(),
                                                    "class", "code",
                                                    "value", state.getId(),
                                                    "readonly", "readonly",
                                                    "style", "width:100%;",
                                                    "onclick", "this.select();");
                                        page.writeEnd();
                                    page.writeEnd();

                                    page.writeStart("tr");
                                        page.writeStart("th");
                                            page.writeStart("label", "for", page.createId());
                                                page.writeHtml("URL");
                                            page.writeEnd();
                                        page.writeEnd();

                                        page.writeStart("td");
                                            page.writeElement("input",
                                                    "type", "text",
                                                    "id", page.getId(),
                                                    "value", JspUtils.getAbsoluteUrl(page.getRequest(), page.cmsUrl("/content/edit.jsp", "id", state.getId())),
                                                    "readonly", "readonly",
                                                    "style", "width:100%;",
                                                    "onclick", "this.select();");
                                        page.writeEnd();
                                    page.writeEnd();
                                page.writeEnd();
                            page.writeEnd();
                        }

                        if (object != null) {
                            ObjectType type = state.getType();

                            if (type != null) {
                                if (!ObjectUtils.isBlank(type.as(Renderer.TypeModification.class).getEmbedPath())) {
                                    String permalink = state.as(Directory.ObjectModification.class).getPermalink();

                                    if (!ObjectUtils.isBlank(permalink)) {
                                        String siteUrl = Application.Static.getInstance(CmsTool.class).getDefaultSiteUrl();
                                        StringBuilder embedCode = new StringBuilder();

                                        embedCode.append("<script type=\"text/javascript\" src=\"");
                                        embedCode.append(StringUtils.addQueryParameters(
                                                StringUtils.removeEnd(siteUrl, "/") + permalink,
                                                "_embed", true,
                                                "_format", "js"));
                                        embedCode.append("\"></script>");

                                        page.writeHtml("Embed Code:");
                                        page.writeElement("br");
                                        page.writeStart("textarea",
                                                "class", "code",
                                                "data-expandable-class", "code",
                                                "readonly", "readonly",
                                                "onclick", "this.select();");
                                            page.writeHtml(embedCode);
                                        page.writeEnd();
                                    }
                                }

                                String defaultPath = type.as(Renderer.TypeModification.class).getPath();
                                Map<String, String> paths = type.as(Renderer.TypeModification.class).getPaths();

                                if (!ObjectUtils.isBlank(defaultPath) || !ObjectUtils.isBlank(paths)) {
                                    page.writeStart("h2");
                                        page.writeHtml("Renderers");
                                    page.writeEnd();

                                    page.writeStart("table", "class", "table-striped");
                                        page.writeStart("tbody");
                                            if (!ObjectUtils.isBlank(defaultPath)) {
                                                page.writeStart("tr");
                                                    page.writeStart("th");
                                                        page.writeStart("code");
                                                            page.writeHtml("Default");
                                                        page.writeEnd();
                                                    page.writeEnd();

                                                    page.writeStart("td");
                                                        page.writeStart("code");
                                                            page.writeStart("a",
                                                                    "target", "_blank",
                                                                    "href", DebugFilter.Static.getServletPath(page.getRequest(), "code",
                                                                            "action", "edit",
                                                                            "type", "JSP",
                                                                            "servletPath", defaultPath));
                                                                page.writeHtml(defaultPath);
                                                            page.writeEnd();
                                                        page.writeEnd();
                                                    page.writeEnd();
                                                page.writeEnd();
                                            }

                                            for (Map.Entry<String, String> entry : paths.entrySet()) {
                                                page.writeStart("tr");
                                                    page.writeStart("th");
                                                        page.writeStart("code");
                                                            page.writeHtml(entry.getKey());
                                                        page.writeEnd();
                                                    page.writeEnd();

                                                    page.writeStart("td");
                                                        page.writeStart("code");
                                                            page.writeStart("a",
                                                                    "target", "_blank",
                                                                    "href", DebugFilter.Static.getServletPath(page.getRequest(), "code",
                                                                            "action", "edit",
                                                                            "type", "JSP",
                                                                            "servletPath", entry.getValue()));
                                                                page.writeHtml(entry.getValue());
                                                            page.writeEnd();
                                                        page.writeEnd();
                                                    page.writeEnd();
                                                page.writeEnd();
                                            }
                                        page.writeEnd();
                                    page.writeEnd();
                                }

                                Class<?> objectClass = type.getObjectClass();

                                if (objectClass != null) {
                                    Static.writeJavaAnnotationDescriptions(page, objectClass);
                                }
                            }
                        }
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }

    /**
     * {@link ContentTools} utility methods.
     */
    public static class Static {

        public static void writeJavaAnnotationDescriptions(
                ToolPageContext page,
                AnnotatedElement annotated)
                throws IOException {

            List<Annotation> presentAnnotations = Arrays.asList(annotated.getAnnotations());
            List<Class<? extends Annotation>> possibleAnnotationClasses = new ArrayList<Class<? extends Annotation>>();

            for (Class<? extends Annotation> ac : ClassFinder.Static.findClasses(Annotation.class)) {
                if (!ac.isAnnotationPresent(Deprecated.class) &&
                        ac.isAnnotationPresent(annotated instanceof Field ?
                                ObjectField.AnnotationProcessorClass.class :
                                ObjectType.AnnotationProcessorClass.class)) {
                    possibleAnnotationClasses.add(ac);
                    continue;
                }
            }

            Collections.sort(possibleAnnotationClasses, new Comparator<Class<? extends Annotation>>() {

                @Override
                public int compare(Class<? extends Annotation> x, Class<? extends Annotation> y) {
                    return x.getName().compareTo(y.getName());
                }
            });

            if (!presentAnnotations.isEmpty()) {
                page.writeStart("h2");
                    page.writeHtml("Present Annotations");
                page.writeEnd();

                page.writeStart("ul");
                    for (Annotation a : presentAnnotations) {
                        page.writeStart("li");
                            writeJavaAnnotationDescription(page, a);
                        page.writeEnd();
                    }
                page.writeEnd();
            }

            page.writeStart("h2");
                page.writeHtml("Possible Annotations");
            page.writeEnd();

            page.writeStart("ul");
                for (Class<? extends Annotation> ac : possibleAnnotationClasses) {
                    page.writeStart("li");
                        page.writeJavaClassLink(ac);
                    page.writeEnd();
                }
            page.writeEnd();
        }

        private static void writeJavaAnnotationDescription(
                ToolPageContext page,
                Annotation annotation)
                throws IOException {

            Class<? extends Annotation> aClass = annotation.annotationType();

            page.writeJavaClassLink(aClass);

            List<Method> aMethods = new ArrayList<Method>(Arrays.asList(aClass.getMethods()));

            for (Iterator<Method> i = aMethods.iterator(); i.hasNext();) {
                if (!i.next().getDeclaringClass().equals(aClass)) {
                    i.remove();
                }
            }

            if (!aMethods.isEmpty()) {
                page.writeStart("table", "class", "table-striped");
                    page.writeStart("tbody");
                        for (Method m : aMethods) {
                            if (m.getDeclaringClass().equals(aClass)) {
                                page.writeStart("tr");
                                    page.writeStart("th");
                                        page.writeStart("code");
                                            page.writeHtml(m.getName());
                                        page.writeEnd();
                                    page.writeEnd();

                                    page.writeStart("td");
                                        try {
                                            writeJavaAnnotationValue(page, m.invoke(annotation));
                                        } catch (IllegalAccessException error) {
                                            // Ignore reflection look-up errors.
                                        } catch (InvocationTargetException error) {
                                            // Ignore reflection look-up errors.
                                        }
                                    page.writeEnd();
                                page.writeEnd();
                            }
                        }
                    page.writeEnd();
                page.writeEnd();
            }
        }

        public static void writeJavaAnnotationValue(
                ToolPageContext page,
                Object value)
                throws IOException {

            if (value instanceof String) {
                page.writeStart("code");
                    page.writeHtml('"');
                    page.writeHtml(value);
                    page.writeHtml('"');
                page.writeEnd();

            } else if (value instanceof Class) {
                page.writeJavaClassLink((Class<?>) value);

            } else if (value instanceof Annotation) {
                writeJavaAnnotationDescription(page, (Annotation) value);

            } else if (value.getClass().isArray()) {
                int length = Array.getLength(value);

                if (length > 0) {
                    page.writeStart("ol");
                        for (int i = 0; i < length; ++ i) {
                            page.writeStart("li");
                                writeJavaAnnotationValue(page, Array.get(value, i));
                            page.writeEnd();
                        }
                    page.writeEnd();
                }

            } else {
                page.writeStart("code");
                    page.writeHtml(value);
                page.writeEnd();
            }
        }
    }
}
