package com.psddev.cms.tool;

/**
 * Interface for displaying &lt;option /&gt; element as HTML.
 * This is only applicable to a field annotated with {@link com.psddev.cms.db.ToolUi.DropDown}
 * <p>e.g.:</p>
 * <p>In order to display colored text in drop down list UI; </p>
 * <p><blockquote>
 * <pre><code>
 *     Class Foo {
 *         {@literal@}ToolUi.DropDown
 *         private Color color;
 *     }
 *
 *     Class Color implements DropDownDisplay {
 *         private String name;
 *         private String color;
 *
 *         {@literal@}Override
 *         String createDropDownDisplayHtml() {
 *             return String.format("&lt;span style="color: %s;"&gt;%s&lt;/span&gt;", color, name);
 *         }
 *     }
 * </code></pre>
 * </blockquote></p>
 */
public interface DropDownDisplay {

    String createDropDownDisplayHtml();
}
