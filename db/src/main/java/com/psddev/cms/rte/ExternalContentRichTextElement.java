package com.psddev.cms.rte;

import com.psddev.cms.db.ExternalContent;
import com.psddev.cms.db.RichTextElement;
import com.psddev.cms.db.RichTextViewBuilder;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.view.ViewBinding;
import com.psddev.dari.util.ObjectUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

@ViewBinding(value = ExternalContentRichTextElementViewModel.class, types = RichTextViewBuilder.RICH_TEXT_ELEMENT_VIEW_TYPE)

@ExternalContentRichTextElement.DisplayName("External Content")
@RichTextElement.Tag(value = "brightspot-cms-external-content", initialBody = "...", block = true, preview = true, readOnly = true)
public class ExternalContentRichTextElement extends RichTextElement {

    @Required
    @Embedded
    private ExternalContent content;

    public ExternalContent getContent() {
        return content;
    }

    public void setContent(ExternalContent content) {
        this.content = content;
    }

    public String getUrl() {
        ExternalContent content = getContent();
        return content != null ? content.getUrl() : null;
    }

    @Override
    public void fromAttributes(Map<String, String> attributes) {
        if (attributes != null) {

            String url = attributes.get("url");
            String contentJsonString = attributes.get("content");

            if (url != null || contentJsonString != null) {
                ExternalContent content = new ExternalContent();

                if (url != null && contentJsonString == null) {
                    content.setUrl(url);

                } else {
                    Object contentJsonObject = null;
                    try {
                        contentJsonObject = ObjectUtils.fromJson(contentJsonString);
                    } catch (RuntimeException e) {
                        // Couldn't parse it as JSON. The data might have been tampered with. Log it and move on.
                        if (Logger.getLogger(getClass()).isDebugEnabled()) {
                            Logger.getLogger(getClass()).debug(e.getMessage(), e);
                        }
                    }

                    if (contentJsonObject instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> contentJsonMap = (Map<String, Object>) contentJsonObject;

                        content.getState().putAll(contentJsonMap);
                    }
                }

                setContent(content);
            }
        }
    }

    @Override
    public Map<String, String> toAttributes() {

        Map<String, String> attributes = new LinkedHashMap<>();

        ExternalContent content = getContent();
        if (content == null) {
            if (url != null) {
                content = new ExternalContent();
                content.setUrl(url);
            }
        }

        if (content != null) {
            // Make sure the response is fetched and cached.
            content.getResponse();

            Map<String, Object> contentState = content.getState().getSimpleValues();
            attributes.put("content", ObjectUtils.toJson(contentState));
        }

        return attributes;
    }

    @Override
    public void fromBody(String body) {
        setUrl("...".equals(body) ? null : body);
    }

    @Override
    public String toBody() {
        return ObjectUtils.firstNonBlank(getUrl(), "...");
    }

    @Override
    public void writePreviewHtml(ToolPageContext page) throws IOException {
        page.writeStart("iframe",
                "class", "ExternalPreviewFrame",
                "src", page.cmsUrl("/content/externalPreviewFrame", "url", getUrl()));
        page.writeEnd();
    }

    @Deprecated
    private String url;

    /**
     * @deprecated Use {@link #setContent(ExternalContent)} instead.
     */
    @Deprecated
    public void setUrl(String url) {
        this.url = url;
    }
}
