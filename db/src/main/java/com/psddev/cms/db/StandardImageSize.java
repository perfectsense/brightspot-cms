package com.psddev.cms.db;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
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

    private static final String HIDDEN_FROM_UI_VALIDATION_ERROR_MESSAGE = "'Independent' and 'Hide From UI' fields cannot be checked at the same time.";

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

    @ToolUi.Note("Check to hide this standard image size from UI.")
    @DisplayName("Hide From UI")
    private Boolean hiddenFromUI;

    private CropOption cropOption;
    private ResizeOption resizeOption;

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

    public boolean isHiddenFromUI() {
        return Boolean.TRUE.equals(hiddenFromUI);
    }

    public void setHiddenFromUI(Boolean hiddenFromUI) {
        this.hiddenFromUI = hiddenFromUI;
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
    protected void onValidate() {
        super.onValidate();
        if (isIndependent() && isHiddenFromUI()) {
            State state = getState();
            state.addError(state.getField("independent"), HIDDEN_FROM_UI_VALIDATION_ERROR_MESSAGE);
            state.addError(state.getField("hiddenFromUI"), HIDDEN_FROM_UI_VALIDATION_ERROR_MESSAGE);
        }
    }
}
