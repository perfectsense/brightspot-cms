package com.psddev.cms.rte;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.RichTextElement;
import com.psddev.cms.tool.ToolPageContext;

import java.io.IOException;
import java.util.Map;

@ExternalContentRichTextElement.DisplayName("External Content")
@RichTextElement.Tag(value = "brightspot-cms-external-content", block = true, preview = true, readOnly = true)
public class ExternalContentRichTextElement extends RichTextElement {

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void fromAttributes(Map<String, String> attributes) {
        if (attributes != null) {
            setUrl(attributes.get("url"));
        }
    }

    @Override
    public Map<String, String> toAttributes() {
        return ImmutableMap.of("url", getUrl());
    }

    @Override
    public void fromBody(String body) {
        setUrl(body);
    }

    @Override
    public String toBody() {
        return getUrl();
    }

    @Override
    public void writePreviewHtml(ToolPageContext page) throws IOException {
        page.writeStart("iframe",
                "class", "ExternalPreviewFrame",
                "src", page.cmsUrl("/content/externalPreviewFrame", "url", getUrl()));
        page.writeEnd();
    }
}
