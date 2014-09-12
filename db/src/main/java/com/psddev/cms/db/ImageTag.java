package com.psddev.cms.db;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.TagSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CollectionUtils;
import com.psddev.dari.util.ImageEditor;
import com.psddev.dari.util.ImageResizeStorageItemListener;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeReference;
import com.psddev.dari.util.WebPageContext;

/**
 * Equivalent to the HTML {@code img} tag where its {@code src} attribute
 * may be set to a URL or a StorageItem object.
 */
@SuppressWarnings("serial")
public class ImageTag extends TagSupport implements DynamicAttributes {
    public static final String HOTSPOT_CLASS = "com.psddev.image.HotSpots";

    protected static final Logger LOGGER = LoggerFactory.getLogger(ImageTag.class);
    protected static final String ORIGINAL_WIDTH_METADATA_PATH = "image/originalWidth";
    protected static final String ORIGINAL_HEIGHT_METADATA_PATH = "image/originalHeight";

    protected Builder tagBuilder = new Builder();
    private static Boolean useHotSpotCrop;

    private static boolean useHotSpotCrop() {
        if (useHotSpotCrop == null) {
            if (ObjectUtils.getClassByName(HOTSPOT_CLASS) != null) {
                useHotSpotCrop = Settings.getOrDefault(Boolean.class, "cms/image/useHotSpotCrop", Boolean.TRUE);
            } else {
                useHotSpotCrop = false;
            }
        }
        return useHotSpotCrop;
    }

    /**
     * Sets the source object, which may be either a URL or a Dari
     * object.
     */
    public void setSrc(Object src) {
        WebPageContext wp = new WebPageContext(pageContext);

        if (src instanceof String ||
                src instanceof URI ||
                src instanceof URL) {

            String path = JspUtils.resolvePath(wp.getServletContext(), wp.getRequest(), src.toString());
            StorageItem pathItem;
            if (path.startsWith("/")) {
                pathItem = StorageItem.Static.createUrl(JspUtils.getAbsoluteUrl(wp.getRequest(), path));
            } else {
                pathItem = StorageItem.Static.createUrl(path);
            }
            tagBuilder.setItem(pathItem);

        } else if (src instanceof StorageItem) {
            StorageItem item = (StorageItem) src;
            //Resets StorageItem's MetaData size on subsequent calls to the same storage Item
            if (CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_HEIGHT_METADATA_PATH) != null) {
                item.getMetadata().put("height", CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_HEIGHT_METADATA_PATH));
            }
            if (CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_WIDTH_METADATA_PATH) != null) {
                item.getMetadata().put("width", CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_WIDTH_METADATA_PATH));
            }
            tagBuilder.setItem(item);

        } else if (src instanceof State || src instanceof Recordable) {

            // -- Hack to ensure backwards compatibility
            String field = (String) super.getValue("field");
            tagBuilder.setField(field);
            if (src instanceof State) {
                tagBuilder.setState((State) src);
            } else if (src instanceof Recordable) {
                tagBuilder.setRecordable((Recordable) src);
            }
            // -- End hack
        }
    }

    /**
     * Sets the field that contains the image. If not set, the first
     * field with {@value ObjectField.FILE_TYPE} type is used.
     * @deprecated No replacement
     */
    @Deprecated
    public void setField(String field) {
        tagBuilder.setField(field);
    }

    /**
     * Sets the name of the {@linkplain ImageEditor image editor}
     * to use.
     */
    public void setEditor(Object object) {
        ImageEditor editor = null;
        if (object instanceof ImageEditor) {
            editor = (ImageEditor) object;
        } else if (object instanceof String) {
            editor = ImageEditor.Static.getInstance((String) object);
        } else {
            editor = ImageEditor.Static.getDefault();
        }
        tagBuilder.setEditor(editor);
    }

    /**
     * Sets the internal name of the {@linkplain StandardImageSize
     * image size} to use.
     */
    public void setSize(Object size) {
        if (size instanceof StandardImageSize) {
            tagBuilder.setStandardImageSize((StandardImageSize) size);
        } else if (size instanceof String) {
            tagBuilder.setStandardImageSize(getStandardImageSizeByName((String) size));
        }
    }

    /**
     * Sets the width. Note that this will override the width provided
     * by the image size set with {@link #setSize(String)}.
     */
    public void setWidth(String width) {
        if (width != null && width.endsWith("px")) {
            width = width.substring(0, width.length() - 2);
        }
        tagBuilder.setWidth(ObjectUtils.to(Integer.class, width));
    }

    /**
     * Sets the height. Note that this will override the height provided
     * by the image size set with {@link #setSize(String)}.
     */
    public void setHeight(String height) {
        if (height != null && height.endsWith("px")) {
            height = height.substring(0, height.length() - 2);
        }
        tagBuilder.setHeight(ObjectUtils.to(Integer.class, height));
    }

    /**
     * Sets the crop option. Note that this will override the crop option
     * provided by the image size set with {@link #setSize(String)}.
     */
    public void setCropOption(Object cropOptionObject) {
        CropOption cropOption = null;
        if (cropOptionObject instanceof CropOption) {
            cropOption = (CropOption) cropOptionObject;
        } else if (cropOptionObject instanceof String) {
            cropOption = CropOption.Static.fromImageEditorOption((String) cropOptionObject);
        }
        tagBuilder.setCropOption(cropOption);
    }

    /**
     * Sets the resize option. Note that this will override the resize option
     * provided by the image size set with {@link #setSize(String)}.
     */
    public void setResizeOption(Object resizeOptionObject) {
        ResizeOption resizeOption = null;
        if (resizeOptionObject instanceof ResizeOption) {
            resizeOption = (ResizeOption) resizeOptionObject;
        } else if (resizeOptionObject instanceof String) {
            resizeOption = ResizeOption.Static.fromImageEditorOption((String) resizeOptionObject);
        }
        tagBuilder.setResizeOption(resizeOption);
    }

    public void setTagName(String tagName) {
        tagBuilder.setTagName(tagName);
    }

    /**
     * Overrides the default attribute (src) used to place the image URL. This
     * is usually used in the conjunction with lazy loading scripts that copy
     * the image URL from this attribute to the "src" attribute at some point
     * after the page has loaded.
     */
    public void setSrcAttr(String srcAttr) {
        tagBuilder.setSrcAttribute(srcAttr);
    }

    /**
     * When set to {@code true}, suppresses the "width" and "height" attributes
     * from the final HTML output.
     */
    public void setHideDimensions(Object hideDimensions) {
        if (ObjectUtils.to(boolean.class, hideDimensions)) {
            tagBuilder.hideDimensions();
        }
    }

    public void setOverlay(Object overlay) {
        tagBuilder.setOverlay(ObjectUtils.to(boolean.class, overlay));
    }

    /**
     * When set to {@code true}, suppresses automatic cropping using hot spots
     * @param disableHotSpotCrop
     */
    public void setDisableHotSpotCrop(Object disableHotSpotCrop) {
        tagBuilder.setDisableHotSpotCrop(ObjectUtils.to(boolean.class, disableHotSpotCrop));
    }

    // --- DynamicAttribute support ---

    @Override
    public void setDynamicAttribute(String uri, String localName, Object value) {
        tagBuilder.addAttribute(localName, value);
    }

    // --- TagSupport support ---

    @Override
    public int doStartTag() throws JspException {
        JspWriter writer = pageContext.getOut();
        try {
            writer.print(tagBuilder.toHtml());
        } catch (IOException e) {
            throw new JspException(e);
        } finally {
            tagBuilder.reset();
        }

        return SKIP_BODY;
    }

    private static final Set<String> VOID_ELEMENTS = new HashSet<String>(Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "keygen", "link", "meta", "param", "source", "track", "wbr"));

    private static String convertAttributesToHtml(String tagName, Map<String, String> attributes) {
        StringBuilder builder = new StringBuilder();
        if (!attributes.isEmpty()) {
            if (tagName == null) {
                tagName = "img";
            }
            builder.append("<");
            builder.append(tagName);
            for (Map.Entry<String, String> e : attributes.entrySet()) {
                String key = e.getKey();
                String value = e.getValue();
                if (!(ObjectUtils.isBlank(key) || ObjectUtils.isBlank(value))) {
                    builder.append(" ");
                    builder.append(StringUtils.escapeHtml(key));
                    builder.append("=\"");
                    builder.append(StringUtils.escapeHtml(value));
                    builder.append("\"");
                }
            }

            if (VOID_ELEMENTS.contains(tagName.toLowerCase(Locale.ENGLISH))) {
                if (Settings.get(boolean.class, "dari/selfClosingElements")) {
                    builder.append('/');
                }

                builder.append('>');

            } else {
                builder.append("></");
                builder.append(tagName);
                builder.append(">");
            }
        }
        return builder.toString();
    }

    /**
     * Finds the dimension {@code name} ("width", or "height") for the given
     * StorageItem {@code item}.
     */
    protected static Integer findDimension(StorageItem item, String name) {
        if (item == null) {
            return null;
        }
        Integer dimension = null;
        Map<String, Object> metadata = item.getMetadata();
        if (metadata != null) {
            dimension = ObjectUtils.to(Integer.class, metadata.get(name));
            if (dimension == null || dimension == 0) {
                dimension = null;
            }
        }
        return dimension;
    }

    /**
     * Finds the crop information for the StorageItem {@code item}.
     */
    protected static Map<String, ImageCrop> findImageCrops(StorageItem item) {
        if (item == null) {
            return null;
        }
        Map<String, ImageCrop> crops = null;
        Map<String, Object> metadata = item.getMetadata();
        if (metadata != null) {
            crops = ObjectUtils.to(new TypeReference<Map<String, ImageCrop>>() { }, metadata.get("cms.crops"));
        }
        if (crops == null) {
            crops = new HashMap<String, ImageCrop>();
        }
        return crops;
    }

    /**
     * Finds the StorageItem that best matches the provided size. This works
     * in conjunction with the ImageResizeStorageItemListener class to use a
     * presized image that is smaller than the original image in an effort to
     * improve resize performance.
     */
    private static StorageItem findStorageItemForSize(StorageItem item, Integer width, Integer height) {
        if (width == null || height == null) {
            return item;
        }

        StorageItem override = StorageItem.Static.createIn(item.getStorage());
                new ObjectMap(override).putAll(new ObjectMap(item));
        CollectionUtils.putByPath(override.getMetadata(), ORIGINAL_WIDTH_METADATA_PATH, ImageTag.findDimension(item, "width"));
        CollectionUtils.putByPath(override.getMetadata(), ORIGINAL_HEIGHT_METADATA_PATH, ImageTag.findDimension(item, "height"));

        boolean overridden = ImageResizeStorageItemListener.overridePathWithNearestSize(override,
                width, height);

        if (overridden) {
            return override;
        }

        return item;
    }

    /**
     * @deprecated No replacement
     */
    @Deprecated
    private static Map<String, String> getAttributes(WebPageContext wp,
            Object src,
            String field,
            ImageEditor editor,
            StandardImageSize standardSize,
            Integer width,
            Integer height,
            CropOption cropOption,
            ResizeOption resizeOption,
            String srcAttr,
            Map<String, String> dynamicAttributes) {

        Builder tagBuilder = new Builder();

        if (src instanceof String ||
                src instanceof URI ||
                src instanceof URL) {

            tagBuilder.setItem(StorageItem.Static.createUrl(
                    JspUtils.getEmbeddedAbsolutePath(
                            wp.getServletContext(),
                            wp.getRequest(),
                            src.toString())));

        } else if (src instanceof StorageItem) {
            tagBuilder.setItem((StorageItem) src);

        } else if (src instanceof State || src instanceof Recordable) {

            // -- Hack to ensure backwards compatibility
            tagBuilder.setField(field);
            if (src instanceof State) {
                tagBuilder.setState((State) src);
            } else if (src instanceof Recordable) {
                tagBuilder.setRecordable((Recordable) src);
            }
            // -- End hack
        }

        return tagBuilder.setEditor(editor)
                .setStandardImageSize(standardSize)
                .setWidth(width)
                .setHeight(height)
                .setCropOption(cropOption)
                .setResizeOption(resizeOption)
                .setSrcAttribute(srcAttr)
                .addAllAttributes(dynamicAttributes)
                .toAttributes();
    }

    /**
     * @deprecated No replacement
     */
    @Deprecated
    private static String findStorageItemField(State state) {
        String field = null;
        ObjectType objectType = state.getType();
        if (objectType != null) {
            for (ObjectField objectField : objectType.getFields()) {
                if (ObjectField.FILE_TYPE.equals(objectField.getInternalType())) {
                    field = objectField.getInternalName();
                    break;
                }
            }
        }
        return field;
    }

    /**
     * @deprecated No replacement
     */
    @Deprecated
    private static StorageItem findStorageItem(State state, String field) {
        StorageItem item = null;
        if (field != null) {
            Object fieldValue = state.get(field);
            if (fieldValue instanceof StorageItem) {
                item = (StorageItem) fieldValue;
            }
        }
        return item;
    }

    /**
     * @deprecated Use {@link #findImageCrops(StorageItem)} instead
     */
    @Deprecated
    private static Map<String, ImageCrop> findImageCrops(State state, String field) {
        Map<String, ImageCrop> crops = null;
        Object fieldValue = state.get(field);
        if (fieldValue instanceof StorageItem) {
            crops = findImageCrops((StorageItem) fieldValue);
        }
        if (crops == null || crops.isEmpty()) {
            crops = ObjectUtils.to(new TypeReference<Map<String, ImageCrop>>() { }, state.getValue(field + ".crops"));
        }
        if (crops == null) {
            crops = new HashMap<String, ImageCrop>();
        }
        return crops;
    }

    /**
     * Finds the dimension value with the given {@code field} and
     * {@code name} from the given {@code state}.
     * @deprecated Use {@link #findDimension(StorageItem, String)} instead.
     */
    @Deprecated
    private static Integer findDimension(State state, String field, String name) {
        Integer dimension = null;
        Object fieldValue = state.get(field);
        if (fieldValue instanceof StorageItem) {
            dimension = findDimension((StorageItem) fieldValue, name);
        }
        if (dimension == null || dimension == 0) {
            dimension = ObjectUtils.to(Integer.class, state.getValue(field + ".metadata/" + name));
            if (dimension == null || dimension == 0) {
                dimension = ObjectUtils.to(Integer.class, state.getValue(field + "." + name));
                if (dimension == null || dimension == 0) {
                    dimension = null;
                }
            }
        }
        return dimension;
    }

    protected static StandardImageSize getStandardImageSizeByName(String size) {
        StandardImageSize standardImageSize = null;
        for (StandardImageSize standardSize : StandardImageSize.findAll()) {
            if (standardSize.getInternalName().equals(size)) {
                standardImageSize = standardSize;
                break;
            }
        }
        return standardImageSize;
    }

    /**
     * <p>Static utility class for building HTML 'img' tags and URLs edited by
     * an {@link ImageEditor}. This class is functionally equivalent to calling
     * the JSTL &lt;cms:img&gt; tag in your JSP code.  Example usage:</p>
     * <pre>
     * StorageItem myStorageItem;
     *
     * String imageTagHtml = new ImageTag.Builder(myStorageItem)
     *      .setWidth(300)
     *      .setHeight(200)
     *      .addAttribute("class", "thumbnail")
     *      .addAttribute("alt", "My image")
     *      .toHtml();
     * </pre>
     * You can also grab just the image URL instead of the entire HTML output
     * by calling:
     * <pre>
     * String imageUrl = new ImageTag.Builder(myStorageItem)
     *      .setWidth(300)
     *      .setHeight(200)
     *      .toUrl()
     * </pre>
     */
    public static final class Builder {

        private StorageItem item;
        @Deprecated
        private String field;
        private ImageEditor editor;

        private StandardImageSize standardImageSize;

        private Integer width;
        private Integer height;
        private CropOption cropOption;
        private ResizeOption resizeOption;

        private String tagName;
        private String srcAttribute;
        private boolean hideDimensions;
        private boolean overlay;
        private boolean disableHotSpotCrop;
        private boolean edits = true;

        private final Map<String, String> attributes = new LinkedHashMap<String, String>();

        // for backwards compatibility
        private State state;

        public Builder(StorageItem item) {
            this.item = item;
        }

        private Builder() {
        }

        public StorageItem getItem() {
            return this.item;
        }

        protected Builder setItem(StorageItem item) {
            this.item = item;
            return this;
        }

        /** Resets all fields back to null */
        private void reset() {
            item = null;
            field = null;
            editor = null;
            standardImageSize = null;
            width = null;
            height = null;
            cropOption = null;
            resizeOption = null;
            tagName = null;
            srcAttribute = null;
            hideDimensions = false;
            disableHotSpotCrop = false;
            attributes.clear();

            state = null;
        }

        /**
         * Sets the field that contains the image. If not set, the first
         * field with {@value ObjectField.FILE_TYPE} type is used.
         * @deprecated No replacement
         */
        @Deprecated
        private Builder setField(String field) {
            this.field = field;
            return this;
        }

        /**
         * Sets the name of the {@linkplain ImageEditor image editor}
         * to use.
         */
        public Builder setEditor(ImageEditor editor) {
            this.editor = editor;
            return this;
        }

        /**
         * Sets the internal name of the {@linkplain StandardImageSize
         * image size} to use.
         */
        public Builder setStandardImageSize(StandardImageSize standardImageSize) {
            this.standardImageSize = standardImageSize;
            return this;
        }

        /**
         * Sets the width. Note that this will override the width provided
         * by the image size set with {@link #setSize(String)}.
         */
        public Builder setWidth(Integer width) {
            this.width = width;
            return this;
        }

        /**
         * Sets the height. Note that this will override the height provided
         * by the image size set with {@link #setSize(String)}.
         */
        public Builder setHeight(Integer height) {
            this.height = height;
            return this;
        }

        /**
         * Sets the crop option. Note that this will override the crop option
         * provided by the image size set with {@link #setSize(String)}.
         */
        public Builder setCropOption(CropOption cropOption) {
            this.cropOption = cropOption;
            return this;
        }

        /**
         * Sets the resize option. Note that this will override the resize option
         * provided by the image size set with {@link #setSize(String)}.
         */
        public Builder setResizeOption(ResizeOption resizeOption) {
            this.resizeOption = resizeOption;
            return this;
        }

        public Builder setTagName(String tagName) {
            this.tagName = tagName;
            return this;
        }

        /**
         * Overrides the default attribute (src) used to place the image URL. This
         * is usually used in the conjunction with lazy loading scripts that copy
         * the image URL from this attribute to the "src" attribute at some point
         * after the page has loaded.
         */
        public Builder setSrcAttribute(String srcAttribute) {
            this.srcAttribute = srcAttribute;
            return this;
        }

        /**
         * Set to true if the resulting image dimensions should be removed
         * from the final tag output.
         */
        public Builder hideDimensions() {
            this.hideDimensions = true;
            return this;
        }

        public boolean isOverlay() {
            return overlay;
        }

        public void setOverlay(boolean overlay) {
            this.overlay = overlay;
        }

        public boolean isDisableHotSpotCrop() {
            return disableHotSpotCrop;
        }

        public void setDisableHotSpotCrop(boolean disableHotSpotCrop) {
            this.disableHotSpotCrop = disableHotSpotCrop;
        }

        public boolean isEdits() {
            return edits;
        }

        public Builder setEdits(boolean edits) {
            this.edits = edits;
            return this;
        }

        /**
         * Adds an attribute to be placed on the tag.
         */
        public Builder addAttribute(String name, Object value) {
            this.attributes.put(name, value != null ? value.toString() : null);
            return this;
        }

        /**
         * Adds all the attributes to be placed on the tag.
         */
        public Builder addAllAttributes(Map<String, ?> attributes) {
            if (attributes != null) {
                for (Map.Entry<String, ?> entry : attributes.entrySet()) {
                    addAttribute(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        /**
         * For backwards compatibility
         *
         * @deprecated
         */
        @Deprecated
        private Builder setState(State state) {
            this.state = state;
            return this;
        }

        /**
         * For backwards compatibility
         *
         * @deprecated
         */
        @Deprecated
        private Builder setRecordable(Recordable recordable) {
            setState(State.getInstance(recordable));
            return this;
        }

        /**
         *
         * @return the HTML for an img tag constructed by this Builder.
         */
        public String toHtml() {
            String html = convertAttributesToHtml(tagName, toAttributes());

            if (isOverlay()) {
                StorageItem item = null;
                Map<String, ImageCrop> crops = null;

                if (this.state != null) {
                    State objectState = this.state;
                    String field = this.field;

                    if (ObjectUtils.isBlank(field)) {
                        field = findStorageItemField(objectState);
                    }

                    item = findStorageItem(objectState, field);

                    if (item != null) {
                        crops = findImageCrops(objectState, field);
                    }

                } else {
                    item = this.item;

                    if (item != null) {
                        crops = findImageCrops(item);
                    }
                }

                if (item != null && crops != null && standardImageSize != null) {
                    ImageCrop crop = crops.get(standardImageSize.getId().toString());

                    if (crop != null) {
                        List<ImageTextOverlay> textOverlays = crop.getTextOverlays();
                        boolean hasOverlays = false;

                        for (ImageTextOverlay textOverlay : textOverlays) {
                            if (!ObjectUtils.isBlank(textOverlay.getText())) {
                                hasOverlays = true;
                                break;
                            }
                        }

                        if (hasOverlays) {
                            StringBuilder overlay = new StringBuilder();
                            CmsTool cms = Application.Static.getInstance(CmsTool.class);
                            String defaultCss = cms.getDefaultTextOverlayCss();
                            String id = "i" + UUID.randomUUID().toString().replace("-", "");

                            overlay.append("<style type=\"text/css\">");

                            if (!ObjectUtils.isBlank(defaultCss)) {
                                overlay.append("#");
                                overlay.append(id);
                                overlay.append("{display:inline-block;overflow:hidden;position:relative;");
                                overlay.append(defaultCss);
                                overlay.append("}");
                            }

                            for (CmsTool.CssClassGroup group : cms.getTextCssClassGroups()) {
                                String groupName = group.getInternalName();
                                for (CmsTool.CssClass cssClass : group.getCssClasses()) {
                                    overlay.append("#");
                                    overlay.append(id);
                                    overlay.append(" .cms-");
                                    overlay.append(groupName);
                                    overlay.append("-");
                                    overlay.append(cssClass.getInternalName());
                                    overlay.append("{");
                                    overlay.append(cssClass.getCss());
                                    overlay.append("}");
                                }
                            }

                            overlay.append("</style>");

                            overlay.append("<span id=\"");
                            overlay.append(id);
                            overlay.append("\">");
                            overlay.append(html);

                            for (ImageTextOverlay textOverlay : textOverlays) {
                                String text = textOverlay.getText();

                                overlay.append("<span style=\"left: ");
                                overlay.append(textOverlay.getX() * 100);
                                overlay.append("%; position: absolute; top: ");
                                overlay.append(textOverlay.getY() * 100);
                                overlay.append("%; font-size: ");
                                overlay.append(textOverlay.getSize() * standardImageSize.getHeight());
                                overlay.append("px; width: ");
                                overlay.append(textOverlay.getWidth() != 0.0 ? textOverlay.getWidth() * 100 : 100.0);
                                overlay.append("%;\">");
                                overlay.append(text);
                                overlay.append("</span>");
                            }

                            overlay.append("</span>");
                            html = overlay.toString();
                        }
                    }
                }
            }

            return html;
        }

        /**
         *
         * @return the URL to the image as a String.
         */
        public String toUrl() {
            return toAttributes().get(srcAttribute != null ? srcAttribute : "src");
        }

        /** Returns all the attributes that will get placed on the img tag. */
        public Map<String, String> toAttributes() {
            // set all the attributes
            Map<String, String> attributes = new LinkedHashMap<String, String>();

            ImageEditor editor = this.editor;

            StandardImageSize standardImageSize = this.standardImageSize;

            Integer width = this.width;
            Integer height = this.height;
            CropOption cropOption = this.cropOption;
            ResizeOption resizeOption = this.resizeOption;

            String srcAttr = this.srcAttribute;
            boolean hideDimensions = this.hideDimensions;

            StorageItem item = null;
            Integer originalWidth = null;
            Integer originalHeight = null;
            Map<String, ImageCrop> crops = null;

            if (this.state != null) { // backwards compatibility path
                State objectState = this.state;
                String field = this.field;

                if (ObjectUtils.isBlank(field)) {
                    field = findStorageItemField(objectState);
                }

                item = findStorageItem(objectState, field);

                if (item != null) {
                    originalWidth = findDimension(objectState, field, "width");
                    originalHeight = findDimension(objectState, field, "height");
                    crops = findImageCrops(objectState, field);
                }

            } else { // new code path
                item = this.item;

                if (item != null) {
                    originalWidth = findDimension(item, "width");
                    originalHeight = findDimension(item, "height");
                    crops = findImageCrops(item);
                }
            }

            // null out all dimensions that are less than or equal to zero
            originalWidth = originalWidth != null && originalWidth <= 0 ? null : originalWidth;
            originalHeight = originalHeight != null && originalHeight <= 0 ? null : originalHeight;
            width = width != null && width <= 0 ? null : width;
            height = height != null && height <= 0 ? null : height;

            if (item != null) {
                Map<String, Object> options = new LinkedHashMap<String, Object>();

                Integer cropX = null, cropY = null, cropWidth = null, cropHeight = null;

                // set fields from this standard size if they haven't already been set
                if (standardImageSize != null) {

                    Integer standardWidth = standardImageSize.getWidth();
                    Integer standardHeight = standardImageSize.getHeight();
                    if (standardWidth <= 0) { standardWidth = null; }
                    if (standardHeight <= 0) { standardHeight = null; }

                    Double standardAspectRatio = null;
                    if (standardWidth != null && standardHeight != null) {
                        standardAspectRatio = (double) standardWidth / (double) standardHeight;
                    }

                    // if only one of either width or height is set then calculate
                    // the other dimension based on the standardImageSize aspect
                    // ratio rather than blindly taking the other standardImageSize
                    // dimension.
                    if (standardAspectRatio != null && (width != null || height != null)) {

                        if (width != null && height == null) {
                            height = (int) (width / standardAspectRatio);

                        } else if (width == null && height != null) {
                            width = (int) (height * standardAspectRatio);
                        }

                    } else {
                        // get the standard image dimensions
                        if (width == null) {
                            width = standardWidth;
                        }
                        if (height == null) {
                            height = standardHeight;
                        }
                    }

                    // get the crop and resize options
                    if (cropOption == null) {
                        cropOption = standardImageSize.getCropOption();
                    }
                    if (resizeOption == null) {
                        resizeOption = standardImageSize.getResizeOption();
                    }

                    // get a potentially smaller image from the StorageItem. This improves
                    // resize performance on large images.
                    StorageItem alternateItem = findStorageItemForSize(item, width, height);
                    if (alternateItem != item) {
                        item = alternateItem;
                        originalWidth = findDimension(item, "width");
                        originalHeight = findDimension(item, "height");
                    }

                    // get the crop coordinates
                    ImageCrop crop;
                    if (crops != null && (crop = crops.get(standardImageSize.getId().toString())) != null &&
                            originalWidth != null && originalHeight != null) {

                        cropX = (int) (crop.getX() * originalWidth);
                        cropY = (int) (crop.getY() * originalHeight);
                        cropWidth = (int) (crop.getWidth() * originalWidth);
                        cropHeight = (int) (crop.getHeight() * originalHeight);
                    }
                }

                // if the crop info is unavailable, assume that the image
                // dimensions are the crop dimensions in case the image editor
                // knows how to crop without the x & y coordinates
                if (cropWidth == null) {
                    cropWidth = width;
                }
                if (cropHeight == null) {
                    cropHeight = height;
                }

                // set the options
                if (cropOption != null) {
                    options.put(ImageEditor.CROP_OPTION, cropOption.getImageEditorOption());
                }
                if (resizeOption != null) {
                    options.put(ImageEditor.RESIZE_OPTION, resizeOption.getImageEditorOption());
                }

                if (isEdits()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> edits = (Map<String, Object>) item.getMetadata().get("cms.edits");

                    if (edits != null) {
                        ImageEditor realEditor = editor;
                        if (realEditor == null) {
                            realEditor = ImageEditor.Static.getDefault();
                        }

                        //rotate first
                        Set<Map.Entry<String, Object>> entrySet = new TreeMap<String, Object>(edits).entrySet();
                        for (Map.Entry<String, Object> entry : entrySet) {
                            if (entry.getKey().equals("rotate")) {
                                item = realEditor.edit(item, entry.getKey(), null, entry.getValue());
                            }
                        }
                        for (Map.Entry<String, Object> entry : entrySet) {
                            if (!entry.getKey().equals("rotate")) {
                                item = realEditor.edit(item, entry.getKey(), null, entry.getValue());
                            }
                        }
                    }
                }

                // Requires at least the width and height to perform a crop
                if (cropWidth != null && cropHeight != null) {
                    if (!disableHotSpotCrop &&
                            useHotSpotCrop() &&
                            (standardImageSize.getCropOption() == null || standardImageSize.getCropOption().equals(CropOption.AUTOMATIC)) &&
                            cropX == null &&
                            cropY == null) {

                        List<Integer> hotSpotCrop = ImageHotSpot.crop(item, cropWidth, cropHeight);
                        if (!ObjectUtils.isBlank(hotSpotCrop) &&
                                hotSpotCrop.size() == 4) {
                            cropX = hotSpotCrop.get(0);
                            cropY = hotSpotCrop.get(1);
                            cropWidth = hotSpotCrop.get(2);
                            cropHeight = hotSpotCrop.get(3);
                        }
                    }

                    item = ImageEditor.Static.crop(editor, item, options, cropX, cropY, cropWidth, cropHeight);
                }

                // Requires only one of either the width or the height to perform a resize
                if (width != null || height != null) {
                    item = ImageEditor.Static.resize(editor, item, options, width, height);
                }

                String url = item.getPublicUrl();
                if (url != null) {
                    attributes.put(srcAttr != null ? srcAttr : "src", url);
                }

                Integer newWidth = findDimension(item, "width");
                Integer newHeight = findDimension(item, "height");
                if (newWidth != null && !hideDimensions) {
                    attributes.put("width", String.valueOf(newWidth));
                }
                if (newHeight != null && !hideDimensions) {
                    attributes.put("height", String.valueOf(newHeight));
                }

                if (this.attributes != null) {
                    attributes.putAll(this.attributes);
                }
            }

            if (standardImageSize != null) {
                attributes.put("data-size", standardImageSize.getInternalName());
            }

            return attributes;
        }
    }

    public static final class Static {

        private Static() {
        }

        /**
         * @deprecated Use {@link ImageTag.Builder} instead.
         */
        @Deprecated
        public static String getHtmlFromStandardImageSize(WebPageContext wp,
                StorageItem item,
                ImageEditor editor,
                StandardImageSize size,
                String srcAttr,
                Map<String, String> dynamicAttributes) {

            return getHtmlFromOptions(wp,
                    item,
                    editor,
                    size.getWidth(),
                    size.getHeight(),
                    size.getCropOption(),
                    size.getResizeOption(),
                    srcAttr,
                    dynamicAttributes);
        }

        /**
         * @deprecated Use {@link ImageTag.Builder} instead.
         */
        @Deprecated
        public static String getHtmlFromOptions(WebPageContext wp,
                StorageItem item,
                ImageEditor editor,
                Integer width,
                Integer height,
                CropOption cropOption,
                ResizeOption resizeOption,
                String srcAttr,
                Map<String, String> dynamicAttributes) {

            return new Builder(item)
                .setEditor(editor)
                .setWidth(width)
                .setHeight(height)
                .setCropOption(cropOption)
                .setResizeOption(resizeOption)
                .setSrcAttribute(srcAttr)
                .addAllAttributes(dynamicAttributes)
                .toHtml();
        }

        /**
         * @deprecated Use {@link ImageTag.Builder} instead.
         */
        @Deprecated
        public static String getUrlFromStandardImageSize(WebPageContext wp,
                StorageItem item,
                ImageEditor editor,
                StandardImageSize size) {

            return getUrlFromOptions(wp,
                    item,
                    editor,
                    size.getWidth(),
                    size.getHeight(),
                    size.getCropOption(),
                    size.getResizeOption());
        }

        /**
         * @deprecated Use {@link ImageTag.Builder} instead.
         */
        @Deprecated
        public static String getUrlFromOptions(WebPageContext wp,
                StorageItem item,
                ImageEditor editor,
                Integer width,
                Integer height,
                CropOption cropOption,
                ResizeOption resizeOption) {

            return new Builder(item)
                .setEditor(editor)
                .setWidth(width)
                .setHeight(height)
                .setCropOption(cropOption)
                .setResizeOption(resizeOption)
                .toUrl();
        }

        /**
         * @deprecated Use {@link ImageTag.Builder} instead.
         */
        @Deprecated
        public static String getHtml(WebPageContext wp,
                Object object,
                String field,
                ImageEditor editor,
                StandardImageSize standardSize,
                Integer width,
                Integer height,
                CropOption cropOption,
                ResizeOption resizeOption,
                String srcAttr,
                Map<String, String> dynamicAttributes) {

            Map<String, String> attributes = getAttributes(wp,
                    object, field, editor, standardSize, width, height, cropOption, resizeOption, srcAttr, dynamicAttributes);

            return convertAttributesToHtml(null, attributes);
        }

        /**
         * @deprecated Use {@link ImageTag.Builder} instead.
         */
        @Deprecated
        public static String getHtml(PageContext pageContext,
                Object object,
                String field,
                ImageEditor editor,
                StandardImageSize standardSize,
                Integer width,
                Integer height,
                CropOption cropOption,
                ResizeOption resizeOption,
                String srcAttr,
                Map<String, String> dynamicAttributes) {
            return getHtml(new WebPageContext(pageContext),
                    object, field, editor, standardSize, width, height, cropOption, resizeOption, srcAttr, dynamicAttributes);
        }

        /**
         * @deprecated Use {@link ImageTag.Builder} instead.
         */
        @Deprecated
        public static String makeUrlFromStandardImageSize(WebPageContext wp,
                Object object,
                String field,
                ImageEditor editor,
                String size) {

            StandardImageSize standardImageSize = getStandardImageSizeByName(size);

            Map<String, String> attributes = getAttributes(wp,
                    object, field, editor, standardImageSize, null, null, null, null, null, null);
            return attributes.get("src");
        }

        /**
         * @deprecated Use {@link ImageTag.Builder} instead.
         */
        @Deprecated
        public static String makeUrlFromStandardImageSize(PageContext pageContext,
                Object object,
                String field,
                ImageEditor editor,
                String size) {

            return makeUrlFromStandardImageSize(new WebPageContext(pageContext), object, field, editor, size);
        }

        /**
         * @deprecated Use {@link ImageTag.Builder} instead.
         */
        @Deprecated
        public static String makeUrlFromOptions(WebPageContext wp,
                Object object,
                String field,
                ImageEditor editor,
                Integer width,
                Integer height,
                CropOption cropOption,
                ResizeOption resizeOption) {

            Map<String, String> attributes = getAttributes(wp,
                    object, field, editor, null, width, height, cropOption, resizeOption, null, null);
            return attributes.get("src");
        }

        /**
         * @deprecated Use {@link ImageTag.Builder} instead.
         */
        @Deprecated
        public static String makeUrlFromOptions(PageContext pageContext,
                Object object,
                String field,
                ImageEditor editor,
                Integer width,
                Integer height,
                CropOption cropOption,
                ResizeOption resizeOption) {
            return makeUrlFromOptions(new WebPageContext(pageContext), object, field, editor, width, height, cropOption, resizeOption);
        }

    }

    /**
     * @deprecated Use {@link ImageTag.Builder} instead.
     */
    @Deprecated
    public static String makeUrl(
            PageContext pageContext,
            Object object,
            String field,
            String editor,
            String size,
            Integer width,
            Integer height) {

        StandardImageSize standardImageSize = getStandardImageSizeByName(size);

        ImageEditor imageEditor = null;
        if (editor != null) {
            imageEditor = ImageEditor.Static.getInstance(editor);
        }
        if (imageEditor == null) {
            imageEditor = ImageEditor.Static.getDefault();
        }

        Map<String, String> attributes = getAttributes(new WebPageContext(pageContext),
                object, field, imageEditor, standardImageSize, width, height, null, null, null, null);
        return attributes.get("src");
    }
}
