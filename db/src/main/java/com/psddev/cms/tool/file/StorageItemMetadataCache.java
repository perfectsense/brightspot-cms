package com.psddev.cms.tool.file;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.AggregateException;
import com.psddev.dari.util.ImageMetadataMap;
import com.psddev.dari.util.StorageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cache that stores storage item metadata in the database in order to minimize
 * the number of data downloads.
 */
public class StorageItemMetadataCache extends Record {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageItemMetadataCache.class);

    private static final LoadingCache<UUID, Deleter> DELETERS = CacheBuilder.newBuilder()
            .build(new CacheLoader<UUID, Deleter>() {

                @Override
                @ParametersAreNonnullByDefault
                public Deleter load(UUID cacheId) {
                    Deleter deleter = new Deleter(cacheId);
                    Thread deleterThread = new Thread(deleter);

                    deleterThread.setDaemon(true);
                    deleterThread.start();

                    return deleter;
                }
            });

    @Indexed(unique = true)
    @Required
    private String key;

    private Map<String, Object> metadata;

    /**
     * Updates the given {@code item}'s metadata.
     *
     * @param item Can't be {@code null}.
     */
    public static void update(StorageItem item) {
        String path = item.getPath();

        if (path == null) {
            return;
        }

        String key = item.getStorage() + ":" + path;
        Map<String, Object> itemMetadata = item.getMetadata();
        StorageItemMetadataCache cache = Query.from(StorageItemMetadataCache.class)
                .where("key = ?", key)
                .first();

        if (cache == null) {
            cache = new StorageItemMetadataCache();
            cache.key = key;

            if (!itemMetadata.containsKey("width")
                    && !itemMetadata.containsKey("height")) {

                String contentType = item.getContentType();

                if (contentType != null
                        && contentType.startsWith("image/")) {

                    try (InputStream imageInput = item.getData()) {
                        ImageMetadataMap imageMetadata = new ImageMetadataMap(imageInput);
                        List<Throwable> errors = imageMetadata.getErrors();

                        if (errors.isEmpty()) {
                            cache.metadata = imageMetadata;

                        } else {
                            LOGGER.info("Can't read image metadata!", new AggregateException(errors));
                        }

                    } catch (IOException error) {
                        LOGGER.info("Can't read image!", error);
                    }
                }
            }

            cache.saveImmediately();
        }

        DELETERS.getUnchecked(cache.getId()).extend();

        if (cache.metadata != null) {
            itemMetadata.putAll(cache.metadata);
        }
    }

    private static class Deleter implements Runnable {

        private final UUID cacheId;
        private volatile long triggerTime;

        public Deleter(UUID cacheId) {
            this.cacheId = cacheId;

            extend();
        }

        public void extend() {
            triggerTime = System.currentTimeMillis() + 10000;
        }

        @Override
        public void run() {
            while (triggerTime > System.currentTimeMillis()) {
                try {
                    Thread.sleep(1000);

                } catch (InterruptedException error) {
                    break;
                }
            }

            Query.fromAll()
                    .where("_id = ?", cacheId)
                    .deleteAll();
        }
    }
}
