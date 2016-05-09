package com.psddev.cms.tool;

/**
 * Interface for displaying {@link Widget}s conditionally in content edit page.
 * <p>e.g.:</p>
 * <p>In order to hide the urls widget from Comment edit page in CMS. </p>
 * <p><blockquote>
 * <pre><code>
 *     Class Comment implements ContentEditWidgetDisplay {
 *         boolean shouldDisplayContentEditWidget(String widgetName) {
 *             return !widgetName.equals("urls");
 *         }
 *     }
 * </code></pre>
 * </blockquote></p>
 */
public interface ContentEditWidgetDisplay {

    /**
     * Returns {@code true} if {@link Widget} should be displayed
     * where {@code widgetName} is {@link Widget#internalName}.
     * @param widgetName {@link Widget#internalName}.
     */
    boolean shouldDisplayContentEditWidget(String widgetName);
}
