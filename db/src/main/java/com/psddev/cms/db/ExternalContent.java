package com.psddev.cms.db;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.psddev.cms.rte.ExternalContentCache;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeReference;

/**
 * @see <a href="http://oembed.com/">oEmbed Specification</a>
 */
@ToolUi.Referenceable
public class ExternalContent extends Content implements Renderer {

    @Required
    @ToolUi.NoteHtml("<a class=\"icon icon-action-preview\" target=\"contentExternalPreview\" onclick=\"this.href = CONTEXT_PATH + '/content/externalPreview?url=' + encodeURIComponent($(this).closest('.inputContainer').find('> .inputSmall > textarea').val() || ''); return true;\">Preview</a>")
    private String url;
    private transient Document document;

    private Integer maximumWidth;
    private Integer maximumHeight;

    @ToolUi.Hidden
    private Map<String, Object> response;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        this.document = null;
    }

    /**
     * Returns a <a href="http://jsoup.org/">jsoup</a> {@link Document}
     * associated with the URL, downloading it if necessary.
     *
     * @return Never {@code null}.
     */
    public Document getOrCreateDocument() throws IOException {
        if (document == null) {
            document = Jsoup.connect(url).get();
        }
        return document;
    }

    public Integer getMaximumWidth() {
        return maximumWidth;
    }

    public void setMaximumWidth(Integer maximumWidth) {
        this.maximumWidth = maximumWidth;
    }

    public Integer getMaximumHeight() {
        return maximumHeight;
    }

    public void setMaximumHeight(Integer maximumHeight) {
        this.maximumHeight = maximumHeight;
    }

    public Map<String, Object> getResponse() {
        if (response == null) {
            response = ExternalContentCache.get(getUrl(), getMaximumWidth(), getMaximumHeight());
        }

        return response;
    }

    public Map<String, Object> getResponseByOEmbedUrl(String oEmbedUrl) {
        try {
            Integer width = getMaximumWidth();
            Integer height = getMaximumHeight();
            Map<String, Object> newResponse = ObjectUtils.to(
                    new TypeReference<Map<String, Object>>() { },
                    ObjectUtils.fromJson(IoUtils.toString(new URL(
                            StringUtils.addQueryParameters(oEmbedUrl,
                            "maxwidth", width,
                            "maxheight", height)))));

            newResponse.put("_url", url);
            newResponse.put("_maximumWidth", width);
            newResponse.put("_maximumHeight", height);
            response = newResponse;

        } catch (IOException error) {
            error.printStackTrace();
        }

        return response;
    }

    public void setResponse(Map<String, Object> response) {
        this.response = response;
    }

    @Override
    public String getLabel() {
        String url = getUrl();

        if (response != null) {
            String title = ObjectUtils.to(String.class, response.get("title"));

            if (!ObjectUtils.isBlank(title)) {
                StringBuilder label = new StringBuilder();

                try {
                    String host = new URL(url).getHost();

                    if (!ObjectUtils.isBlank(host)) {
                        label.append("(");
                        label.append(host);
                        label.append(") ");
                    }

                } catch (MalformedURLException error) {
                    // Not a valid URL, but that's OK.
                }

                label.append(title);
                return label.toString();
            }
        }

        return url;
    }

    @Override
    protected void beforeCommit() {
        super.beforeCommit();
        getResponse();
    }

    @Override
    public void renderObject(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            HtmlWriter writer)
            throws IOException {
        Map<String, Object> response = getResponse();
        if (!ObjectUtils.isBlank(response)) {

            Object type = response.get("type");

            if ("photo".equals(type)) {
                writer.writeElement("img",
                        "src", response.get("url"),
                        "width", response.get("width"),
                        "height", response.get("height"),
                        "alt", response.get("title"));

            } else if ("video".equals(type)) {
                writer.writeRaw(response.get("html"));

            } else if ("link".equals(type)) {
                writer.writeStart("a",
                        "href", response.get("_url"));
                    writer.writeHtml(response.get("title"));
                writer.writeEnd();

            } else if ("rich".equals(type)) {
                writer.writeRaw(response.get("html"));
            }
        }
    }
}
