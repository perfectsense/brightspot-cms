package com.psddev.cms.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Reference;
import com.psddev.dari.db.ReferentialText;
import com.psddev.dari.util.HtmlElement;

/**
 * A builder of views from rich text.
 */
public class RichTextViewBuilder {

    public static final String RICH_TEXT_ELEMENT_VIEW_TYPE = "rte";

    private Collection<?> richText;

    private Function<String, Object> htmlViewFunction;

    private Function<RichTextElement, Object> richTextElementViewFunction;

    private BiFunction<HtmlElement, List<Object>, Object> htmlElementWrapperViewFunction;

    private boolean renderUnhandledRichTextElements;

    private List<RichTextProcessor> preProcessors = new ArrayList<>();

    /**
     * Creates a new builder for the given rich text.
     *
     * @param richText the rich text to be converted to a view.
     */
    public RichTextViewBuilder(String richText) {
        this.richText = Collections.singletonList(richText);
    }

    /**
     * Sets a handler for converting raw HTML into a view. The function is
     * passed a (possibly unbalanced) HTML fragment and expected to return a
     * view. This is also an opportunity to perform post-processing on non rich
     * text elements.
     *
     * @param htmlViewFunction the HTML view function to be applied.
     * @return this builder.
     */
    public RichTextViewBuilder htmlViewFunction(Function<String, Object> htmlViewFunction) {
        this.htmlViewFunction = htmlViewFunction;
        return this;
    }

    /**
     * Sets a handler for converting rich text elements into views. The function
     * is passed a RichTextElement and is expected to return the resulting view.
     *
     * @param richTextElementViewFunction the rich text element view function
     *        to be applied.
     * @return this builder.
     */
    public RichTextViewBuilder richTextElementViewFunction(Function<RichTextElement, Object> richTextElementViewFunction) {
        this.richTextElementViewFunction = richTextElementViewFunction;
        return this;
    }

    /**
     * Sets a handler for well-balanced HTML elements that contain Rich Text
     * Elements inside of them. The function is passed an HtmlElement, and a
     * list of views that are contained within, and is expected to produce a
     * single view from it. Allows for the preservation of a well-balanced
     * document structure if desired.
     *
     * @param htmlElementWrapperViewFunction the HTML element wrapper view
     *        function to be applied.
     * @return this builder.
     */
    public RichTextViewBuilder htmlElementWrapperViewFunction(BiFunction<HtmlElement, List<Object>, Object> htmlElementWrapperViewFunction) {
        this.htmlElementWrapperViewFunction = htmlElementWrapperViewFunction;
        return this;
    }

    /**
     * Specifies whether rich text element tags should remain in the output
     * if there is no view handler for them.
     *
     * @param renderUnhandledRichTextElements true if the element should remain
     *        in the DOM if unhandled, false otherwise.
     * @return this builder.
     */
    public RichTextViewBuilder renderUnhandledRichTextElements(boolean renderUnhandledRichTextElements) {
        this.renderUnhandledRichTextElements = renderUnhandledRichTextElements;
        return this;
    }

    /**
     * Adds a rich text processor to be applied to the rich text prior to the
     * transformation into a set of views.
     *
     * @param preProcessor the rich text processor to be applied.
     * @return this builder.
     */
    public RichTextViewBuilder addPreProcessor(RichTextProcessor preProcessor) {
        if (preProcessor != null) {
            this.preProcessors.add(preProcessor);
        }
        return this;
    }

    /**
     * Adds all the default rich text processors that are commonly needed when
     * transforming rich text into views.
     *
     * @return this builder.
     */
    public RichTextViewBuilder addAllDefaultPreProcessors() {
        addPreProcessor(new RichTextEditorialMarkupProcessor());
        addPreProcessor(new RichTextLineBreakProcessor());
        return this;
    }

    /**
     * Converts the rich text into 1 or many views.
     *
     * @return a list of views.
     */
    public List<Object> build() {

        List<Object> views = new ArrayList<>();

        Map<String, ObjectType> tagTypes = RichTextElement.getConcreteTagTypes();

        if (richText != null) {

            for (Object item : richText) {

                if (item instanceof String) {

                    String html = (String) item;

                    Document document = Jsoup.parseBodyFragment(html);
                    document.outputSettings().prettyPrint(false);

                    for (RichTextProcessor preProcessor : preProcessors) {
                        preProcessor.process(document);
                    }

                    toRteNodes(document.body().childNodes(), tagTypes)
                            .stream()
                            .map(RteNode::toViews)
                            .flatMap(Collection::stream)
                            .forEach(views::add);

                } else if (item instanceof Reference && referenceViewFunction != null) {

                    Object referenceView = referenceViewFunction.apply((Reference) item);
                    if (referenceView != null) {
                        views.add(referenceView);
                    }
                }
            }
        }

        return views;
    }

    /*
     * Traverses the siblings all the way down the tree, collapsing balanced
     * blocks of HTML that do NOT contain any rich text elements into a single
     * HTML String. If a non-rich text element is found and NONE of its
     * descendants are rich text elements, then it will be collapsed into a
     * String. If ANY of its descendants DO contain a rich text element then
     * each parent of the rich text element will remain an Element object and
     * not be collapsed into a String UNLESS there is no htmlElementWrapperViewFunction
     * defined in which case the element will be converted into a potentially
     * unbalanced HTML String.
     */
    private List<RteNode> toRteNodes(List<Node> siblings, Map<String, ObjectType> tagTypes) {

        List<RteNode> rteNodes = new ArrayList<>();

        for (Node sibling : siblings) {

            if (sibling instanceof Element) {

                Element element = (Element) sibling;
                String tagName = element.tagName();

                ObjectType tagType = tagTypes.get(tagName);

                if (tagType != null && richTextElementViewFunction != null) {

                    RichTextElement rte = (RichTextElement) tagType.createObject(null);

                    rte.fromAttributes(StreamSupport.stream(element.attributes().spliterator(), false)
                            .collect(Collectors.toMap(Attribute::getKey, Attribute::getValue)));

                    rte.fromBody(element.html());

                    rteNodes.add(new RteRichTextElement(rte));

                } else if (tagType == null || renderUnhandledRichTextElements) {

                    List<RteNode> childRteNodes = toRteNodes(element.childNodes(), tagTypes);

                    if (htmlElementWrapperViewFunction != null
                            && childRteNodes.stream().filter(rteNode -> !(rteNode instanceof RteHtml)).count() > 0) {

                        // preserve the RteElement, and collapse children as much as possible
                        rteNodes.add(new RteElement(element, childRteNodes));

                    } else {
                        String elementHtml = element.outerHtml();

                        if (element.tag().isSelfClosing()) {
                            rteNodes.add(new RteHtml(elementHtml));

                        } else {
                            int firstGtAt = elementHtml.indexOf('>');
                            int lastLtAt = elementHtml.lastIndexOf('<');

                            // deliberately do not validate the index values
                            // above since they should always be valid. If it
                            // turns out there's an edge case where they aren't,
                            // a RuntimeException will be thrown and we can
                            // re-evaluate from there.

                            rteNodes.add(new RteHtml(elementHtml.substring(0, firstGtAt + 1)));
                            rteNodes.addAll(childRteNodes);
                            rteNodes.add(new RteHtml(elementHtml.substring(lastLtAt)));
                        }
                    }
                }

            } else if (sibling instanceof TextNode || sibling instanceof DataNode) {

                if (sibling instanceof TextNode) {
                    rteNodes.add(new RteHtml(((TextNode) sibling).text()));

                } else {
                    rteNodes.add(new RteHtml(((DataNode) sibling).getWholeData()));
                }
            }
        }

        // collapse the nodes as much as possible
        return collapseRteNodes(rteNodes);
    }

    private List<RteNode> collapseRteNodes(List<RteNode> rteNodes) {

        List<RteNode> collapsedRteNodes = new ArrayList<>();

        List<RteHtml> adjacentHtmlNodes = new ArrayList<>();

        for (RteNode childRteNode : rteNodes) {

            if (childRteNode instanceof RteHtml) {
                adjacentHtmlNodes.add((RteHtml) childRteNode);

            } else {
                collapsedRteNodes.add(new RteHtml(
                        adjacentHtmlNodes.stream()
                                .map(rteHtml -> rteHtml.html)
                                .collect(Collectors.joining(""))));

                adjacentHtmlNodes.clear();

                collapsedRteNodes.add(childRteNode);
            }
        }

        if (!adjacentHtmlNodes.isEmpty()) {
            collapsedRteNodes.add(new RteHtml(
                    adjacentHtmlNodes.stream()
                            .map(rteHtml -> rteHtml.html)
                            .collect(Collectors.joining(""))));
        }

        return collapsedRteNodes;
    }

    private interface RteNode {

        List<Object> toViews();
    }

    private class RteHtml implements RteNode {

        private String html;

        RteHtml(String html) {
            this.html = html;
        }

        @Override
        public List<Object> toViews() {
            Object view;
            if (htmlViewFunction != null) {
                view = htmlViewFunction.apply(html);
            } else {
                view = html;
            }
            return view != null ? Collections.singletonList(view) : Collections.emptyList();
        }
    }

    private class RteElement implements RteNode {

        private Element element;

        private List<RteNode> children = new ArrayList<>();

        RteElement(Element element, List<RteNode> children) {
            this.element = element;
            this.children = children;
        }

        @Override
        public List<Object> toViews() {
            if (htmlElementWrapperViewFunction != null) {

                HtmlElement htmlElement = new HtmlElement();
                htmlElement.setName(element.tagName());
                htmlElement.setAttributes(element.attributes().asList()
                        .stream()
                        .collect(Collectors.toMap(Attribute::getKey, Attribute::getValue)));

                Object view = htmlElementWrapperViewFunction.apply(htmlElement,
                        children.stream()
                                .map(RteNode::toViews)
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList()));

                return view != null ? Collections.singletonList(view) : Collections.emptyList();

            } else {
                return Collections.emptyList();
            }
        }
    }

    private class RteRichTextElement implements RteNode {

        private RichTextElement richTextElement;

        RteRichTextElement(RichTextElement richTextElement) {
            this.richTextElement = richTextElement;
        }

        @Override
        public List<Object> toViews() {
            Object view = null;
            if (richTextElementViewFunction != null) {
                view = richTextElementViewFunction.apply(richTextElement);
            }
            return view != null ? Collections.singletonList(view) : Collections.emptyList();
        }
    }

    /**
     * @deprecated No replacement.
     */
    @Deprecated
    public static final String REFERENCE_VIEW_TYPE = "ref";

    /**
     * @deprecated Use {@link RichTextProcessor(String)} instead.
     */
    @Deprecated
    public RichTextViewBuilder(ReferentialText referentialText) {
        this.richText = referentialText;
    }

    @Deprecated
    private Function<Reference, Object> referenceViewFunction;

    /**
     * @deprecated No replacement.
     */
    @Deprecated
    public RichTextViewBuilder referenceViewFunction(Function<Reference, Object> referenceViewFunction) {
        this.referenceViewFunction = referenceViewFunction;
        return this;
    }
}
