package com.psddev.cms.db;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;
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
        String url = getUrl();
        Integer width = getMaximumWidth();
        Integer height = getMaximumHeight();

        if (!ObjectUtils.isBlank(url)
                && (response == null
                || !ObjectUtils.equals(url, response.get("_url"))
                || !ObjectUtils.equals(width, ObjectUtils.to(Integer.class, response.get("_maximumWidth")))
                || !ObjectUtils.equals(height, ObjectUtils.to(Integer.class, response.get("_maximumHeight"))))) {

            for (Class<? extends ExternalContentProvider> providerClass : ClassFinder.Static.findClasses(ExternalContentProvider.class)) {
                if (providerClass.isInterface()
                        || Modifier.isAbstract(providerClass.getModifiers())) {
                    continue;
                }

                ExternalContentProvider provider = TypeDefinition.getInstance(providerClass).newInstance();
                Map<String, Object> newResponse = provider.createResponse(this);

                if (newResponse != null) {
                    newResponse.put("_url", url);
                    newResponse.put("_maximumWidth", width);
                    newResponse.put("_maximumHeight", height);

                    response = newResponse;

                    return response;
                }
            }

            try {
                for (Element link : getOrCreateDocument().select("link[type=application/json+oembed]")) {
                    String oEmbedUrl = link.attr("href");

                    if (!ObjectUtils.isBlank(oEmbedUrl)) {
                        getResponseByOEmbedUrl(oEmbedUrl);
                        break;
                    }
                }

            } catch (IOException error) {
                error.printStackTrace();
            }
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
    protected void beforeSave() {
        super.beforeSave();
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
