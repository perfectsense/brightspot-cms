package com.psddev.cms.tool.page.content;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.psddev.cms.db.Content;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.Overlay;
import com.psddev.cms.db.OverlayProvider;
import com.psddev.cms.db.Page;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.WorkInProgress;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.ContentEditWidget;
import com.psddev.cms.tool.ContentEditWidgetDisplay;
import com.psddev.cms.tool.ContentEditSection;
import com.psddev.cms.tool.ContentEditWidgetFilter;
import com.psddev.cms.tool.Tool;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.UpdatingContentEditWidget;
import com.psddev.cms.tool.Widget;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.DependencyResolver;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Comparator;
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

    /**
     * @param page Nonnull.
     * @param content Nonnull.
     * @param section Nonnull.
     */
    public static void writeWidgets(ToolPageContext page, Object content, ContentEditSection section) throws IOException {
        Preconditions.checkNotNull(page);
        Preconditions.checkNotNull(content);
        Preconditions.checkNotNull(section);

        String legacyPosition = section.getLegacyPosition();

        if (legacyPosition != null) {
            page.writeStart("div", "class", "contentWidgets contentWidgets-" + legacyPosition);
            writeLegacyWidgets(page, content, legacyPosition);
        }

        CmsTool cms = page.getCmsTool();
        List<ContentEditWidget> widgets = cms.getContentEditWidgets();

        ClassFinder.findConcreteClasses(ContentEditWidget.class)
                .stream()
                .filter(c -> widgets.stream().noneMatch(c::isInstance))
                .sorted(Comparator.comparing(Class::getName))
                .map(c -> TypeDefinition.getInstance(c).newInstance())
                .forEach(widgets::add);

        for (ContentEditWidget widget : widgets) {
            ContentEditSection widgetSection = widget.getSectionOverride();

            if (widgetSection == null) {
                widgetSection = widget.getSection(page, content);
            }

            if (section.equals(widgetSection)
                    && widget.shouldDisplay(page, content)) {

                if (widget instanceof UpdatingContentEditWidget) {
                    writeWidgetOrError(page, content, section, widget);

                } else {
                    ContentEditWidgetFilter.writeFrame(page, content, section, widget);
                }
            }
        }

        if (legacyPosition != null) {
            page.writeEnd();
        }
    }

    private static void writeLegacyWidgets(ToolPageContext page, Object content, String position) throws IOException {
        List<Widget> widgets = Tool.Static.getWidgets(position).stream().findFirst().orElse(null);

        if (ObjectUtils.isBlank(widgets)) {
            return;
        }

        State state = State.getInstance(content);
        ObjectType type = state.getType();

        for (Widget widget : widgets) {
            String internalName = widget.getInternalName();

            if (content instanceof ContentEditWidgetDisplay) {
                if (!((ContentEditWidgetDisplay) content).shouldDisplayContentEditWidget(internalName)) {
                    continue;
                }

            } else if ((type == null
                    || !type.as(ToolUi.class).isPublishable())
                    && !widget.shouldDisplayInNonPublishable()) {

                continue;
            }

            if (!page.hasPermission(widget.getPermissionId())) {
                continue;
            }

            page.writeElement("input",
                    "type", "hidden",
                    "name", state.getId() + "/_widget",
                    "value", internalName);

            String displayHtml;

            try {
                displayHtml = widget.createDisplayHtml(page, content);

            } catch (Exception error) {
                displayHtml = createErrorHtml(error);
            }

            if (!ObjectUtils.isBlank(displayHtml)) {
                page.write(displayHtml);
            }
        }
    }

    public static void writeWidgetOrError(ToolPageContext page, Object content, ContentEditSection section, ContentEditWidget widget) throws IOException {
        String widgetHtml;

        try {
            ToolPageContext pageCopy = new ToolPageContext(page.getServletContext(), page.getRequest(), page.getResponse());
            StringWriter widgetHtmlWriter = new StringWriter();

            pageCopy.setDelegate(widgetHtmlWriter);
            widget.display(pageCopy, content, section);

            widgetHtml = widgetHtmlWriter.toString();

        } catch (Exception error) {
            Throwables.propagateIfInstanceOf(error, IOException.class);

            widgetHtml = createErrorHtml(error);
        }

        if (!ObjectUtils.isBlank(widgetHtml)) {
            section.displayBefore(page, content, widget);
            page.write(widgetHtml);
            section.displayAfter(page);
        }
    }

    private static String createErrorHtml(Throwable error) throws IOException {
        StringWriter errorString = new StringWriter();
        HtmlWriter errorHtml = new HtmlWriter(errorString);

        errorHtml.putAllStandardDefaults();
        errorHtml.writeStart("pre", "class", "message message-error");
        errorHtml.writeObject(error);
        errorHtml.writeEnd();

        return errorString.toString();
    }

    /**
     * @param page Nonnull.
     * @param content Nonnull.
     */
    @SuppressWarnings("deprecation")
    public static void updateUsingWidgets(ToolPageContext page, Object content) throws Exception {
        Preconditions.checkNotNull(page);
        Preconditions.checkNotNull(content);

        State state = State.getInstance(content);
        List<String> requestWidgets = page.params(String.class, state.getId() + "/_widget");

        if (!requestWidgets.isEmpty()) {
            DependencyResolver<Widget> widgets = new DependencyResolver<>();

            for (Widget widget : Tool.Static.getPluginsByClass(Widget.class)) {
                widgets.addRequired(widget, widget.getUpdateDependencies());
            }

            for (Widget widget : widgets.resolve()) {
                for (String requestWidget : requestWidgets) {
                    if (widget.getInternalName().equals(requestWidget)) {
                        widget.update(page, content);
                        break;
                    }
                }
            }
        }

        Page.Layout layout = (Page.Layout) page.getRequest().getAttribute("layoutHack");

        if (layout != null) {
            ((Page) content).setLayout(layout);
        }

        CmsTool cms = page.getCmsTool();
        List<ContentEditWidget> widgets = cms.getContentEditWidgets();

        ClassFinder.findConcreteClasses(ContentEditWidget.class)
                .stream()
                .filter(c -> widgets.stream().noneMatch(c::isInstance))
                .sorted(Comparator.comparing(Class::getName))
                .map(c -> TypeDefinition.getInstance(c).newInstance())
                .forEach(widgets::add);

        for (ContentEditWidget widget : widgets) {
            if (widget instanceof UpdatingContentEditWidget) {
                ((UpdatingContentEditWidget) widget).displayOrUpdate(page, content, null);
            }
        }
    }
}
