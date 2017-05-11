package com.psddev.cms.db;

import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.PeriodicValue;
import com.psddev.dari.util.PullThroughValue;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Represents a standard image size. */
@ToolUi.IconName("object-standardImageSize")
@Record.BootstrapPackages("Standard Image Sizes")
public class StandardImageSize extends Record {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardImageSize.class);

    private static final PullThroughValue<PeriodicValue<List<StandardImageSize>>>
            ALL = new PullThroughValue<PeriodicValue<List<StandardImageSize>>>() {

        @Override
        protected PeriodicValue<List<StandardImageSize>> produce() {
            return new PeriodicValue<List<StandardImageSize>>() {

                @Override
                protected List<StandardImageSize> update() {

                    Query<StandardImageSize> query = Query.from(StandardImageSize.class).sortAscending("displayName");
                    Date cacheUpdate = getUpdateDate();
                    Date databaseUpdate = query.lastUpdate();
                    if (databaseUpdate == null || (cacheUpdate != null && !databaseUpdate.after(cacheUpdate))) {
                        List<StandardImageSize> sizes = get();
                        return sizes != null ? sizes : Collections.<StandardImageSize>emptyList();
                    }

                    LOGGER.info("Loading image sizes");
                    return query.selectAll();
                }
            };
        }
    };

    @Indexed(unique = true)
    @Required
    private String displayName;

    @Indexed(unique = true)
    @Required
    private String internalName;

    private int width;
    private int height;

    @ToolUi.Note("Check to prevent this standard image size from merging with others in the image editor.")
    private boolean independent;

    private CropOption cropOption;
    private ResizeOption resizeOption;

    private Clip clip;

    /** Returns a list of all the image sizes. */
    public static List<StandardImageSize> findAll() {
        return ALL.get().get();
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

    public Clip getClip() {
        return clip;
    }

    public void setClip(Clip clip) {
        this.clip = clip;
    }

    @Embedded
    public abstract static class Clip extends Record {
    }

    @Embedded
    public static class Circle extends Clip {
    }

    @Embedded
    public static class Star extends Clip {
    }

    @Embedded
    public static class StarBurst extends Clip {
        @ToolUi.Note("Defaults to 5")
        private Integer size;
        @ToolUi.Note("Defaults to 30")
        private Integer points;

        public Integer getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public Integer getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }

        public String getValue() {
            if (size != null || points != null) {
                if (size != null && points != null) {
                    return String.format("%sx%s", getSize(), getPoints());
                } else if (size != null) {
                    return String.format("%s", getSize());
                } else {
                    return String.format("x%s", getPoints());
                }
            }
            return null;
        }

        @Override
        public String getLabel() {
            if (points == null && size == null) {
                return "";
            } else {
                return String.format("%s x %s", points == null ? "" : points, size == null ? "" : size);
            }
        }
    }
}
