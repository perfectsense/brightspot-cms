package com.psddev.cms.tool.page;

import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Displays core image metadata that has been extracted via
 * <a href="http://www.drewnoakes.com/code/exif/">Metadata Extractor</a>.
 */
@RoutingFilter.Path(application = "cms", value = ContentMetadata.PATH)
public class ContentMetadata extends PageServlet {

    public static final String PATH = "/contentMetadata";

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        if (!page.isFormPost()) {
            throw new IllegalArgumentException("Request must be POST!");
        }

        String metadataJson = Optional.ofNullable(page.param(String.class, "metadata"))
                .orElseThrow(() -> new IllegalArgumentException("No metadata found!"));

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = new TreeMap<>(((Map<String, Object>) ObjectUtils.fromJson(metadataJson)).entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("cms."))
                .collect(Collectors.toMap(entry -> {
                    String key = entry.getKey();
                    return key.substring(0, 1).toUpperCase() + key.substring(1);
                }, Map.Entry::getValue)));

        // Disable remove button.
        page.writeStart("style", "type", "text/css"); {
            page.writeCss(".removeButton", "display", "none !important");
        }
        page.writeEnd();

        page.writeStart("div", "class", "widget"); {
            page.writeStart("h1").writeHtml(page.localize(ContentMetadata.class, "title")).writeEnd();
            page.writeStart("div", "class", "repeatableForm"); {
                page.writeStart("ul"); {
                    for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                        writeEntry(page, entry);
                    }
                }
                page.writeEnd();
            }
            page.writeEnd();
        }
        page.writeEnd();
    }

    /*
     * Recursively writes metadata.
     */
    @SuppressWarnings("unchecked")
    private void writeEntry(ToolPageContext page, Map.Entry<String, Object> entry) throws IOException {
        String key = entry.getKey();
        Object value = entry.getValue();

        if (value instanceof CompactMap) {
            CompactMap valueMap = (CompactMap) value;

            if (!valueMap.isEmpty()) {
                page.writeStart("li", "data-type", key); {
                    page.writeStart("div", "class", "objectInputs"); {
                        page.writeStart("div", "class", "repeatableForm"); {
                            page.writeStart("ul"); {
                                for (Object subEntry : valueMap.entrySet()) {
                                    writeEntry(page, (Map.Entry) subEntry);
                                }
                            }
                            page.writeEnd();
                        }
                        page.writeEnd();
                    }
                    page.writeEnd();
                }
                page.writeEnd();
                return;
            }

        } else {
            key += ": " + value;
        }

        page.writeStart("li", "data-type", key).writeEnd();

        // Disable pointer events.
        page.writeStart("style", "type", "text/css"); {
            String labelSelector = "li[data-type=\"" + key + "\"] .repeatableLabel";
            page.writeCss(labelSelector, "pointer-events", "none");
            page.writeCss(labelSelector + ":after", "content", "none !important");
        }
        page.writeEnd();
    }
}
