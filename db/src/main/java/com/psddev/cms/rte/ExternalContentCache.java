package com.psddev.cms.rte;

import com.psddev.cms.db.ExternalContent;
import com.psddev.cms.db.ExternalContentProvider;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;
import com.psddev.dari.util.TypeReference;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Cache that stores external content responses in the database in order to
 * minimize the number of API calls.
 *
 * @see <a href="http://oembed.com/">oEmbed Specification</a>
 */
public class ExternalContentCache extends Record {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalContentCache.class);

    private long created;
    private Map<String, Object> response;

    /**
     * Returns a response that can be used to render the given {@code url} and
     * should fit within the given {@code maximumWidth} and
     * {@code maximumHeight}.
     *
     * @param url Nullable.
     * @param maximumWidth Nullable.
     * @param maximumHeight Nullable.
     * @return Nullable.
     */
    public static Map<String, Object> get(String url, Integer maximumWidth, Integer maximumHeight) {
        if (url == null) {
            return null;
        }

        // Cache key based on all the arguments.
        String key = url + ":" + maximumWidth + ":" + maximumHeight;
        UUID id = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));

        // Already cached?
        ExternalContentCache cache = Query.from(ExternalContentCache.class).where("_id = ?", id).first();
        long now = Database.Static.getDefault().now();

        if (cache != null) {
            Map<String, Object> response = cache.response;
            Long cacheAge = null;

            // Response contains suggested cache lifetime in seconds?
            if (response != null) {
                cacheAge = ObjectUtils.to(Long.class, response.get("cache_age"));
            }

            // Nope, default to 1 hour.
            if (cacheAge == null) {
                cacheAge = TimeUnit.HOURS.toSeconds(1);
            }

            // Not expired yet.
            if (cache.created + TimeUnit.SECONDS.toMillis(cacheAge) > now) {
                return response;
            }
        }

        // See if there's ExternalContentProvider set up to handle the URL.
        Map<String, Object> response = null;
        ExternalContent content = new ExternalContent();

        content.setUrl(url);
        content.setMaximumWidth(maximumWidth);
        content.setMaximumHeight(maximumHeight);

        for (Class<? extends ExternalContentProvider> providerClass : ClassFinder.findConcreteClasses(ExternalContentProvider.class)) {
            ExternalContentProvider provider = TypeDefinition.getInstance(providerClass).newInstance();
            response = provider.createResponse(content);

            if (response != null) {
                break;
            }
        }

        // If not, try looking for oEmbed information.
        if (response == null) {
            try {
                for (Element link : Jsoup.connect(url).get().select("link[type=application/json+oembed]")) {
                    String oEmbedUrl = link.attr("href");

                    if (!ObjectUtils.isBlank(oEmbedUrl)) {
                        response = ObjectUtils.to(
                                new TypeReference<Map<String, Object>>() {
                                },
                                ObjectUtils.fromJson(IoUtils.toString(new URL(
                                        StringUtils.addQueryParameters(oEmbedUrl,
                                                "maxwidth", maximumWidth,
                                                "maxheight", maximumHeight)), 5000)));

                        if (response != null) {
                            break;
                        }
                    }
                }

            } catch (IOException error) {
                LOGGER.warn(
                        String.format("Can't download from [%s] to get the oEmbed URL!", url),
                        error);
            }
        }

        // Legacy ExternalContent support.
        if (response != null) {
            response.put("_url", url);
            response.put("_maximumWidth", maximumWidth);
            response.put("_maximumHeight", maximumHeight);
        }

        // Save the cache.
        cache = new ExternalContentCache();

        cache.getState().setId(id);
        cache.created = now;
        cache.response = response;
        cache.saveImmediately();

        return response;
    }
}
