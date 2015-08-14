package com.psddev.cms.db;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.HtmlElement;
import com.psddev.dari.util.HtmlNode;
import com.psddev.dari.util.HtmlText;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;

/**
 * Holds various data on how to render a page. This class is commonly used to
 * easily update and render the contents of the {@code <head>} element.
 *
 * <p>For example, given the following class:</p>
 *
 * <blockquote><pre data-type="java">
 *public class Article extends Content implements PageStage.Updatable {
 *
 *    private String title;
 *
 *    public String getTitle() {
 *        return title;
 *    }
 *
 *    {@literal @}Override
 *    public void updateStage(PageStage stage) {
 *        stage.setTitle(getTitle());
 *    }
 *}
 * </pre></blockquote>
 *
 * <p>The following JSP fragment:</p>
 *
 * <blockquote><pre data-type="jsp">
 *&lt;head&gt;
 *&lt;cms:render value="${stage.headNodes}" /&gt;
 *&lt;/head&gt;
 * </pre></blockquote>
 *
 * <p>Will output:</p>
 *
 * <blockquote><pre data-type="html">{@literal
 *<head>
 *<title>The Article Title</title>
 *</head>
 * }</pre></blockquote>
 *
 * <p>Additionally, classes can share the update logic using the
 * {@link UpdateClass} annotation:</p>
 *
 * <blockquote><pre data-type="java">
 *public class DefaultPageStageUpdater implements PageStage.SharedUpdatable {
 *
 *    {@literal @}Override
 *    public void updateStageBefore(Object object, PageStage stage) {
 *    }
 *
 *    {@literal @}Override
 *    public void updateStageAfter(Object object, PageStage stage) {
 *        stage.setTitle(stage.getTitle() + " | Site Name");
 *    }
 *}
 *
 *{@literal @}PageStage.UpdateClass(DefaultPageStageUpdater.class)
 *public class Article extends Content {
 *}
 * </pre></blockquote>
 */
public class PageStage extends Record {

    private transient ServletContext servletContext;

    private transient HttpServletRequest request;

    private final transient List<HtmlNode> headNodes = new ArrayList<HtmlNode>();

    public PageStage() {
    }

    public PageStage(ServletContext servletContext, HttpServletRequest request) {
        this.servletContext = servletContext;
        this.request = request;
    }

    /**
     * Returns the servlet context associated with this page stage.
     *
     * @return May be {@code null}.
     */
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Returns the request associated with this page stage.
     *
     * @return May be {@code null}.
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Returns the list of all nodes in the {@code <head>} element.
     *
     * @return Never {@code null}. Mutable.
     */
    public List<HtmlNode> getHeadNodes() {
        return headNodes;
    }

    /**
     * Finds an element with the the given {@code name} and
     * {@code attributes} within the {@code <head>}.
     *
     * @param name Can't be blank.
     * @param attributes May be {@code null}.
     * @return May be {@code null}.
     */
    public HtmlElement findHeadElement(String name, Object... attributes) {
        ErrorUtils.errorIfBlank(name, "name");

        for (HtmlNode node : getHeadNodes()) {
            if (!(node instanceof HtmlElement)) {
                continue;
            }

            HtmlElement element = (HtmlElement) node;

            if (name.equals(element.getName())
                    && element.hasAttributes(attributes)) {
                return element;
            }
        }

        return null;
    }

    /**
     * Finds or creates an element with the given {@code name} and
     * {@code attributes} within the {@code <head>}.
     *
     * <p>Note that the stylesheet elements are grouped first, followed by
     * all non-script elements, followed by script elements, with insertion
     * order within the groups preserved.</p>
     *
     * @param name Can't be blank.
     * @param attributes May be {@code null}.
     * @return Never {@code null}.
     */
    public HtmlElement findOrCreateHeadElement(String name, Object... attributes) {
        HtmlElement element = findHeadElement(name, attributes);

        if (element == null) {
            element = new HtmlElement();

            element.setName(name);
            element.addAttributes(attributes);

            List<HtmlNode> nodes = getHeadNodes();

            if (ObjectUtils.isBlank(nodes)) {
                nodes.add(element);

            } else {

                // JS goes last.
                if ("script".equals(name)) {
                    nodes.add(element);

                // CSS goes first.
                } else if ("link".equals(name)
                        && "text/css".equals(element.getAttributes().get("type"))) {
                    int insertIndex = 0;

                    for (ListIterator<HtmlNode> i = nodes.listIterator(); i.hasNext();) {
                        HtmlNode node = i.next();

                        if (!(node instanceof HtmlElement)) {
                            continue;
                        }

                        HtmlElement iElement = (HtmlElement) node;

                        if ("link".equals(iElement.getName())
                                && "text/css".equals(iElement.getAttributes().get("type"))) {
                            continue;

                        } else {
                            insertIndex = i.previousIndex();
                            break;
                        }
                    }

                    nodes.add(insertIndex, element);

                // Everything else in between.
                } else {
                    int insertIndex = 0;

                    for (ListIterator<HtmlNode> i = nodes.listIterator(); i.hasNext();) {
                        HtmlNode node = i.next();

                        if (!(node instanceof HtmlElement)) {
                            continue;
                        }

                        HtmlElement iElement = (HtmlElement) node;

                        if ("script".equals(iElement.getName())) {
                            insertIndex = i.previousIndex();
                            break;

                        } else {
                            insertIndex = i.nextIndex();
                        }
                    }

                    nodes.add(insertIndex, element);
                }
            }
        }

        return element;
    }

    /**
     * Removes all tags with the given {@code name} and {@code attributes}
     * within the {@code <head>} element.
     *
     * @param name If blank, does nothing.
     * @param attribues May be {@code null}.
     */
    public void removeHeadTag(String name, Object... attributes) {
        if (ObjectUtils.isBlank(name)) {
            return;
        }

        for (Iterator<HtmlNode> i = getHeadNodes().iterator(); i.hasNext();) {
            HtmlNode node = i.next();

            if (!(node instanceof HtmlElement)) {
                continue;
            }

            HtmlElement element = (HtmlElement) node;

            if (name.equals(element.getName())
                    && element.hasAttributes(attributes)) {
                i.remove();
            }
        }
    }

    /**
     * Returns the title (the text in the {@code <title>} element).
     *
     * @return Never {@code null}.
     */
    public String getTitle() {
        StringBuilder titleText = new StringBuilder();
        HtmlElement titleElement = findHeadElement("title");

        if (titleElement != null) {
            for (HtmlNode child : titleElement.getChildren()) {
                if (child instanceof HtmlText) {
                    titleText.append(((HtmlText) child).getText());
                }
            }
        }

        return titleText.toString();
    }

    /**
     * Sets the title (the text in the {@code <title>} element).
     *
     * @param title If {@code null}, removes the element.
     */
    public void setTitle(String title) {
        if (ObjectUtils.isBlank(title)) {
            removeHeadTag("title");

        } else {
            HtmlText text = new HtmlText();
            List<HtmlNode> children = findOrCreateHeadElement("title").getChildren();

            text.setText(title);
            children.clear();
            children.add(text);
        }

        setMetaProperty("og:title", title);
    }

    /**
     * Returns the description (the value of the {@code content} attribute
     * in either {@code <meta name="description">} or
     * {@code <meta property="og:description">}).
     *
     * @return Never {@code null}.
     */
    public String getDescription() {
        HtmlElement descriptionElement = findHeadElement("meta", "name", "description");

        if (descriptionElement == null) {
            descriptionElement = findHeadElement("meta", "property", "og:description");
        }

        if (descriptionElement != null) {
            String description = descriptionElement.getAttributes().get("content");

            if (description != null) {
                return description;
            }
        }

        return "";
    }

    /**
     * Sets the description (the value of the {@code content} attribute
     * in both {@code <meta name="description" content="description">}
     * and {@code <meta property="og:description" content="description">}).
     *
     * @param description If {@code null}, removes the elements.
     */
    public void setDescription(String description) {
        setMetaName("description", description);
        setMetaProperty("og:description", description);
    }

    /**
     * Returns the canonical URL (the value of the {@code href} attribute
     * in {@code <link rel="canonical">}.
     *
     * @return May be {@code null}.
     */
    public String getCanonicalUrl() {
        HtmlElement urlElement = findHeadElement("link", "rel", "canonical");

        return urlElement != null
                ? urlElement.getAttributes().get("href")
                : null;
    }

    /**
     * Sets the canonical URL (the value of the {@code href} attribute in
     * {@code <link rel="canonical">}.
     *
     * @param canonicalUrl If {@code null}, removes the element.
     */
    public void setCanonicalUrl(String canonicalUrl) {
        if (ObjectUtils.isBlank(canonicalUrl)) {
            removeHeadTag("link", "rel", "canonical");

        } else {
            findOrCreateHeadElement("link", "rel", "canonical").getAttributes().put("href", canonicalUrl);
        }
    }

    /**
     * Sets {@code <meta name="name" content="content">}.
     *
     * @param name If blank, does nothing.
     * @param content If blank, removes the tag.
     */
    public void setMetaName(String name, String content) {
        if (!ObjectUtils.isBlank(name)) {
            if (ObjectUtils.isBlank(content)) {
                removeHeadTag("meta", "name", name);

            } else {
                findOrCreateHeadElement("meta", "name", name).getAttributes().put("content", content);
            }
        }
    }

    /**
     * Sets {@code <meta property="property" content="content">}.
     *
     * @param property If blank, does nothing.
     * @param content If blank, removes the tag.
     */
    public void setMetaProperty(String property, String content) {
        if (!ObjectUtils.isBlank(property)) {
            if (ObjectUtils.isBlank(content)) {
                removeHeadTag("meta", "property", property);

            } else {
                findOrCreateHeadElement("meta", "property", property).getAttributes().put("content", content);
            }
        }
    }

    /**
     * Sets {@code <meta http-equiv="httpEquiv" content="content">}.
     *
     * @param httpEquiv If blank, does nothing.
     * @param content If blank, removes the tag.
     */
    public void setHttpEquiv(String httpEquiv, String content) {
        if (!ObjectUtils.isBlank(httpEquiv)) {
            if (ObjectUtils.isBlank(content)) {
                removeHeadTag("meta", "http-equiv", httpEquiv);

            } else {
                findOrCreateHeadElement("meta", "http-equiv", httpEquiv).getAttributes().put("content", content);
            }
        }
    }

    /**
     * Adds {@code <link rel="stylsheet" type="text/css" href="href">}.
     *
     * @param href If blank, does nothing.
     */
    public void addStyleSheet(String href) {
        if (!ObjectUtils.isBlank(href)) {
            findOrCreateHeadElement("link",
                    "rel", href.endsWith(".less") ? "stylesheet/less" : "stylesheet",
                    "type", "text/css",
                    "href", ElFunctionUtils.resource(href));
        }
    }

    /**
     * Adds {@code <script type="text/javascript" src="src"></script>}
     * within the {@code <head>} element.
     *
     * @param src If blank, does nothing.
     */
    public void addScript(String src) {
        if (!ObjectUtils.isBlank(src)) {
            findOrCreateHeadElement("script",
                    "type", "text/javascript",
                    "src", ElFunctionUtils.resource(src));
        }
    }

    /**
     * Updates this stage using the given {@code object}.
     *
     * @param object Can't be {@code null}.
     */
    public void update(Object object) {
        State state = State.getInstance(object);
        ObjectType type = state.getType();
        SharedUpdatable sharedUpdatable = type != null
                ? type.as(TypeData.class).createSharedUpdatable()
                : null;

        if (sharedUpdatable != null) {
            sharedUpdatable.updateStageBefore(object, this);
        }

        if (object instanceof Updatable) {
            ((Updatable) object).updateStage(this);
        }

        if (sharedUpdatable != null) {
            sharedUpdatable.updateStageAfter(object, this);
        }
    }

    /**
     * Specifies the {@link Updatable} class that will update the
     * {@link PageStage} for all instances of the target type. If the class
     * directly implements {@link Updatable}, this will run after.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(UpdateClassProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface UpdateClass {

        Class<? extends SharedUpdatable> value();
    }

    private static class UpdateClassProcessor implements ObjectType.AnnotationProcessor<UpdateClass> {

        @Override
        public void process(ObjectType type, UpdateClass annotation) {
            type.as(TypeData.class).setUpdateClassName(annotation.value().getName());
        }
    }

    /**
     * Type modification that adds {@link PageStage}-specific data.
     */
    @FieldInternalNamePrefix("cms.pageStage.")
    public static class TypeData extends Modification<ObjectType> {

        private String updateClassName;

        public String getUpdateClassName() {
            return updateClassName;
        }

        public void setUpdateClassName(String updateClassName) {
            this.updateClassName = updateClassName;
        }

        /**
         * Creates a shared updatable object appropriate for this type.
         *
         * @return May be {@code null}.
         */
        @SuppressWarnings("unchecked")
        public SharedUpdatable createSharedUpdatable() {
            Class<?> c = ObjectUtils.getClassByName(getUpdateClassName());

            return c != null && SharedUpdatable.class.isAssignableFrom(c)
                    ? TypeDefinition.getInstance((Class<? extends SharedUpdatable>) c).newInstance()
                    : null;
        }
    }

    /**
     * {@link PageFilter} will call {@link #updateStage} before rendering
     * if the main content implements this interface.
     */
    public static interface Updatable {

        /**
         * Updates the given {@code stage}. This is typically used to set
         * the nodes within the {@code <head>} element, such as the
         * {@code <title>}.
         *
         * @param stage Can't be {@code null}.
         */
        public void updateStage(PageStage stage);
    }

    /**
     * {@link PageFilter} will call the appropriate methods if the main
     * content is annotated with {@link UpdateClass} that points to a class
     * that implements this interface.
     */
    public static interface SharedUpdatable {

        /**
         * Updates the given {@code stage} before the {@code object}-specific
         * logic executes. This is typically used to set the defaults.
         *
         * @param object Can't be {@code nul}.
         * @param stage Can't be {@code null}.
         */
        public void updateStageBefore(Object object, PageStage stage);

        /**
         * Updates the given {@code stage} after the {@code object}-specific
         * logic executes. This is typically used to add common extra data,
         * such as adding the site name to the page title.
         *
         * @param object Can't be {@code nul}.
         * @param stage Can't be {@code null}.
         */
        public void updateStageAfter(Object object, PageStage stage);
    }
}
