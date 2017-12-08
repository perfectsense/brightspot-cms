package com.psddev.cms.rte;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.base.Preconditions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.psddev.cms.db.RichTextElement;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Reference;
import com.psddev.dari.db.ReferentialText;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ObjectUtils;

/**
 * Builder that can convert HTML into views.
 *
 * <p>Typical use within a {@link com.psddev.cms.view.ViewModel} implementation
 * might look like:</p>
 *
 * <blockquote><pre>
 *     {@code List<MyRichTextItemView> views = RichTextViewBuilder.build(model.getBody(), rte -> createView(MyRichTextItemView.class, rte);}
 * </pre></blockquote>
 */
public class RichTextViewBuilder<V> {

    private final String html;
    private Function<String, V> htmlToView;
    private Function<RichTextElement, V> elementToView;
    private boolean keepUnboundElements;
    private final List<RichTextPreprocessor> preprocessors = new ArrayList<>();

    /**
     * Creates a new instance that will convert the given {@code html}.
     *
     * @param html Nonnull.
     */
    public RichTextViewBuilder(String html) {
        Preconditions.checkNotNull(html);
        this.html = html;
    }

    /**
     * Creates a new instance that will convert the given {@code text}.
     *
     * @param text Nonnull.
     */
    public RichTextViewBuilder(ReferentialText text) {
        Preconditions.checkNotNull(text);
        this.html = toHtml(text);
    }

    // Converts ref text into an HTML string.
    private static String toHtml(ReferentialText text) {
        if (text == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (Object item : text) {
            if (item instanceof Reference) {
                Reference ref = (Reference) item;
                StringWriter refString = new StringWriter();
                HtmlWriter refHtml = new HtmlWriter(refString);

                try {
                    refHtml.writeStart(
                            ReferenceRichTextElement.TAG_NAME,
                            ReferenceRichTextElement.VALUES_ATTRIBUTE,
                            ObjectUtils.toJson(ref.getState().getSimpleValues()));

                    refHtml.writeEnd();

                } catch (IOException error) {
                    throw new IllegalStateException(error);
                }

                builder.append(refString);

            } else {
                builder.append(item);
            }
        }

        return builder.toString();
    }

    /**
     * Converts the given given {@code html} into a list of views using the
     * given {@code elementToView} function.
     *
     * <p>This method sets the most commonly used options automatically.</p>
     *
     * <p>Note that this API deliberately breaks the generics contract of the
     * returned list, such that it may contain instances of
     * {@link com.psddev.cms.view.RawView}. Thus, explicitly iterating over
     * the returned list with the type of the generic argument may result in
     * a {@link java.lang.ClassCastException}.</p>
     *
     * <p>If explicit iteration is required for your application to function
     * properly, cast the list to {@code List<Object>} and do an
     * {@code instanceof} check before casting to the desired type, or use the
     * more general {@link #RichTextViewBuilder(String)} API and call
     * {@link #htmlToView(Function)} to supply a function that conforms to the
     * generic type.</p>
     *
     * @param <V> The type of views to return.
     * @param html If {@code null}, returns an empty list.
     * @param elementToView Nonnull.
     * @return Nonnull.
     */
    public static <V> List<V> build(String html, Function<RichTextElement, V> elementToView) {
        Preconditions.checkNotNull(elementToView);

        if (html != null) {
            return new RichTextViewBuilder<V>(html)
                    .addPreprocessor(new EditorialMarkupRichTextPreprocessor())
                    .addPreprocessor(new LineBreakRichTextPreprocessor())
                    .elementToView(elementToView)
                    .build();

        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Converts the given given {@code text} into a list of views using the
     * given {@code elementToView} function.
     *
     * @param <V> The type of views to return.
     * @param text If {@code null}, returns an empty list.
     * @param elementToView Nonnull.
     * @return Nonnull.
     * @see #build(String, Function)
     */
    public static <V> List<V> build(ReferentialText text, Function<RichTextElement, V> elementToView) {
        return build(toHtml(text), elementToView);
    }

    /**
     * Sets the function that's used to convert raw HTML into a view.
     *
     * <p>Note that the raw HTML may be unbalanced.</p>
     *
     * @param htmlToView Nullable.
     * @return Itself.
     */
    public RichTextViewBuilder<V> htmlToView(Function<String, V> htmlToView) {
        this.htmlToView = htmlToView;
        return this;
    }

    /**
     * Sets the handler that's used to convert a rich text element into a view.
     *
     * @param elementToView Nullable.
     * @return Itself.
     */
    public RichTextViewBuilder<V> elementToView(Function<RichTextElement, V> elementToView) {
        this.elementToView = elementToView;
        return this;
    }

    /**
     * Sets whether the rich text element tags should remain in the output
     * when there aren't any {@link com.psddev.cms.view.ViewBinding}s on them.
     *
     * @return Itself.
     */
    public RichTextViewBuilder<V> keepUnboundElements(boolean keepUnboundElements) {
        this.keepUnboundElements = keepUnboundElements;
        return this;
    }

    /**
     * Adds a rich text preprocessor to be applied to the rich text prior to the
     * transformation into a set of views.
     *
     * @param preprocessor the rich text preprocessor to be applied.
     * @return this builder.
     */
    public RichTextViewBuilder<V> addPreprocessor(RichTextPreprocessor preprocessor) {
        Preconditions.checkNotNull(preprocessor);
        this.preprocessors.add(preprocessor);
        return this;
    }

    /**
     * Converts the HTML into a list of views.
     *
     * @return Nonnull.
     */
    public List<V> build() {
        List<V> views = new ArrayList<>();
        Map<String, ObjectType> tagTypes = new HashMap<>(RichTextElement.getConcreteTagTypes());

        tagTypes.put(ReferenceRichTextElement.TAG_NAME, ObjectType.getInstance(ReferenceRichTextElement.class));

        Document document = Jsoup.parseBodyFragment(html);

        document.outputSettings().prettyPrint(false);

        for (RichTextPreprocessor preprocessor : preprocessors) {
            preprocessor.preprocess(document.body());
        }

        toViewNodes(document.body().childNodes(), tagTypes)
                .stream()
                .map(RichTextViewNode::toView)
                .filter(Objects::nonNull)
                .forEach(views::add);

        return views;
    }

    // Traverses the siblings all the way down the tree, collapsing balanced
    // blocks of HTML that do NOT contain any rich text elements into a single
    // HTML string.
    private List<RichTextViewNode<V>> toViewNodes(List<Node> siblings, Map<String, ObjectType> tagTypes) {
        List<RichTextViewNode<V>> viewNodes = new ArrayList<>();

        for (Node sibling : siblings) {
            if (sibling instanceof Element) {
                Element element = (Element) sibling;
                ObjectType tagType = tagTypes.get(element.tagName());

                if (tagType != null && elementToView != null) {
                    RichTextElement rte = (RichTextElement) tagType.createObject(null);

                    rte.fromAttributes(StreamSupport
                            .stream(element.attributes().spliterator(), false)
                            .collect(Collectors.toMap(Attribute::getKey, Attribute::getValue)));

                    rte.fromBody(element.html());

                    viewNodes.add(new ElementRichTextViewNode<>(rte, elementToView));

                } else if (tagType == null || keepUnboundElements) {
                    List<RichTextViewNode<V>> childViewNodes = toViewNodes(element.childNodes(), tagTypes);
                    String html = element.outerHtml();

                    if (element.tag().isSelfClosing()) {
                        viewNodes.add(new StringRichTextViewNode<>(html, htmlToView));

                    } else {
                        int firstGtAt = html.indexOf('>');
                        int lastLtAt = html.lastIndexOf('<');

                        // This deliberately does not validate the index values
                        // above, since non-self-closing element should always
                        // have those characters present in the HTML.
                        viewNodes.add(new StringRichTextViewNode<>(html.substring(0, firstGtAt + 1), htmlToView));
                        viewNodes.addAll(childViewNodes);
                        viewNodes.add(new StringRichTextViewNode<>(html.substring(lastLtAt), htmlToView));
                    }
                }

            } else if (sibling instanceof TextNode) {
                viewNodes.add(new StringRichTextViewNode<>(sibling.outerHtml(), htmlToView));

            } else if (sibling instanceof DataNode) {
                viewNodes.add(new StringRichTextViewNode<>(((DataNode) sibling).getWholeData(), htmlToView));
            }
        }

        // Collapse the nodes as much as possible.
        List<RichTextViewNode<V>> collapsed = new ArrayList<>();
        List<StringRichTextViewNode<V>> adjacent = new ArrayList<>();

        for (RichTextViewNode<V> childBuilderNode : viewNodes) {
            if (childBuilderNode instanceof StringRichTextViewNode) {
                adjacent.add((StringRichTextViewNode<V>) childBuilderNode);

            } else {
                collapsed.add(new StringRichTextViewNode<>(
                        adjacent.stream()
                                .map(StringRichTextViewNode::getHtml)
                                .collect(Collectors.joining()), htmlToView));

                adjacent.clear();

                collapsed.add(childBuilderNode);
            }
        }

        if (!adjacent.isEmpty()) {
            collapsed.add(new StringRichTextViewNode<>(
                    adjacent.stream()
                            .map(StringRichTextViewNode::getHtml)
                            .collect(Collectors.joining()), htmlToView));
        }

        return collapsed;
    }
}
