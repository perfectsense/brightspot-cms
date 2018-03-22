<%@ page session="false" import="

com.psddev.cms.db.Content,
com.psddev.cms.db.ContentLock,
com.psddev.cms.db.Copyable,
com.psddev.cms.db.Overlay,
com.psddev.cms.db.OverlayProvider,
com.psddev.cms.db.Directory,
com.psddev.cms.db.Draft,
com.psddev.cms.db.Guide,
com.psddev.cms.db.GuidePage,
com.psddev.cms.db.History,
com.psddev.cms.db.Page,
com.psddev.cms.db.PageFilter,
com.psddev.cms.db.Renderer,
com.psddev.cms.db.Schedule,
com.psddev.cms.db.Site,
com.psddev.cms.db.Template,
com.psddev.cms.db.ToolUi,
com.psddev.cms.db.ToolUser,
com.psddev.cms.db.Variation,
com.psddev.cms.db.Workflow,
com.psddev.cms.db.WorkflowLog,
com.psddev.cms.db.WorkflowState,
com.psddev.cms.db.WorkflowTransition,
com.psddev.cms.db.WorkInProgress,
com.psddev.cms.db.WorkStream,
com.psddev.cms.tool.CmsTool,
com.psddev.cms.tool.ContentEditWidgetDisplay,
com.psddev.cms.tool.ToolPageContext,
com.psddev.cms.tool.Widget,
com.psddev.cms.tool.page.content.Edit,
com.psddev.cms.tool.GalleryDisplay,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Query,
com.psddev.dari.db.Singleton,
com.psddev.dari.db.State,
com.psddev.dari.util.HtmlWriter,
com.psddev.dari.util.JspUtils,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.Settings,
com.psddev.dari.util.StringUtils,
com.psddev.cms.tool.ContentEditable,

java.io.StringWriter,
java.util.ArrayList,
java.util.Date,
java.util.LinkedHashMap,
java.util.List,
java.util.Map,
java.util.Set,
java.util.UUID,

org.joda.time.DateTime,
com.google.common.collect.ImmutableMap" %>
<%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requireUser()) {
    return;
}

Object selected = wp.findOrReserve();
if (selected == null) {
    wp.writeHeader();
    wp.writeStart("div", "class", "message message-warning");
    wp.writeHtml(wp.localize(
            "com.psddev.cms.tool.page.content.Edit",
            ImmutableMap.of("queryString", request.getQueryString()),
            "message.missing"));
    wp.writeEnd();
    wp.writeFooter();
    return;
}

State state = State.getInstance(selected);
Site site = wp.getSite();

if (selected != null) {
    if (!(site == null || Site.Static.isObjectAccessible(site, selected))) {
        wp.writeHeader();
        wp.writeStart("div", "class", "message message-warning");
        wp.writeHtml(wp.localize(
                "com.psddev.cms.tool.page.content.Edit",
                ImmutableMap.of(
                        "typeLabel", wp.getTypeLabel(selected),
                        "objectLabel", wp.getObjectLabel(selected),
                        "siteName", site.getName()
                ),
                "message.notAccessible"));
        wp.writeEnd();
        wp.writeFooter();
        return;
    }
}

UUID variationId = wp.param(UUID.class, ToolPageContext.VARIATION_ID_PARAMETER);

if (site != null) {
    Variation defaultVariation = site.getDefaultVariation();

    if (defaultVariation != null && !defaultVariation.getId().equals(variationId)) {
        wp.redirect("", "variationId", defaultVariation.getId());
        return;
    }
}

Template template = null;
if (selected != null) {
    template = state.as(Template.ObjectModification.class).getDefault();
}

UUID newTypeId = wp.uuidParam("newTypeId");
if (newTypeId != null) {
    state.setTypeId(newTypeId);
}

Object editing = selected;
Object sectionContent = null;
if (selected instanceof Page) {
    sectionContent = Query.findById(Object.class, wp.uuidParam("contentId"));
    if (sectionContent != null) {
        editing = sectionContent;

        if (variationId != null) {
            State editingState = State.getInstance(editing);
            @SuppressWarnings("unchecked")
            Map<String, Object> variationValues = (Map<String, Object>) editingState.getValue("variations/" + variationId.toString());

            if (variationValues != null) {
                editingState.setValues(variationValues);
            }
        }
    }
}

Map<String, Object> editingOldValues = Draft.findOldValues(editing);
WorkStream workStream = Query.from(WorkStream.class).where("_id = ?", wp.param(UUID.class, "workStreamId")).first();

if (workStream != null) {

    Draft draft = wp.getOverlaidDraft(editing);
    Object workstreamObject = (draft != null) ? draft : editing;

    if (wp.param(boolean.class, "action-skipWorkStream")) {
        workStream.skip(wp.getUser(), workstreamObject);
        wp.redirect("", "action-skipWorkStream", null);
        return;

    } else if (wp.param(boolean.class, "action-stopWorkStream")) {
        workStream.stop(wp.getUser());
        wp.redirect("/", "reason", "stop-work-stream");
        return;
    } else if (workStream.countIncomplete() == 0) {
        wp.writeHeader();
            wp.writeStart("div", "class", "message message-success");
                wp.writeHtml(wp.localize(WorkStream.class, "message.complete"));
            wp.writeEnd();
        wp.writeFooter();
        return;
    }

    State.getInstance(workstreamObject).as(WorkStream.Data.class).complete(workStream, wp.getUser());
}

// Only permit copy if the copy source object is accessible to the current Site
Object copy = Query.findById(Object.class, wp.uuidParam("copyId"));
if (copy != null) {

    if (site != null && !Site.Static.isObjectAccessible(site, copy)) {
        wp.writeHeader();
        wp.writeStart("div", "class", "message message-warning");
        wp.writeHtml(wp.localize(
                "com.psddev.cms.tool.page.content.Edit",
                ImmutableMap.of(
                        "typeLabel", wp.getTypeLabel(copy),
                        "objectLabel", wp.getObjectLabel(copy),
                        "siteName", site.getName()
                ),
                "message.notAccessible"));
        wp.writeEnd();
        wp.writeFooter();
        return;
    }
}

State editingState = State.getInstance(editing);

// When a copy is specified as part of a POST, overlay the editingState on top of
// the copyState to retain non-displaying State values from the original copy.
if (wp.isFormPost() && copy != null && editingState.isNew()) {

    State copyState = State.getInstance(Copyable.copy(copy));

    if (site != null
            && !Settings.get(boolean.class, "cms/tool/copiedObjectInheritsSourceObjectsSiteOwner")) {
        // Only set the owner to current site if not on global and no setting to dictate otherwise.
        copyState.as(Site.ObjectModification.class).setOwner(site);
    }

    copyState.setId(editingState.getId());
    copyState.setStatus(editingState.getStatus());
    state = copyState;
    editing = state.getOriginalObject();
    selected = editing;
}

if (wp.tryDelete(editing) ||
        wp.tryNewDraft(editing) ||
        wp.tryDraft(editing) ||
        wp.tryPublish(editing) ||
        wp.tryRestore(editing) ||
        wp.tryTrash(editing) ||
        wp.tryMerge(editing) ||
        wp.tryWorkflow(editing) ||
        wp.tryUnschedule(editing)) {
    return;
}

// Only copy on a GET request to the page.  Subsequent POSTs should not overwrite
// the editing state with the copy source state again.
if (!wp.isFormPost() && copy != null && editingState.isNew()) {

    state = State.getInstance(Copyable.copy(copy));

    if (site != null
            && !Settings.get(boolean.class, "cms/tool/copiedObjectInheritsSourceObjectsSiteOwner")) {
        // Only set the owner to current site if not on global and no setting to dictate otherwise.
        state.as(Site.ObjectModification.class).setOwner(site);
    }

    editing = state.getOriginalObject();
    selected = editing;
}

// Directory directory = Query.findById(Directory.class, wp.uuidParam("directoryId"));
History history = wp.getOverlaidHistory(editing);
Draft draft = wp.getOverlaidDraft(editing);
Set<ObjectType> compatibleTypes = ToolUi.getCompatibleTypes(State.getInstance(editing).getType());
ToolUser user = wp.getUser();
ContentLock contentLock = null;
boolean lockedOut = false;
boolean editAnyway = wp.param(boolean.class, "editAnyway");
boolean optInLock = wp.param(boolean.class, "lock");

if (!wp.getCmsTool().isDisableContentLocking()) {
    if (wp.getCmsTool().isOptInContentLocking()) {
        if (optInLock && wp.hasPermission("type/" + editingState.getTypeId() + "/write")) {
            contentLock = ContentLock.Static.lock(editing, null, user);
            lockedOut = !user.equals(contentLock.getOwner());

        } else {
            contentLock = ContentLock.Static.findLock(editing, null);
            lockedOut = contentLock != null && !user.equals(contentLock.getOwner());
        }

    } else if (wp.hasPermission("type/" + editingState.getTypeId() + "/write")) {
        contentLock = ContentLock.Static.lock(editing, null, user);
        lockedOut = !user.equals(contentLock.getOwner());
    }
}

// --- Presentation ---

Content.ObjectModification contentData = editingState.as(Content.ObjectModification.class);
Object oldObject = Query.fromAll().where("_id = ?", editingState.getId()).noCache().first();
boolean visible = false;

if (oldObject != null) {
    visible = State.getInstance(oldObject).isVisible();
}

%>
<%
wp.writeHeader(editingState.getType() != null ? editingState.getType().getLabel() : null);
%>
<div class="content-edit"<%= wp.getCmsTool().isHorizontalSearchCarousel() ? "" : " data-vertical-carousel" %>>
<%

    String search = wp.param(String.class, "search");

    if (search != null) {
        wp.writeStart("div", "class", "frame");
            wp.writeStart("a",
                    "href", wp.cmsUrl("/searchCarousel",
                            "id", editingState.getId(),
                            "search", search,
                            "draftId", wp.param(UUID.class, "draftId")));
            wp.writeEnd();
        wp.writeEnd();
    }

    Overlay overlay = Edit.getOverlay(editing);
    OverlayProvider overlayProvider = overlay != null ? overlay.getOverlayProvider() : null;

    Edit.writeOverlayProviderSelect(wp, editing, overlayProvider);
%>
    <form class="contentForm contentLock"
            method="post"
            enctype="multipart/form-data"
            action="<%= wp.objectUrl("", selected,
                    ToolPageContext.DELETE_ACTION_PARAMETER, null,
                    ToolPageContext.TRASH_ACTION_PARAMETER, null,
                    ToolPageContext.DRAFT_ACTION_PARAMETER, null,
                    ToolPageContext.NEW_DRAFT_ACTION_PARAMETER, null,
                    ToolPageContext.PUBLISH_ACTION_PARAMETER, null,
                    ToolPageContext.RESTORE_ACTION_PARAMETER, null,
                    ToolPageContext.SAVE_ACTION_PARAMETER, null,
                    ToolPageContext.DRAFT_ACTION_PARAMETER, null,
                    ToolPageContext.MERGE_ACTION_PARAMETER, null,
                    ToolPageContext.RESTORE_ACTION_PARAMETER, null,
                    ToolPageContext.WORKFLOW_ACTION_PARAMETER, null,
                    ToolPageContext.UNSCHEDULE_ACTION_PARAMETER, null,
                    "published", null) %>"
            autocomplete="off"
            <% if (!wp.getCmsTool().isDisableFieldLocking()) { %>
            data-rtc-content-id="<%= draft != null ? draft.getId() : editingState.getId() %>"
            <% } %>
            data-new="<%= State.getInstance(editing).isNew() %>"
            data-o-id="<%= State.getInstance(selected).getId() %>"
            data-o-label="<%= wp.h(State.getInstance(selected).getLabel()) %>"
            data-o-preview="<%= wp.h(wp.getPreviewThumbnailUrl(selected)) %>"
            data-object-id="<%= State.getInstance(editing).getId() %>"
            data-content-locked-out="<%= lockedOut && !editAnyway %>"
            data-content-id="<%= State.getInstance(editing).getId() %>"
            <% if (overlay != null) { %>data-overlay-differences="<%= wp.h(ObjectUtils.toJson(overlay.getDifferences())) %>"<% } %>>

        <input type="hidden" name="<%= editingState.getId() %>/oldValues" value="<%= wp.h(ObjectUtils.toJson(editingOldValues)) %>">

        <div class="contentForm-main">
            <div class="widget widget-content">
                <h1 class="breadcrumbs"><%

                    wp.writeStart("span", "class", "breadcrumbItem icon icon-object");
                        if (overlayProvider != null) {
                            wp.writeTypeObjectLabel(overlayProvider);
                            wp.writeHtml(" - ");
                        }

                        if (state.isNew()) {
                            wp.writeHtml("New");

                        } else {
                            wp.writeHtml("Edit");
                        }

                        wp.writeHtml(" ");

                        if (compatibleTypes.size() < 2) {
                            wp.write(wp.objectLabel(state.getType()));

                        } else {
                            wp.write("<select name=\"newTypeId\">");
                                for (ObjectType type : compatibleTypes) {
                                    wp.write("<option");
                                    wp.write(state.getType().equals(type) ? " selected" : "");
                                    wp.write(" value=\"");
                                    wp.write(type.getId());
                                    wp.write("\">");
                                    wp.write(wp.objectLabel(type));
                                    wp.write("</option>");
                                }
                            wp.write("</select>");
                        }

                        wp.write(": " );

                        wp.writeStart("span", "class", "ContentLabel", "data-dynamic-html", "${toolPageContext.createObjectLabelHtml(content)}");
                            wp.write(wp.createObjectLabelHtml(editing));
                        wp.writeEnd();
                    wp.writeEnd();

                    if (selected instanceof Page &&
                            ((Page) selected).getLayout() != null) {
                        wp.writeStart("span", "class", "breadcrumbItem");
                            wp.write("<a class=\"icon icon-object-template\" href=\"");
                            wp.write(wp.returnableUrl("/content/editableSections.jsp", "id", State.getInstance(selected).getId()));
                            wp.write("\" target=\"contentPageSections-");
                            wp.write(state.getId());
                            wp.write("\">");
                                if (sectionContent != null) {
                                    wp.write(wp.objectLabel(State.getInstance(editing).getType()));
                                } else {
                                    wp.write("Layout");
                                }
                            wp.write("</a>");
                        wp.writeEnd();
                    }

                    wp.include("/WEB-INF/objectVariation.jsp", "object", editing);
                %></h1>

                <div class="widgetControls">
                    <a class="icon icon-action-edit widgetControlsEditInFull" target="_blank" href="<%= wp.url("",
                                                                                                        ToolPageContext.DELETE_ACTION_PARAMETER, null,
                                                                                                        ToolPageContext.TRASH_ACTION_PARAMETER, null,
                                                                                                        ToolPageContext.DRAFT_ACTION_PARAMETER, null,
                                                                                                        ToolPageContext.NEW_DRAFT_ACTION_PARAMETER, null,
                                                                                                        ToolPageContext.PUBLISH_ACTION_PARAMETER, null,
                                                                                                        ToolPageContext.RESTORE_ACTION_PARAMETER, null,
                                                                                                        ToolPageContext.SAVE_ACTION_PARAMETER, null,
                                                                                                        ToolPageContext.DRAFT_ACTION_PARAMETER, null,
                                                                                                        ToolPageContext.MERGE_ACTION_PARAMETER, null,
                                                                                                        ToolPageContext.RESTORE_ACTION_PARAMETER, null,
                                                                                                        ToolPageContext.WORKFLOW_ACTION_PARAMETER, null,
                                                                                                        ToolPageContext.UNSCHEDULE_ACTION_PARAMETER, null) %>">
                        <%= wp.h(wp.localize("com.psddev.cms.tool.page.content.Edit", "action.editFull"))%>
                    </a>
                    <% if (wp.getCmsTool().isEnableAbTesting()) { %>
                        <a class="icon icon-beaker" href="<%= wp.url("", "ab", !wp.param(boolean.class, "ab")) %>">A/B</a>
                    <% } %>
                    <%
                    GuidePage guide = Guide.Static.getPageTypeProductionGuide(state.getType());
                    if (guide != null && guide.getDescription() != null && !guide.getDescription().isEmpty()) {
                        wp.write("<a class=\"icon icon-object-guide\" target=\"guideType\" href=\"", wp.objectUrl("/content/guideType.jsp", selected, "pageGuideId", guide.getId(),  "popup", true), "\">PG</a>");
                    }
                    %>
                </div>

                <% if (!State.getInstance(editing).isNew() &&
                        !(editing instanceof com.psddev.dari.db.Singleton)
                        && !State.getInstance(editing).getType().as(ToolUi.class).isReadOnly()) { %>
                    <div class="widget-contentCreate">
                        <div class="action action-create">
                            <%= wp.h(wp.localize("com.psddev.cms.tool.page.content.Edit", "action.new"))%>
                        </div>
                        <ul>
                            <li>
                                <a class="action action-create" href="<%= wp.url("/content/edit.jsp",
                                    "typeId", State.getInstance(editing).getTypeId(),
                                    "templateId", template != null ? template.getId() : null)%>">
                                    <%= wp.h(wp.localize(
                                            editingState.getType(),
                                            "action.newType"))%>
                                </a>
                            </li>
                            <li>
                                <a class="action action-copy" href="<%= wp.url("/content/edit.jsp",
                                    "typeId", State.getInstance(editing).getTypeId(),
                                    "templateId", template != null ? template.getId() : null,
                                    "copyId", State.getInstance(editing).getId())
                                    %>" target="_top">
                                    <%= wp.h(wp.localize(
                                            editingState.getType(),
                                            "action.copy"))%>
                                </a>
                            </li>
                        </ul>
                    </div>
                <% } %>

                <% if (sectionContent != null) { %>
                    <p><a class="icon icon-arrow-left" href="<%= wp.url("", "contentId", null) %>">Back to Layout</a></p>
                <% } %>

                <%
                wp.include("/WEB-INF/objectMessage.jsp", "object", editing);

                Edit.restoreWorkInProgress(wp, editing);

                Object compareObject = null;

                if (wp.param(boolean.class, "compare")) {
                    compareObject = user.createCompareObject();
                }

                if (compareObject != null) {
                    wp.writeStart("div", "class", "message message-info");
                        wp.writeStart("a",
                                "href", wp.url("", "compare", null));
                            wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Edit", "action.stopCompare"));
                        wp.writeEnd();
                    wp.writeEnd();

                    wp.writeStart("div", "class", "contentDiff");
                        wp.writeStart("div", "class", "contentDiffOld contentDiffLeft");
                            wp.writeStart("h2");
                            wp.writeObjectLabel(compareObject);
                            wp.writeEnd();
                            wp.writeSomeFormFields(compareObject, true, null, null);
                        wp.writeEnd();

                        try {
                            wp.disableFormFields();

                            wp.writeStart("div", "class", "contentDiffCurrent contentDiffRight");
                                wp.writeStart("h2");
                                    wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Edit", "subtitle.current"));
                                wp.writeEnd();
                                wp.writeSomeFormFields(editing, true, null, null);
                            wp.writeEnd();

                        } finally {
                            wp.enableFormFields();
                        }
                    wp.writeEnd();

                } else if (history != null || (draft != null && !draft.isNewContent())) {
                    State original = State.getInstance(Query.
                            from(Object.class).
                            where("_id = ?", editing).
                            noCache().
                            first());

                    if (original != null) {
                        wp.writeStart("div", "class", "contentDiff");
                            if (history != null) {
                                wp.writeStart("div", "class", "contentDiffOld contentDiffLeft");
                                    wp.writeStart("h2");
                                        wp.writeObjectLabel(ObjectType.getInstance(History.class));
                                    wp.writeEnd();
                                    wp.writeSomeFormFields(editing, true, null, null);
                                wp.writeEnd();
                            }

                            try {
                                wp.disableFormFields();

                                wp.writeStart("div", "class", "contentDiffCurrent " + (history != null ? "contentDiffRight" : "contentDiffLeft"));
                                    wp.writeStart("h2");
                                        wp.writeHtml(wp.localize(editingState.getType(), "subtitle.current"));
                                    wp.writeEnd();
                                    wp.writeSomeFormFields(original.getOriginalObject(), true, null, null);
                                wp.writeEnd();

                            } finally {
                                wp.enableFormFields();
                            }

                            if (draft != null) {
                                wp.writeStart("div", "class", "contentDiffNew contentDiffRight");
                                    wp.writeStart("h2");
                                        wp.writeObjectLabel(ObjectType.getInstance(Draft.class));
                                    wp.writeEnd();
                                    wp.writeSomeFormFields(editing, true, null, null);
                                wp.writeEnd();
                            }
                        wp.writeEnd();

                    } else {
                        wp.writeSomeFormFields(editing, true, null, null);
                    }

                } else {
                    wp.writeSomeFormFields(editing, true, null, null);
                }
                %>
            </div>

            <% renderWidgets(wp, editing, CmsTool.CONTENT_BOTTOM_WIDGET_POSITION); %>
        </div>

        <div class="contentForm-aside">
            <%
                ObjectType editingType = editingState.getType();
                boolean publishable = editingType != null && editingType.as(ToolUi.class).isPublishable();
            %>

            <div class="widget widget-publishing"<%= publishable ? " data-publishable" : "" %>>
                <h1 class="icon icon-action-publish" data-rtc-edit-field-update-other-viewers><%= wp.h(wp.localize(editingState.getType(), publishable ? "action.publish" : "action.save")) %></h1>

                <%
                wp.writeStart("div", "class", "widget-controls");
                    if (!wp.getCmsTool().isDisableContentLocking() && !lockedOut && wp.getCmsTool().isOptInContentLocking()) {
                        wp.writeStart("a",
                                "class", "icon icon-only icon-" + (optInLock ? "lock" : "unlock"),
                                "href", wp.url("", "lock", !optInLock));
                            wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Edit", "action.lock"));
                        wp.writeEnd();
                    }

                    wp.writeStart("a",
                            "class", "widget-publishing-tools",
                            "href", wp.objectUrl("/contentTools", editing, "returnUrl", wp.url("")),
                            "target", "contentTools");
                        wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Edit", "action.tools"));
                    wp.writeEnd();
                wp.writeEnd();

                if (workStream != null) {
                    long skipped = workStream.countSkipped(user);
                    long complete = workStream.countComplete();
                    long incomplete = workStream.countIncomplete() - skipped;
                    long total = complete + incomplete + skipped;

                    wp.writeStart("div",
                            "class", "publishing-workflow block",
                            "style", wp.cssString(
                                    "border-bottom", "1px solid #bbb",
                                    "padding-bottom", "5px"));
                        wp.writeStart("a",
                                "href", wp.url("/workStreamUsers", "id", workStream.getId()),
                                "target", "workStream");
                            wp.writeHtml(workStream.getUsers().size() - 1);
                            wp.writeHtml(" others");
                        wp.writeEnd();

                        wp.writeHtml(" working on ");

                        wp.writeStart("a",
                                "href", wp.objectUrl("/content/editWorkStream", workStream),
                                "target", "workStream");
                            wp.writeObjectLabel(workStream);
                        wp.writeEnd();

                        wp.writeHtml(" with you");

                        wp.writeStart("div", "class", "progress", "style", "margin: 5px 0;");
                            wp.writeStart("div", "class", "progressBar", "style", "width:" + ((total - incomplete) * 100.0 / total) + "%");
                            wp.writeEnd();

                            wp.writeStart("strong");
                                wp.writeHtml(incomplete);
                            wp.writeEnd();

                            wp.writeHtml(" of ");

                            wp.writeStart("strong");
                                wp.writeHtml(total);
                            wp.writeEnd();

                            wp.writeHtml(" left ");

                            if (complete > 0L || skipped > 0L) {
                                wp.writeHtml("(");
                            }

                            if (complete > 0L) {
                                wp.writeStart("strong");
                                    wp.writeHtml(complete);
                                wp.writeEnd();

                                wp.writeHtml(" complete");

                                if (skipped > 0L) {
                                    wp.writeHtml(", ");
                                }
                            }

                            if (skipped > 0L) {
                                wp.writeStart("strong");
                                    wp.writeHtml(skipped);
                                wp.writeEnd();

                                wp.writeHtml(" skipped");
                            }

                            if (complete > 0L || skipped > 0L) {
                                wp.writeHtml(")");
                            }

                        wp.writeEnd();

                        wp.writeStart("ul", "class", "piped");
                            wp.writeStart("li");
                                wp.writeStart("a",
                                        "class", "icon icon-step-forward",
                                        "href", wp.url("", "action-skipWorkStream", "true"));
                                    wp.writeHtml("Skip");
                                wp.writeEnd();
                            wp.writeEnd();

                            wp.writeStart("li");
                                wp.writeStart("a",
                                        "class", "icon icon-stop",
                                        "href", wp.url("", "action-stopWorkStream", "true"));
                                    wp.writeHtml("Stop");
                                wp.writeEnd();
                            wp.writeEnd();
                        wp.writeEnd();
                    wp.writeEnd();
                }

                boolean isWritable = wp.hasPermission("type/" + editingState.getTypeId() + "/write")
                        && !editingState.getType().as(ToolUi.class).isReadOnly()
                        && ContentEditable.shouldContentBeEditable(editing);
                boolean isDraft = !editingState.isNew() && (contentData.isDraft() || draft != null);
                boolean isHistory = history != null;
                boolean isTrash = contentData.isTrash();
                Schedule schedule = draft != null ? draft.getSchedule() : null;
                boolean displayWorkflowSave = false;

                if (isWritable) {

                    // Message and actions if the content is a draft.
                    if (isDraft) {
                        Content.ObjectModification draftContentData = State.
                                getInstance(draft != null ? draft : editing).
                                as(Content.ObjectModification.class);

                        wp.writeStart("div", "class", "message message-warning");
                            wp.writeStart("p");
                                wp.writeStart("span", "class", "visibilityLabel widget-publishingWorkflowState");
                                    if (draft != null && !draft.isNewContent()) {
                                        wp.writeObjectLabel(ObjectType.getInstance(Draft.class));

                                        String draftName = draft.getName();

                                        if (!ObjectUtils.isBlank(draftName)) {
                                            wp.writeHtml(" (");
                                            wp.writeHtml(draftName);
                                            wp.writeHtml(")");
                                        }

                                    } else {
                                        wp.writeHtml("Initial Draft");
                                    }
                                wp.writeEnd();


                                wp.writeHtml(" by ");
                                wp.writeObjectLabel(draftContentData.getUpdateUser());
                                wp.writeHtml(" at ");
                                wp.writeHtml(wp.formatUserDateTime(draftContentData.getUpdateDate()));
                            wp.writeEnd();
                        wp.writeEnd();

                        if (schedule != null) {
                            Date triggerDate = schedule.getTriggerDate();
                            ToolUser triggerUser = schedule.getTriggerUser();

                            if (triggerDate != null && triggerUser != null) {
                                wp.writeStart("div", "class", "message message-warning");
                                    wp.writeStart("p");
                                        wp.writeHtml(wp.localize(editingType,
                                                ImmutableMap.of("publishTime", wp.formatUserDateTime(triggerDate),
                                                        "publishUser", wp.createObjectLabelHtml(triggerUser)),
                                                "message.scheduled"));
                                    wp.writeEnd();
                                wp.writeEnd();
                            }
                        }

                    // Message and actions if the content is a past revision.
                    } else if (isHistory) {
                        String historyName = history.getName();
                        boolean hasHistoryName = !ObjectUtils.isBlank(historyName);

                        wp.writeStart("div", "class", "message message-warning");
                            wp.writeStart("p");
                                if (hasHistoryName) {
                                    wp.writeHtml(historyName);
                                    wp.writeHtml(" - ");
                                }

                                wp.writeHtml("Past revision saved ");
                                wp.writeHtml(wp.formatUserDateTime(history.getUpdateDate()));
                                wp.writeHtml(" by ");
                                wp.writeObjectLabel(history.getUpdateUser());
                                wp.writeHtml(".");
                            wp.writeEnd();

                            wp.writeStart("div", "class", "actions");
                                wp.writeStart("a",
                                        "class", "icon icon-action-edit",
                                        "href", wp.url("", "historyId", null));
                                    wp.writeHtml("Live");
                                wp.writeEnd();

                                wp.writeHtml(" ");

                                wp.writeStart("a",
                                        "class", "icon icon-object-history",
                                        "href", wp.url("/historyEdit", "id", history.getId()),
                                        "target", "historyEdit");
                                    wp.writeHtml(hasHistoryName ? "Rename" : "Name");
                                    wp.writeHtml(" Revision");
                                wp.writeEnd();
                            wp.writeEnd();
                        wp.writeEnd();

                    // Message and actions if the content is a trash.
                    } else if (isTrash) {
                        wp.writeStart("div", "class", "message message-warning");
                            wp.writeStart("p");
                                wp.writeHtml("Archived ");
                                wp.writeHtml(wp.formatUserDateTime(contentData.getUpdateDate()));
                                wp.writeHtml(" by ");
                                wp.writeObjectLabel(contentData.getUpdateUser());
                                wp.writeHtml(".");
                            wp.writeEnd();
                        wp.writeEnd();
                    }

                    if (lockedOut) {
                        wp.writeStart("div", "class", "message message-warning");
                            wp.writeStart("p");
                                wp.writeHtml(editAnyway ? "Ignoring lock by " : "Locked by ");
                                wp.writeObjectLabel(contentLock.getOwner());
                                wp.writeHtml(" since ");
                                wp.writeHtml(wp.formatUserDateTime(contentLock.getCreateDate()));
                                wp.writeHtml(".");
                            wp.writeEnd();

                            if (!editAnyway && !wp.getCmsTool().isOptInContentLocking()) {
                                wp.writeStart("div", "class", "actions");
                                    wp.writeStart("a",
                                            "class", "icon icon-unlock",
                                            "href", wp.url("", "editAnyway", true));
                                        wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Edit", "action.ignoreLock"));
                                    wp.writeEnd();
                                wp.writeEnd();
                            }
                        wp.writeEnd();

                    } else {
                        %><script type="text/javascript">
                            require([ 'content/lock' ], function(lock) {
                                var unlocked = false;

                                setInterval(function() {
                                    if (!unlocked) {
                                        lock.lock('<%= editingState.getId() %>');
                                    }
                                }, 1000);

                                $(window).bind('beforeunload', function() {
                                    unlocked = true;

                                    lock.unlock('<%= editingState.getId() %>');
                                });
                            });
                        </script><%
                    }

                    if (!lockedOut || editAnyway) {

                        // Workflow actions.
                        if (!isTrash &&
                                !(draft != null && draft.getSchedule() != null) &&
                                (editingState.isNew() ||
                                editingState.as(Content.ObjectModification.class).isDraft() ||
                                draft != null ||
                                editingState.as(Workflow.Data.class).getCurrentState() != null)) {

                            Workflow workflow = Workflow.findWorkflow(site, editingState);

                            if (workflow != null) {
                                State workflowParentState = draft != null ? draft.getState() : editingState;
                                Workflow.Data workflowData = workflowParentState.as(Workflow.Data.class);
                                String currentState = workflowData.getCurrentState();
                                Map<String, String> transitionNames = new LinkedHashMap<String, String>();

                                for (Map.Entry<String, WorkflowTransition> entry : workflow.getTransitionsFrom(currentState).entrySet()) {
                                    String transitionName = entry.getKey();

                                    if (wp.hasPermission("type/" + editingState.getTypeId() + "/" + transitionName)) {
                                        transitionNames.put(transitionName, entry.getValue().getDisplayName());
                                    }
                                }

                                if (currentState != null || !transitionNames.isEmpty()) {
                                    WorkflowLog log = Query.
                                            from(WorkflowLog.class).
                                            where("objectId = ?", workflowParentState.getId()).
                                            sortDescending("date").
                                            first();

                                    if (!ObjectUtils.isBlank(currentState)) {
                                        String workflowStateDisplayName = currentState;

                                        for (WorkflowState s : workflow.getStates()) {
                                            if (ObjectUtils.equals(s.getName(), currentState)) {
                                                workflowStateDisplayName = s.getDisplayName();
                                                break;
                                            }
                                        }

                                        wp.writeStart("div", "class", "widget-publishingWorkflowComment");
                                            wp.writeStart("div", "class", "message message-warning");
                                            wp.writeStart("span", "class", "visibilityLabel widget-publishingWorkflowState");
                                                wp.writeHtml(workflowStateDisplayName);
                                            wp.writeEnd();

                                            if (log != null) {
                                                String comment = log.getComment();

                                                wp.writeHtml(" ");
                                                wp.writeStart("a",
                                                        "target", "workflowLogs",
                                                        "href", wp.cmsUrl("/workflowLogs", "objectId", workflowParentState.getId()));
                                                    if (ObjectUtils.isBlank(comment)) {
                                                        wp.writeHtml("by ");

                                                    } else {
                                                        wp.writeStart("q");
                                                            wp.writeHtml(comment);
                                                        wp.writeEnd();
                                                        wp.writeHtml(" said ");
                                                    }

                                                    wp.writeHtml(log.getUserName());
                                                    wp.writeHtml(" at ");
                                                    wp.writeHtml(wp.formatUserDateTime(log.getDate()));
                                                wp.writeEnd();
                                            }

                                            if (draft == null
                                                    && wp.hasPermission("type/" + editingState.getTypeId() + "/workflow.saveAllowed." + currentState)) {

                                                displayWorkflowSave = true;
                                            }
                                            wp.writeEnd();
                                        wp.writeEnd();
                                    }

                                    if (!transitionNames.isEmpty()) {
                                        wp.writeStart("div", "class", "widget-publishingWorkflow");
                                            WorkflowLog newLog = editingState.as(Workflow.Data.class).getCurrentLog();

                                            if (newLog == null) {
                                                newLog = new WorkflowLog();
                                                newLog.getState().setId(wp.param(UUID.class, "workflowLogId"));
                                            }

                                            if (wp.isFormPost()) {
                                                wp.updateUsingParameters(newLog);
                                            }

                                            if (log != null) {
                                                for (ObjectField field : ObjectType.getInstance(WorkflowLog.class).getFields()) {
                                                    if (field.as(WorkflowLog.FieldData.class).isPersistent()) {
                                                        String name = field.getInternalName();

                                                        newLog.getState().put(name, log.getState().get(name));
                                                    }
                                                }
                                            }

                                            wp.writeStart("div", "class", "widget-publishingWorkflowLog");
                                                wp.writeElement("input",
                                                        "type", "hidden",
                                                        "name", "workflowLogId",
                                                        "value", newLog.getId());

                                                wp.writeFormFields(newLog);
                                            wp.writeEnd();

                                            if (!visible
                                                    && draft != null
                                                    && workflow.getTransitionsTo(editingState.as(Workflow.Data.class).getCurrentState())
                                                            .keySet()
                                                            .stream()
                                                            .filter(name -> wp.hasPermission("type/" + editingState.getTypeId() + "/" + name))
                                                            .findFirst()
                                                            .isPresent()) {
                                                wp.writeStart("button",
                                                        "name", "action-merge",
                                                        "value", "true");
                                                    wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Edit", "action.merge"));
                                                wp.writeEnd();
                                            }

                                            for (Map.Entry<String, String> entry : transitionNames.entrySet()) {
                                                wp.writeStart("button",
                                                        "name", "action-workflow",
                                                        "value", entry.getKey());
                                                    wp.writeHtml(entry.getValue());
                                                wp.writeEnd();
                                            }
                                        wp.writeEnd();
                                    }
                                }
                            }
                        }

                        // Publish and trash buttons.
                        if (!wp.hasPermission("type/" + editingState.getTypeId() + "/publish")) {
                            /*
                            wp.write("<div class=\"message message-warning\"><p>You cannot edit this ");
                            wp.write(wp.typeLabel(state));
                            wp.write("!</p></div>");
                            */

                        } else if (!isTrash) {
                            wp.writeStart("div", "class", "widget-publishingPublish");
                                if (publishable && wp.getUser().getCurrentSchedule() == null) {
                                    if (!contentData.isDraft() && schedule != null) {
                                        boolean newSchedule = wp.param(boolean.class, "newSchedule");

                                        wp.writeStart("div", "style", wp.cssString("margin-bottom", "5px"));
                                            wp.writeStart("select", "name", "newSchedule");
                                                wp.writeStart("option",
                                                        "selected", newSchedule ? null : "selected",
                                                        "value", "");
                                                    wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Edit", "option.updateExistingSchedule"));
                                                wp.writeEnd();

                                                wp.writeStart("option",
                                                        "selected", newSchedule ? "selected" : null,
                                                        "value", "true");
                                                    wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Edit", "option.createNewSchedule"));
                                                wp.writeEnd();
                                            wp.writeEnd();
                                        wp.writeEnd();
                                    }

                                    DateTime publishDate;
                                    String scheduleLabel;

                                    if (schedule != null) {
                                        publishDate = wp.toUserDateTime(schedule.getTriggerDate());
                                        scheduleLabel = wp.localize(editingType, "action.reschedule");

                                    } else {
                                        publishDate = wp.param(DateTime.class, "publishDate");
                                        scheduleLabel = wp.localize(editingType, "action.schedule");

                                        if (publishDate == null &&
                                                (isDraft ||
                                                editingState.as(Workflow.Data.class).getCurrentState() != null)) {
                                            Date pd = editingState.as(Content.ObjectModification.class).getScheduleDate();

                                            if (pd != null) {
                                                publishDate = new DateTime(pd);
                                            }
                                        }
                                    }

                                    wp.writeElement("input",
                                            "type", "text",
                                            "class", "date dateInput",
                                            "data-emptylabel", "Now",
                                            "data-schedule-label", scheduleLabel,
                                            "name", "publishDate",
                                            "size", 9,
                                            "value", publishDate != null ? publishDate.getMillis() : null);
                                }

                                wp.writeStart("button",
                                        "name", "action-publish",
                                        "data-schedule-label", wp.localize(editingType, "action.schedule"),
                                        "value", "true");
                                    ObjectType type = editingState.getType();
                                    if (type != null) {
                                        wp.writeHtml(ObjectUtils.firstNonBlank(type.as(ToolUi.class).getPublishButtonText(), wp.localize(type, publishable ? "action.publish" : "action.save")));
                                    } else {
                                        wp.writeHtml(wp.localize(type, publishable ? "action.publish" : "action.save"));
                                    }
                                wp.writeEnd();
                            wp.writeEnd();
                        }
                    }
                }

                wp.writeStart("div", "class", "widget-publishingExtra");
                    wp.writeStart("ul", "class", "widget-publishingExtra-left");
                        if (publishable && (overlay == null && (!lockedOut || editAnyway) && isWritable)) {
                            if (isDraft) {
                                if (schedule == null) {
                                    wp.writeStart("li");
                                        wp.writeStart("button",
                                                "class", "link icon icon-action-save",
                                                "name", "action-draft",
                                                "value", "true");
                                            wp.writeHtml(wp.localize(editingState.getType(), "action.save.draft"));
                                        wp.writeEnd();
                                    wp.writeEnd();

                                    wp.writeStart("li", "class", "DraftAndReturnAction");
                                        wp.writeStart("button",
                                                "class", "link icon icon-action-save",
                                                "name", "action-draftAndReturn",
                                                "value", "true");
                                            wp.writeHtml(wp.localize(editingState.getType(), "action.saveAndReturn.draft"));
                                        wp.writeEnd();
                                    wp.writeEnd();
                                }

                                if (draft != null && !draft.isNewContent()) {
                                    wp.writeStart("li");
                                        wp.writeStart("a",
                                                "class", "icon icon-arrow-left",
                                                "href", wp.url("", "draftId", null));
                                            wp.writeHtml(wp.localize(editingState.getType(),
                                                    !visible ? "action.backToInitialDraft" : "action.backToLive"));
                                        wp.writeEnd();
                                    wp.writeEnd();
                                }

                            } else if (editingState.isVisible()) {
                                wp.writeStart("li");
                                    wp.writeStart("button",
                                            "class", "link icon icon-object-draft",
                                            "name", "action-newDraft",
                                            "value", "true");
                                        wp.writeHtml(wp.localize(
                                                editingState.getType(),
                                                editingState.isNew()
                                                        ? "action.new.initialDraft"
                                                        : "action.new.draft"));
                                    wp.writeEnd();
                                wp.writeEnd();

                                wp.writeStart("li", "class", "NewDraftAndReturnAction");
                                    wp.writeStart("button",
                                            "class", "link icon icon-object-draft",
                                            "name", "action-newDraftAndReturn",
                                            "value", "true");
                                        wp.writeHtml(wp.localize(editingState.getType(), "action.newAndReturn.draft"));
                                    wp.writeEnd();
                                wp.writeEnd();

                            } else if (displayWorkflowSave) {
                                wp.writeStart("li");
                                    wp.writeStart("button",
                                            "class", "link icon icon-action-save",
                                            "name", "action-draft",
                                            "value", "true");
                                        wp.writeHtml(wp.localize(editingState.getType(), "action.save.draft"));
                                    wp.writeEnd();
                                wp.writeEnd();

                                wp.writeStart("li", "class", "DraftAndReturnAction");
                                    wp.writeStart("button",
                                            "class", "link icon icon-action-save",
                                            "name", "action-draftAndReturn",
                                            "value", "true");
                                        wp.writeHtml(wp.localize(editingState.getType(), "action.saveAndReturn.draft"));
                                    wp.writeEnd();
                                wp.writeEnd();
                            }
                        }

                        if (isTrash && wp.hasPermission("type/" + state.getType().getId() + "/restore")) {
                            wp.writeStart("li");
                                wp.writeStart("button",
                                        "class", "link icon icon-action-restore",
                                        "name", "action-restore",
                                        "value", "true");
                                    wp.writeHtml("Restore");
                                wp.writeEnd();
                            wp.writeEnd();
                        }
                    wp.writeEnd();

                    if (isWritable && overlay != null && !overlay.getState().isNew()) {
                        wp.writeStart("ul", "class", "widget-publishingExtra-right");
                            wp.writeStart("li");
                                wp.writeStart("button",
                                        "class", "link icon icon-action-delete",
                                        "name", "action-delete",
                                        "value", "true");
                                wp.writeHtml(wp.localize(
                                        overlay.getState().getType(),
                                        "action.delete.type"));
                                wp.writeEnd();
                            wp.writeEnd();
                        wp.writeEnd();

                    } else if (overlay == null && (!lockedOut || editAnyway) && isWritable) {
                        wp.writeStart("ul", "class", "widget-publishingExtra-right");
                            if (isWritable && isDraft) {
                                if (schedule != null) {
                                    wp.writeStart("button",
                                            "class", "link icon icon-action-cancel",
                                            "name", "action-unschedule",
                                            "value", "true");
                                        wp.writeHtml(wp.localize(editingState.getType(), "action.unschedule"));
                                    wp.writeEnd();
                                }

                                wp.writeStart("li");
                                    wp.writeStart("button",
                                            "class", "link icon icon-action-delete",
                                            "name", "action-delete",
                                            "value", "true");
                                        wp.writeHtml(wp.localize(
                                                editingState.getType(),
                                                draft != null && !draft.isNewContent()
                                                        ? "action.delete.draft"
                                                        : "action.delete"));
                                    wp.writeEnd();
                                wp.writeEnd();
                            }

                            if (wp.hasPermission("type/" + editingState.getTypeId() + "/archive") &&
                                    !isDraft &&
                                    !isHistory &&
                                    !editingState.isNew() &&
                                    (editingState.getType() == null ||
                                    !editingState.getType().getGroups().contains(Singleton.class.getName()))) {

                                wp.writeStart("li");
                                    if (displayWorkflowSave) {
                                        wp.writeStart("button",
                                                "class", "link icon icon-action-delete",
                                                "name", "action-delete",
                                                "value", "true");
                                            wp.writeHtml(wp.localize(editingState.getType(), "action.delete"));
                                        wp.writeEnd();

                                    } else if (!isTrash) {
                                        wp.writeStart("button",
                                                "class", "link icon icon-action-trash",
                                                "name", "action-trash",
                                                "value", "true");
                                            wp.writeHtml(wp.localize(editingState.getType(), "action.archive"));
                                        wp.writeEnd();
                                    }
                                wp.writeEnd();
                            }

                            if (isTrash && wp.hasPermission("type/" + state.getType().getId() + "/delete")) {
                                wp.writeStart("li");
                                    wp.writeStart("button",
                                            "class", "link icon icon-action-delete",
                                            "name", "action-delete",
                                            "value", "true");
                                        wp.writeHtml("Delete Permanently");
                                    wp.writeEnd();
                                wp.writeEnd();
                            }
                        wp.writeEnd();
                    }
                wp.writeEnd();

                Date updateDate = contentData.getUpdateDate();

                if (updateDate != null) {
                    wp.writeStart("div", "class", "widget-publishingHistory");
                        wp.writeStart("p");
                            wp.writeHtml(wp.localize(editingType,
                                    ImmutableMap.of("publishUser", wp.createObjectLabelHtml(contentData.getUpdateUser()),
                                            "publishTime", wp.formatUserDateTime(updateDate)),
                                    "message.published"));
                        wp.writeEnd();
                    wp.writeEnd();
                }

                %>
            </div>

            <% renderWidgets(wp, editing, CmsTool.CONTENT_RIGHT_WIDGET_POSITION); %>
        </div>
    </form>
</div>

<% if (wp.isPreviewable(selected)) { %>
    <div class="contentPreview">
        <div class="widget widget-preview">
            <h1>
                <% wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Edit", "title.preview")); %>
            </h1>

            <%
            String previewFormId = wp.createId();
            String previewTarget = wp.createId();
            String modeId = wp.createId();
            %>

            <div class="widget-preview_controls">
                <form enctype="multipart/form-data" action="<%= wp.url("/content/sharePreview.jsp") %>" method="post" target="_blank">
                    <input name="<%= PageFilter.PREVIEW_ID_PARAMETER %>" type="hidden" value="<%= state.getId() %>">
                    <% if (site != null) { %>
                        <input name="<%= PageFilter.PREVIEW_SITE_ID_PARAMETER %>" type="hidden" value="<%= site.getId() %>">
                    <% } %>
                    <input name="<%= PageFilter.PREVIEW_OBJECT_PARAMETER %>" type="hidden">
                    <input type="hidden" name="scheduleId" value="<%= user.getCurrentSchedule() != null ? user.getCurrentSchedule().getId() : "" %>">
                    <input name="previewDate" type="hidden">
                    <button class="action-share">
                        <% wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.Edit", "action.share")); %>
                    </button>
                </form>

                <%
                wp.writeStart("form",
                        "method", "post",
                        "id", previewFormId,
                        "target", previewTarget,
                        "action", JspUtils.getAbsolutePath(request, "/_preview"));
                    wp.writeElement("input", "type", "hidden", "name", "_fields", "value", true);
                    wp.writeElement("input", "type", "hidden", "name", PageFilter.PREVIEW_ID_PARAMETER, "value", state.getId());
                    wp.writeElement("input", "type", "hidden", "name", PageFilter.PREVIEW_OBJECT_PARAMETER);

                    if (site != null) {
                        wp.writeElement("input", "type", "hidden", "name", PageFilter.PREVIEW_SITE_ID_PARAMETER, "value", site.getId());
                    }

                    wp.writeElement("input",
                            "type", "text",
                            "data-bsp-autosubmit", "",
                            "class", "date",
                            "name", "_date",
                            "placeholder", "Now",
                            "onchange", "$('.widget-preview_controls').find('form').eq(0).find(':input[name=\"previewDate\"]').val($(this).val());");

                    wp.writeHtml(" ");
                    wp.writeStart("select", "class", "deviceWidthSelect", "onchange",
                            "var $input = $(this)," +
                                    "$form = $input.closest('form');" +
                            "$('iframe[name=\"' + $form.attr('target') + '\"]').css('width', $input.val() || '100%');" +
                            "$form.submit();");
                        for (Device d : Device.values()) {
                            wp.writeStart("option", "value", d.width);
                                wp.writeHtml(d);
                            wp.writeEnd();
                        }
                    wp.writeEnd();

                    if (editingType != null) {
                        Renderer.TypeModification rendererData = editingType.as(Renderer.TypeModification.class);
                        List<Object> refs = Query.
                                fromAll().
                                and("_any matches ?", editingState.getId()).
                                and("_id != ?", editingState.getId()).
                                and("_type != ?", Draft.class).
                                select(0, 10).
                                getItems();

                        if (!refs.isEmpty()) {
                            wp.writeHtml(" ");
                            wp.writeStart("select",
                                    "name", "_mainObjectId",
                                    "onchange", "$(this).closest('form').submit();",
                                    "style", "width:200px;");
                                wp.writeStart("option", "value", "");
                                    wp.writeTypeObjectLabel(editing);
                                wp.writeEnd();

                                for (Object ref : refs) {
                                    wp.writeStart("option", "value", State.getInstance(ref).getId());
                                        wp.writeTypeObjectLabel(ref);
                                    wp.writeEnd();
                                }
                            wp.writeEnd();
                        }

                        List<Context> contexts = new ArrayList<Context>();
                        Integer embedPreviewWidth = rendererData.getEmbedPreviewWidth();

                        contexts.add(new Context("", null, "Default"));

                        if (embedPreviewWidth <= 0) {
                            embedPreviewWidth = null;
                        }

                        for (String context : rendererData.getPaths().keySet()) {
                            if (!ObjectUtils.isBlank(context)) {
                                contexts.add(new Context(context, embedPreviewWidth, StringUtils.toLabel(context)));
                            }
                        }

                        if (contexts.size() > 1) {
                            wp.writeHtml(" ");
                            wp.writeStart("select",
                                    "name", "_context",
                                    "onchange",
                                            "var $input = $(this)," +
                                                    "$form = $input.closest('form');" +
                                            "$('iframe[name=\"' + $form.attr('target') + '\"]').css('width', $input.find(':selected').attr('data-width') || '100%');" +
                                            "$form.submit();");
                                for (Context context : contexts) {
                                    wp.writeStart("option",
                                            "value", context.value,
                                            "data-width", context.width);
                                        wp.writeHtml("Context: ");
                                        wp.writeHtml(context.label);
                                    wp.writeEnd();
                                }
                            wp.writeEnd();
                        }
                    }

                    Set<Directory.Path> paths = editingState.as(Directory.Data.class).getPaths();

                    if (paths != null && !paths.isEmpty()) {
                        wp.writeHtml(" ");
                        wp.writeStart("select",
                                "data-bsp-autosubmit", "",
                                "name", "_previewPath");
                            for (Directory.Path p : paths) {
                                Site s = p.getSite();
                                String path = p.getPath();

                                if (s != null) {
                                    wp.writeStart("option", "value", s.getId() + ":" + path);
                                        wp.writeObjectLabel(s);
                                        wp.writeHtml(": ");
                                        wp.writeHtml(path);
                                    wp.writeEnd();

                                } else {
                                    wp.writeStart("option", "value", path);
                                        wp.writeHtml(path);
                                    wp.writeEnd();
                                }
                            }
                        wp.writeEnd();
                    }
                wp.writeEnd();
                %>
            </div>
        </div>
    </div>

    <% if (!wp.getUser().isDisableNavigateAwayAlert() &&
            (wp.getCmsTool().isDisableAutomaticallySavingDrafts() ||
            (!editingState.isNew() &&
            !editingState.as(Content.ObjectModification.class).isDraft()))) { %>
        <script type="text/javascript">
            (function($, window, undefined) {
                $('.contentForm').submit(function() {
                    $.data(this, 'submitting', true);
                });

                $(window).bind('beforeunload', function() {
                    var $form = $('.contentForm');

                    return !$.data($form[0], 'submitting') && $form.find('.state-changed').length > 0 ?
                            'Are you sure you want to leave this page? Unsaved changes will be lost.' :
                            undefined;
                });
            })(jQuery, window);
        </script>
    <% } %>

    <script type="text/javascript">
        var PREVIEW_DATA = {
            <% if (Boolean.TRUE.equals(wp.getUser().getState().get("liveContentPreview"))) { %>
            live: true,
            <% } %>
            formId: '<%= previewFormId %>',
            target: '<%= previewTarget %>',
            stateId: '<%= state.getId() %>',
            objectParameter: '<%= PageFilter.PREVIEW_OBJECT_PARAMETER %>'
        };
        require(['v3/content/preview']);
    </script>
<% } %>
<%
    GalleryDisplay galleryDisplay = wp.getCmsTool().getGalleryDisplay();
    if (galleryDisplay != null && galleryDisplay.equals(GalleryDisplay.VERTICAL)) {
%>
<script type="text/javascript">
    (function($, window) {
        window.showVerticalView = true;
    })(jQuery, window);
</script>
<%  } %>
<% wp.include("/WEB-INF/footer.jsp"); %><%!

// Renders all the content widgets for the given position.
private static void renderWidgets(ToolPageContext wp, Object object, String position) throws Exception {

    State state = State.getInstance(object);
    ObjectType type = state.getType();
    List<Widget> widgets = null;
    for (List<Widget> item : wp.getTool().findWidgets(position)) {
        widgets = item;
        break;
    }

    if (!ObjectUtils.isBlank(widgets)) {
        wp.write("<div class=\"contentWidgets contentWidgets-");
        wp.write(wp.h(position));
        wp.write("\">");

        for (Widget widget : widgets) {

            if (object instanceof ContentEditWidgetDisplay) {
                if (!((ContentEditWidgetDisplay) object).shouldDisplayContentEditWidget(widget.getInternalName())) {
                    continue;
                }

            } else if((type == null || !type.as(ToolUi.class).isPublishable()) && !widget.shouldDisplayInNonPublishable()) {
                continue;
            }

            if (!wp.hasPermission(widget.getPermissionId())) {
                continue;
            }

            wp.write("<input type=\"hidden\" name=\"");
            wp.write(wp.h(state.getId()));
            wp.write("/_widget\" value=\"");
            wp.write(wp.h(widget.getInternalName()));
            wp.write("\">");

            String displayHtml;

            try {
                displayHtml = widget.createDisplayHtml(wp, object);

            } catch (Exception ex) {
                StringWriter sw = new StringWriter();
                HtmlWriter hw = new HtmlWriter(sw);
                hw.putAllStandardDefaults();
                hw.start("pre", "class", "message message-error").object(ex).end();
                displayHtml = sw.toString();
            }

            if (!ObjectUtils.isBlank(displayHtml)) {
                wp.write(displayHtml);
            }
        }
        wp.write("</div>");
    }
}
%><%!

private enum Device {

    DESKTOP("Desktop", 1280),
    TABLET_LANDSCAPE("Tablet - Landscape", 1024),
    TABLET_PORTRAIT("Tablet - Portrait", 768),
    MOBILE_LANDSCAPE("Mobile - Landscape", 480),
    MOBILE_PORTRAIT("Mobile - Portrait", 320);

    public final String label;
    public final int width;

    private Device(String label, int width) {
        this.label = label;
        this.width = width;
    }

    @Override
    public String toString() {
        return label + " (" + width + ")";
    }
}

private static class Context {

    public String value;
    public Integer width;
    public String label;

    public Context(String value, Integer width, String label) {
        this.value = value;
        this.width = width;
        this.label = label;
    }
}
%>
