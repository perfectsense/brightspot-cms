package com.psddev.cms.tool.page;

import com.psddev.cms.db.ImageTag;
import com.psddev.cms.db.ResizeOption;
import com.psddev.cms.db.Site;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ImageEditor;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PaginatedResult;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StorageItem;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RoutingFilter.Path(application = "cms", value = "/searchCarousel")
public class SearchCarousel extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Search search = new Search();
        String searchString = page.param(String.class, "search");
        Long searchOffset = page.param(Long.class, Search.OFFSET_PARAMETER);

        search.getState().putAll((Map<String, Object>) ObjectUtils.fromJson(searchString));

        if (searchOffset != null) {
            search.setOffset(searchOffset);
        }

        PaginatedResult<?> result = search.toQuery(page.getSite()).select(search.getOffset(), search.getLimit());

        List<Object> items = new ArrayList<>();

        for (Object resultItem : result.getItems()) {
            items.add(resultItem);
        }

        UUID currentContentId = page.param(UUID.class, "id");
        boolean included = true;

        if (searchOffset == null) { // only splice in the current object if this is the initial page and the current object isn't in the result list
            Object currentContent = currentContentId == null ? null : Query.fromAll().where("id = ?", currentContentId).first();

            if (currentContent != null && !items.contains(currentContent)) {
                included = false;
                items.add(0, currentContent);
            }
        }

        if (items.size() <= 1 && !result.hasPrevious()) {
            return;
        }

        page.writeStart("div", "class", "widget-searchCarousel",
                "data-next-page", result.hasNext() ? page.url("", Search.OFFSET_PARAMETER, result.getNextOffset()) : "",
                "data-prev-page", result.hasPrevious() ? page.url("", Search.OFFSET_PARAMETER, result.getPreviousOffset()) : "",
                "data-start-index", search.getOffset());

            for (Object item : items) {
                State itemState = State.getInstance(item);
                UUID itemId = itemState.getId();
                StorageItem itemPreview = item instanceof SearchCarouselPreviewable
                        ? ((SearchCarouselPreviewable) item).getSearchCarouselPreview()
                        : itemState.getPreview();

                page.writeStart("a",
                        "class", (itemId.equals(currentContentId) ? "widget-searchCarousel-item-selected" + (included ? "" : " notIncluded") : null),
                        "data-objectId", itemState.getId(),
                        "target", "_top",
                        "href", page.toolUrl(CmsTool.class, "/content/edit.jsp",
                                "id", itemState.getId(),
                                "search", ObjectUtils.toJson(search.getState().getSimpleValues())));

                    boolean itemPreviewImage = false;

                    if (itemPreview != null) {
                        String previewContentType = itemPreview.getContentType();

                        if (previewContentType != null && previewContentType.startsWith("image/")) {
                            itemPreviewImage = true;
                        }
                    }

                    if (itemPreviewImage) {
                        String itemPreviewUrl = ImageEditor.Static.getDefault() != null
                                ? new ImageTag.Builder(itemPreview)
                                .setHeight(300)
                                .setResizeOption(ResizeOption.ONLY_SHRINK_LARGER)
                                .toUrl()
                                : itemPreview.getPublicUrl();

                        page.writeStart("figure");
                            page.writeElement("img",
                                    "src", itemPreviewUrl,
                                    "alt", (page.getObjectLabel(itemState.as(Site.ObjectModification.class).getOwner()) + ": ")
                                            + (page.getTypeLabel(item) + ": ") + page.getObjectLabel(item));

                            page.writeStart("figcaption");
                                page.writeTypeObjectLabel(item);
                            page.writeEnd();
                        page.writeEnd();

                    } else {
                        page.writeTypeObjectLabel(item);
                    }
                page.writeEnd();
            }
        page.writeEnd();
    }
}
