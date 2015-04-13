package com.psddev.cms.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;

/** Represents a standard image size. */
@ToolUi.IconName("object-standardImageSize")
@Record.BootstrapPackages("Standard Image Sizes")
public class StandardImageSize extends Record {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardImageSize.class);

    private static final LoadingCache<Optional<UUID>, Map<String, StandardImageSize>>
        IMAGE_SIZES = CacheBuilder.newBuilder().refreshAfterWrite(30, TimeUnit.SECONDS).build(new CacheLoader<Optional<UUID>, Map<String, StandardImageSize>>() {

        @Override
        public Map<String, StandardImageSize> load(Optional<UUID> uuid) throws Exception {
            Query<StandardImageSize> query = Query.from(StandardImageSize.class);

            if (!uuid.isPresent()) {
                query.and(Site.CONSUMERS_FIELD + " is missing || " + Site.IS_GLOBAL_FIELD + " = true");
            } else {
                query.and(Site.CONSUMERS_FIELD + " = ?", uuid.get());
            }

            List<StandardImageSize> sizes = query.selectAll();
            Map<String, StandardImageSize> map = new HashMap<>();

            if (!ObjectUtils.isBlank(sizes)) {
                sizes.forEach(size -> map.put(size.getInternalName(), size));
            }

            return map;
        }
    });

    @Indexed
    @Required
    private String displayName;

    @Indexed
    @Required
    private String internalName;

    private int width;
    private int height;

    @ToolUi.Note("Check to prevent this standard image size from merging with others in the image editor.")
    private boolean independent;

    private CropOption cropOption;
    private ResizeOption resizeOption;

    public static List<StandardImageSize> findAll() {
        try {
            return new ArrayList<>(IMAGE_SIZES.get(Optional.absent()).values());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static StandardImageSize findByInternalName(String internalName) {
        return findByInternalName(null, internalName);
    }

    public static StandardImageSize findByInternalName(Site site, String internalName) {

        StandardImageSize size = null;

        try {
            Map<String, StandardImageSize> sizes = IMAGE_SIZES.get(site != null ? Optional.of(site.getId()) : Optional.absent());

            if (!ObjectUtils.isBlank(sizes)) {
                size = sizes.get(internalName);
            }

            //fall back to global image sizes
            if (site != null && size == null) {

                sizes = IMAGE_SIZES.get(null);

                if (!ObjectUtils.isBlank(sizes)) {
                    size = sizes.get(internalName);
                }
            }

        } catch (Exception e) {
            //ignore
        }

        return size;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isIndependent() {
        return independent;
    }

    public void setIndependent(boolean independent) {
        this.independent = independent;
    }

    public CropOption getCropOption() {
        return cropOption;
    }

    public void setCropOption(CropOption cropOption) {
        this.cropOption = cropOption;
    }

    public ResizeOption getResizeOption() {
        return resizeOption;
    }

    public void setResizeOption(ResizeOption resizeOption) {
        this.resizeOption = resizeOption;
    }

    @Override
    public void beforeSave() {

        Set<Site> consumers = this.as(Site.ObjectModification.class).getConsumers();
        State state = this.getState();
        StandardImageSize duplicate;

        Query<StandardImageSize> baseQuery = Query.from(StandardImageSize.class).where("id != ?", this.getId());
        Predicate internalNamePredicate = PredicateParser.Static.parse("internalName = ?", this.getInternalName());
        Predicate displayNamePredicate = PredicateParser.Static.parse("displayName = ?", this.getDisplayName());

        if (ObjectUtils.isBlank(consumers)) {

            Query<StandardImageSize> globalDuplicateQuery = baseQuery.clone().and(Site.CONSUMERS_FIELD + " is missing");
            duplicate = globalDuplicateQuery.clone().and(internalNamePredicate).first();

            if (duplicate != null) {
                state.addError(state.getField("internalName"), "Must be unique, duplicate found at " + duplicate.getId());
            }

            duplicate = globalDuplicateQuery.clone().and(displayNamePredicate).first();

            if (duplicate != null) {
                state.addError(state.getField("displayName"), "Must be unique, duplicate found at " + duplicate.getId());
            }

            return;
        }

        Query<StandardImageSize> siteDuplicateQuery = baseQuery.clone().and(Site.CONSUMERS_FIELD + " = ?", consumers);
        duplicate = siteDuplicateQuery.clone().and(internalNamePredicate).first();

        if (duplicate != null) {
            this.getState().addError(state.getField("internalName"), "Must be unique per site, but duplicate found at " + duplicate.getId());
        }

        duplicate = siteDuplicateQuery.clone().and(displayNamePredicate).first();

        if (duplicate != null) {
            this.getState().addError(state.getField("displayName"), "Must be unique per site, but duplicate found at " + duplicate.getId());
        }
    }
}
