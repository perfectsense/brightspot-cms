package com.psddev.cms.tool.page.content;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.Overlay;
import com.psddev.cms.db.OverlayProvider;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.WorkInProgress;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Edit {

    private static final String ATTRIBUTE_PREFIX = Edit.class.getName() + ".";
    private static final String WIP_DIFFERENCE_IDS_ATTRIBUTE = ATTRIBUTE_PREFIX + "wipDifferenceIds";

    public static Overlay getOverlay(Object content) {
        return content != null
                ? (Overlay) State.getInstance(content).getExtras().get("cms.tool.overlay")
                : null;
    }

    public static void writeOverlayProviderSelect(ToolPageContext page, Object content, OverlayProvider selected) throws IOException {
        List<OverlayProvider> overlayProviders = Query.from(OverlayProvider.class).selectAll();

        overlayProviders.removeIf(p -> !p.shouldOverlay(content));

        if (overlayProviders.isEmpty()) {
            return;
        }

        UUID contentId = State.getInstance(content).getId();

        page.writeStart("div", "class", "OverlayProviderSelect");
        page.writeStart("ul");
        {
            page.writeStart("li", "class", selected == null ? "selected" : null);
            {
                page.writeStart("a",
                        "href", page.url("",
                                "id", contentId,
                                "overlayId", null));
                page.writeHtml("Default");
                page.writeEnd();
            }
            page.writeEnd();

            for (OverlayProvider overlayProvider : overlayProviders) {
                page.writeStart("li", "class", overlayProvider.equals(selected) ? "selected" : null);
                {
                    page.writeStart("a",
                            "href", page.url("",
                                    "id", contentId,
                                    "overlayId", overlayProvider.getState().getId()));
                    page.writeObjectLabel(overlayProvider);
                    page.writeEnd();
                }
                page.writeEnd();
            }
        }
        page.writeEnd();
        page.writeEnd();
    }

    /**
     * Creates the placeholder text for the given {@code field} that should
     * be displayed to the user in the context of the given {@code page}.
     *
     * @param page Can't be {@code null}.
     * @param field Can't be {@code null}.
     * @return Never {@code null}.
     */
    public static String createPlaceholderText(ToolPageContext page, ObjectField field) throws IOException {
        String placeholder = field.as(ToolUi.class).getPlaceholder();

        if (field.isRequired()) {
            String required = page.localize(field.getParentType(), "placeholder.required");

            if (ObjectUtils.isBlank(placeholder)) {
                placeholder = required;

            } else {
                placeholder += ' ';
                placeholder += required;
            }
        }

        if (ObjectUtils.isBlank(placeholder)) {
            return "";

        } else {
            return placeholder;
        }
    }

    /**
     * Restores the work in progress associated with the given {@code content}
     * in the context of the given {@code page}.
     *
     * <p>If successful, writes an appropriate message to the output attached
     * to the given {@code page}.</p>
     *
     * @param page Can't be {@code null}.
     * @param content Can't be {@code null}.
     */
    public static void restoreWorkInProgress(ToolPageContext page, Object content) throws IOException {
        if (page.getOverlaidHistory(content) != null
                || page.getOverlaidDraft(content) != null) {

            return;
        }

        State state = State.getInstance(content);

        if (state.hasAnyErrors()) {
            return;
        }

        ToolUser user = page.getUser();

        if (user.isDisableWorkInProgress()
                || page.getCmsTool().isDisableWorkInProgress()) {

            return;
        }

        WorkInProgress wip = Query.from(WorkInProgress.class)
                .where("owner = ?", user)
                .and("contentId = ?", state.getId())
                .first();

        if (wip == null) {
            return;
        }

        Date wipCreate = wip.getCreateDate();
        Date wipUpdate = wip.getUpdateDate();
        Date contentUpdate = State.getInstance(content).as(Content.ObjectModification.class).getUpdateDate();

        if (wipCreate != null && wipUpdate != null && contentUpdate != null) {
            long contentTime = contentUpdate.getTime();

            if (wipCreate.getTime() < contentTime && contentTime <= wipUpdate.getTime()) {
                wip.delete();
                return;
            }
        }

        Map<String, Map<String, Object>> differences = wip.getDifferences();

        page.getRequest().setAttribute(WIP_DIFFERENCE_IDS_ATTRIBUTE, differences.keySet());

        state.setValues(Draft.mergeDifferences(
                state.getDatabase().getEnvironment(),
                state.getSimpleValues(),
                differences));

        page.writeStart("div", "class", "message message-warning WorkInProgressRestoredMessage");
        {
            page.writeStart("div", "class", "WorkInProgressRestoredMessage-actions");
            {
                page.writeStart("a",
                        "class", "icon icon-action-remove",
                        "href", page.cmsUrl("/user/wips",
                                "action-delete", true,
                                "wip", wip.getId(),
                                "returnUrl", page.url("")));
                page.writeHtml(page.localize(wip, "action.clearChanges"));
                page.writeEnd();
            }
            page.writeEnd();

            page.writeStart("p");
            {
                page.writeHtml(page.localize(wip, "message.restored"));
            }
            page.writeEnd();
        }
        page.writeEnd();
    }

    /**
     * Returns {@code true} if a work in progress object was restored on top of
     * the given {@code object} using {@link #restoreWorkInProgress} in the
     * context of the given {@code page}.
     *
     * @param page Can't be {@code null}.
     * @param object Can't be {@code null}.
     */
    public static boolean isWorkInProgressRestored(ToolPageContext page, Object object) {
        @SuppressWarnings("unchecked")
        Set<String> differenceIds = (Set<String>) page.getRequest().getAttribute(WIP_DIFFERENCE_IDS_ATTRIBUTE);

        if (differenceIds == null) {
            return false;
        }

        State state = State.getInstance(object);

        return differenceIds.contains(state.getId().toString())
                || wipCheckObject(differenceIds, state.getSimpleValues());
    }

    @SuppressWarnings("unchecked")
    private static boolean wipCheckObject(Set<String> differenceIds, Object object) {
        if (object instanceof List) {
            return wipCheckCollection(differenceIds, (List<Object>) object);

        } else if (object instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) object;
            String ref = ObjectUtils.to(String.class, map.get("_ref"));

            if (ref != null) {
                return differenceIds.contains(ref);
            }

            String id = ObjectUtils.to(String.class, map.get(State.ID_KEY));

            return (id != null && differenceIds.contains(id))
                    || wipCheckCollection(differenceIds, map.values());

        } else {
            return false;
        }
    }

    private static boolean wipCheckCollection(Set<String> differenceIds, Collection<Object> collection) {
        return collection.stream().anyMatch(v -> wipCheckObject(differenceIds, v));
    }
}
