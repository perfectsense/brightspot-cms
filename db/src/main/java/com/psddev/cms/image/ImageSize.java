package com.psddev.cms.image;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.psddev.cms.db.ImageTag;
import com.psddev.cms.view.ViewModel;
import com.psddev.dari.util.DimsImageEditor;
import com.psddev.dari.util.ImageEditor;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.ThreadLocalStack;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Image sizing information.
 */
public final class ImageSize {

    private static final ThreadLocalStack<ImageSizeProvider> PROVIDER = new ThreadLocalStack<>();
    private static final ThreadLocalStack<String> FIELD = new ThreadLocalStack<>();
    private static final ThreadLocal<List<String>> CONTEXTS = ThreadLocal.withInitial(ArrayList::new);

    private final String group;
    private final String internalName;
    private final String displayName;
    private final int width;
    private final int height;
    private final String format;
    private final int quality;

    /**
     * Returns an instance most appropriate for use with the given
     * {@code field} in the current contexts.
     *
     * @param field Nullable.
     * @return Nullable.
     */
    public static ImageSize getInstance(String field) {
        ImageSizeProvider provider = PROVIDER.get();
        return provider != null ? provider.get(CONTEXTS.get(), field) : null;
    }

    /**
     * Returns a URL to the given {@code image}, sized most appropriately for
     * use with the given {@code field} in the current contexts.
     *
     * @param image Nullable.
     * @param field Nullable.
     * @return Nullable.
     */
    public static String getUrlForField(StorageItem image, String field) {
        if (image == null) {
            return null;
        }

        ImageSize size = getInstance(field);

        if (size != null) {
            ImageEditor editor = ImageEditor.Static.getDefault();

            if (editor instanceof DimsImageEditor) {
                return new DimsImageUrl((DimsImageEditor) editor, image, size).toUrl();

            } else {
                return new ImageTag.Builder(image)
                        .setWidth(size.getWidth())
                        .setHeight(size.getHeight())
                        .toAttributes()
                        .get("src");
            }

        } else {
            return image.getPublicUrl();
        }
    }

    /**
     * Returns a URL to the given {@code image}, sized most appropriate for
     * use in the current contexts.
     *
     * @param image Nullable.
     * @return Nullable.
     */
    public static String getUrl(StorageItem image) {
        if (image == null) {
            return null;
        }

        String field = FIELD.get();

        if (field == null) {
            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                Class<?> c = ObjectUtils.getClassByName(ste.getClassName());

                if (c != null && ViewModel.class.isAssignableFrom(c)) {
                    field = ImageSizeEnhancer.toField(ste.getMethodName());

                    if (field != null) {
                        break;
                    }
                }
            }
        }

        return field != null
                ? getUrlForField(image, field)
                : image.getPublicUrl();
    }

    /**
     * Returns the stack that points to the current image size provider.
     *
     * @return Nonnull.
     */
    public static ThreadLocalStack<ImageSizeProvider> getProviderStack() {
        return PROVIDER;
    }

    /**
     * Returns the stack that points to the current field.
     *
     * @return Nonnull.
     */
    public static ThreadLocalStack<String> getFieldStack() {
        return FIELD;
    }

    /**
     * Adds the given {@code context}.
     *
     * @param context Nullable.
     */
    public static void pushContext(String context) {
        CONTEXTS.get().add(context);
    }

    /**
     * Removes the lastly added context.
     */
    public static void popContext() {
        List<String> contexts = CONTEXTS.get();

        if (!contexts.isEmpty()) {
            contexts.remove(contexts.size() - 1);
        }
    }

    /**
     * Clears all contexts.
     */
    public static void clearContexts() {
        CONTEXTS.remove();
    }

    /**
     * Returns a builder that can be used to create an instance.
     *
     * @return Nonnull.
     */
    public static ImageSizeBuilder builder() {
        return new ImageSizeBuilder();
    }

    ImageSize(
            String group,
            String internalName,
            String displayName,
            int width,
            int height,
            String format,
            int quality) {

        Preconditions.checkNotNull(internalName);

        this.group = group;
        this.internalName = internalName;
        this.displayName = !StringUtils.isBlank(displayName) ? displayName : internalName;
        this.width = width;
        this.height = height;
        this.format = format;
        this.quality = quality;
    }

    public String getGroup() {
        return group;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getFormat() {
        return format;
    }

    public int getQuality() {
        return quality;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;

        } else if (other instanceof ImageSize) {
            ImageSize otherSize = (ImageSize) other;
            return getInternalName().equals(otherSize.getInternalName());

        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCode(width, height);
    }

    @Override
    public String toString() {
        return MoreObjects
                .toStringHelper(this)
                .add("group", getGroup())
                .add("internalName", getInternalName())
                .add("displayName", getDisplayName())
                .add("width", getWidth())
                .add("height", getHeight())
                .add("format", getFormat())
                .add("quality", getQuality())
                .toString();
    }
}
