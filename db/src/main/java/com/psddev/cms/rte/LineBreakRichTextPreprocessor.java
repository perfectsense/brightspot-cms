package com.psddev.cms.rte;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import com.psddev.cms.db.RichTextElement;
import com.psddev.dari.db.ObjectType;

/**
 * {@link RichTextPreprocessor} implementation that converts line breaks into
 * block level elements.
 *
 * <p>By default, this preprocessor tries to wrap a block of text that's
 * visually surrounded by blank lines into a paragraph. For example, given
 * either of the following two HTML snippets:</p>
 *
 * <ul>
 *     <li><{@code foo<br><br>bar}</li>
 *     <li><{@code <div>foo</div><br>bar}</li>
 * </ul>
 *
 * <p>This preprocessor would both convert both into:</p>
 *
 * <blockquote><pre>{@code <p>foo</p><p>bar</p>}</pre></blockquote>
 */
public class LineBreakRichTextPreprocessor implements RichTextPreprocessor {

    private static final Tag BR_TAG = Tag.valueOf("br");
    private static final Tag DIV_TAG = Tag.valueOf("div");

    private Tag tag;

    /**
     * Creates an instance that uses tags with the given {@code tagName} to
     * wrap the blocks.
     *
     * @param tagName Nonnull.
     */
    private LineBreakRichTextPreprocessor(String tagName) {
        Preconditions.checkNotNull(tagName);
        this.tag = Tag.valueOf(tagName);
    }

    /**
     * Creates an instance that uses {@code <p>} to wrap the blocks.
     */
    public LineBreakRichTextPreprocessor() {
        this("p");
    }

    @Override
    public void preprocess(Element body) {
        body.select(".cms-textAlign-left, .cms-textAlign-center, .cms-textAlign-right, ol, ul").forEach(element -> {
            Element next = element.nextElementSibling();

            if (next != null && BR_TAG.equals(next.tag())) {
                next.remove();
            }
        });

        body.select(".cms-textAlign-left, .cms-textAlign-center, .cms-textAlign-right")
                .forEach(div -> div.tagName(tag.getName()));

        // Convert 'text<br><br>' to '<p>text</p>'.
        for (Element br : body.getElementsByTag(BR_TAG.getName())) {
            Element previousBr = null;

            // Find the closest previous <br> without any intervening content.
            for (Node previousNode = br;
                     (previousNode = previousNode.previousSibling()) != null;) {

                if (previousNode instanceof Element) {
                    Element previousElement = (Element) previousNode;

                    if (BR_TAG.equals(previousElement.tag())) {
                        previousBr = previousElement;
                    }

                    break;

                } else if (previousNode instanceof TextNode
                        && !((TextNode) previousNode).isBlank()) {
                    break;
                }
            }

            if (previousBr == null) {
                continue;
            }

            List<Node> paragraphChildren = new ArrayList<>();

            for (Node previous = previousBr;
                     (previous = previous.previousSibling()) != null;) {

                if (previous instanceof Element
                        && ((Element) previous).isBlock()) {
                    break;

                } else {
                    paragraphChildren.add(previous);
                }
            }

            Element paragraph = body.ownerDocument().createElement(tag.getName());

            for (Node child : paragraphChildren) {
                child.remove();
                paragraph.prependChild(child.clone());
            }

            br.before(paragraph);
            br.remove();
            previousBr.remove();
        }

        // Convert inline text first in body and after block elements into
        // paragraphs.
        if (body.childNodeSize() > 0) {
            Node next = body.childNode(0);

            do {
                if (!(next instanceof TextNode
                        && ((TextNode) next).isBlank())) {
                    break;
                }
            } while ((next = next.nextSibling()) != null);

            Element lastParagraph = inlineTextToParagraph(next);

            if (lastParagraph != null) {
                body.prependChild(lastParagraph);
            }
        }

        for (Element paragraph : body.getAllElements()) {
            if (!paragraph.isBlock()) {
                continue;
            }

            Node next = paragraph;

            while ((next = next.nextSibling()) != null) {
                if (!(next instanceof TextNode
                        && ((TextNode) next).isBlank())) {
                    break;
                }
            }

            Element lastParagraph = inlineTextToParagraph(next);

            if (lastParagraph != null) {
                paragraph.after(lastParagraph);
            }
        }

        // Convert '<div>text<div><div><br></div>' to '<p>text</p>'
        List<Element> divs = new ArrayList<>();

        DIV:
        for (Element div : body.getElementsByTag(DIV_TAG.getName())) {
            Element brDiv = nextTag(DIV_TAG, div);

            if (brDiv == null) {
                continue;
            }

            // '<div><br></div>'?
            boolean sawBr = false;

            for (Node child : brDiv.childNodes()) {
                if (child instanceof TextNode) {
                    if (!((TextNode) child).isBlank()) {
                        continue DIV;
                    }

                } else if (child instanceof Element
                        && BR_TAG.equals(((Element) child).tag())) {

                    if (sawBr) {
                        continue DIV;

                    } else {
                        sawBr = true;
                    }

                } else {
                    continue DIV;
                }
            }

            divs.add(div);
            div.tagName(tag.getName());
            brDiv.remove();
        }

        for (Element div : divs) {
            div = nextTag(DIV_TAG, div);

            if (div != null) {
                div.tagName(tag.getName());
            }
        }

        // Unwrap nested '<p>'s.
        for (Element paragraph : body.getElementsByTag(tag.getName())) {
            if (paragraph.getElementsByTag(tag.getName()).size() > 1) {
                paragraph.unwrap();
            }
        }

        Map<String, ObjectType> tagTypes = new HashMap<>(RichTextElement.getConcreteTagTypes());
        tagTypes.put(ReferenceRichTextElement.TAG_NAME, ObjectType.getInstance(ReferenceRichTextElement.class));

        // <p>before [enh] after</p> -> <p>before</p> [enh] <p>after</p>
        for (Element enhancement : tagTypes.keySet()
                .stream()
                .map(body::getElementsByTag)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(Elements::new))) {

            ObjectType tagType = tagTypes.get(enhancement.tagName());

            if (tagType != null) {
                Class<?> tagClass = tagType.getObjectClass();

                if (tagClass != null) {
                    RichTextElement.Tag rteTag = tagClass.getAnnotation(RichTextElement.Tag.class);

                    if (rteTag != null && !rteTag.block()) {
                        continue;
                    }
                }
            }

            Element paragraph = enhancement.parent();

            if (tag.equals(paragraph.tag())) {
                Element before = new Element(tag, "");
                List<Node> beforeChildren = new ArrayList<>();

                for (Node previous = enhancement.previousSibling();
                         previous != null;
                         previous = previous.previousSibling()) {

                    beforeChildren.add(previous);
                }

                for (int i = beforeChildren.size() - 1; i >= 0; --i) {
                    before.appendChild(beforeChildren.get(i));
                }

                if (!before.childNodes().isEmpty()) {
                    before.attributes().addAll(paragraph.attributes());
                    paragraph.before(before);
                }

                paragraph.before(enhancement);
            }
        }
    }

    // Find the closest next tag without any intervening content.
    private Element nextTag(Tag tag, Element current) {
        Element nextTag = null;

        for (Node nextNode = current;
                 (nextNode = nextNode.nextSibling()) != null;) {

            if (nextNode instanceof Element) {
                Element nextElement = (Element) nextNode;

                if (tag.equals(nextElement.tag())) {
                    nextTag = nextElement;
                }

                break;

            } else if (nextNode instanceof TextNode
                    && !((TextNode) nextNode).isBlank()) {
                break;
            }
        }

        return nextTag;
    }

    private Element inlineTextToParagraph(Node next) {
        if (next == null) {
            return null;
        }

        List<Node> paragraphChildren = new ArrayList<>();

        do {
            if (next instanceof Element
                    && ((Element) next).isBlock()) {
                break;

            } else {
                paragraphChildren.add(next);
            }
        } while ((next = next.nextSibling()) != null);

        if (paragraphChildren.isEmpty()) {
            return null;
        }

        Element lastParagraph = new Element(tag, "");

        for (Node child : paragraphChildren) {
            child.remove();
            lastParagraph.appendChild(child.clone());
        }

        return lastParagraph;
    }
}
