<%@ page session="false" import="

com.psddev.cms.db.Content,
com.psddev.cms.db.ContentLock,
com.psddev.cms.db.ContentSection,
com.psddev.cms.db.Directory,
com.psddev.cms.db.Draft,
com.psddev.cms.db.DraftStatus,
com.psddev.cms.db.Guide,
com.psddev.cms.db.GuidePage,
com.psddev.cms.db.History,
com.psddev.cms.db.Page,
com.psddev.cms.db.PageFilter,
com.psddev.cms.db.Renderer,
com.psddev.cms.db.Schedule,
com.psddev.cms.db.Section,
com.psddev.cms.db.Site,
com.psddev.cms.db.Template,
com.psddev.cms.db.ToolSearch,
com.psddev.cms.db.ToolUi,
com.psddev.cms.db.ToolUser,
com.psddev.cms.db.Variation,
com.psddev.cms.db.Workflow,
com.psddev.cms.db.WorkflowLog,
com.psddev.cms.db.WorkflowState,
com.psddev.cms.db.WorkflowTransition,
com.psddev.cms.db.WorkStream,
com.psddev.cms.tool.CmsTool,
com.psddev.cms.tool.ToolPageContext,
com.psddev.cms.tool.Widget,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Query,
com.psddev.dari.db.Singleton,
com.psddev.dari.db.State,
com.psddev.dari.util.DateUtils,
com.psddev.dari.util.HtmlWriter,
com.psddev.dari.util.JspUtils,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.PaginatedResult,
com.psddev.dari.util.StringUtils,

java.io.StringWriter,
java.util.ArrayList,
java.util.Date,
java.util.LinkedHashMap,
java.util.LinkedHashSet,
java.util.List,
java.util.ListIterator,
java.util.Map,
java.util.Set,
java.util.UUID,

org.joda.time.DateTime
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requireUser()) {
    return;
}

Object selected = wp.findOrReserve();
if (selected == null) {
    wp.redirect("/", "reason", "no-object");
    return;
}

State state = State.getInstance(selected);
Site site = wp.getSite();

if (selected != null) {
    if (!(site == null || Site.Static.isObjectAccessible(site, selected))) {
        wp.redirect("/", "reason", "not-accessible");
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
    }

    State.getInstance(workstreamObject).as(WorkStream.Data.class).complete(workStream, wp.getUser());
}

if (wp.tryDelete(editing) ||
        wp.tryDraft(editing) ||
        wp.tryPublish(editing) ||
        wp.tryRestore(editing) ||
        wp.tryTrash(editing) ||
        wp.tryWorkflow(editing)) {
    return;
}

Object copy = Query.findById(Object.class, wp.uuidParam("copyId"));
if (copy != null) {
    State editingState = State.getInstance(editing);
    editingState.setValues(State.getInstance(copy).getSimpleValues());
    editingState.setId(null);
}

// Directory directory = Query.findById(Directory.class, wp.uuidParam("directoryId"));
History history = wp.getOverlaidHistory(editing);
Draft draft = wp.getOverlaidDraft(editing);
Set<ObjectType> compatibleTypes = ToolUi.getCompatibleTypes(State.getInstance(editing).getType());
State editingState = State.getInstance(editing);
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

%><%

wp.writeHeader(editingState.getType() != null ? editingState.getType().getLabel() : null);

    String search = wp.param(String.class, "search");

    if (search != null) {
        wp.writeStart("div", "class", "frame");
            wp.writeStart("a", "href", wp.cmsUrl("/searchCarousel", "id", editingState.getId(), "search", search));
            wp.writeEnd();
        wp.writeEnd();
    }
%>
<div class="content-edit">
    <form class="contentForm contentLock"
            method="post"
            enctype="multipart/form-data"
            action="<%= wp.objectUrl("", selected,
                    "action-delete", null,
                    "action-draft", null,
                    "action-publish", null,
                    "action-restore", null,
                    "action-save", null,
                    "action-trash", null,
                    "published", null) %>"
            autocomplete="off"
            data-new="<%= State.getInstance(editing).isNew() %>"
            data-o-id="<%= State.getInstance(selected).getId() %>"
            data-o-label="<%= wp.h(State.getInstance(selected).getLabel()) %>"
            data-o-preview="<%= wp.h(wp.getPreviewThumbnailUrl(selected)) %>"
            data-content-locked-out="<%= lockedOut && !editAnyway %>"
            data-content-id="<%= State.getInstance(editing).getId() %>">
        <div class="contentForm-main">
            <div class="widget widget-content">
                <h1 class="breadcrumbs"><%

                    wp.writeStart("span", "class", "breadcrumbItem icon icon-object");
                        wp.writeHtml(state.isNew() ? "New " : "Edit ");

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

                        if (!state.isNew()) {
                            wp.write(": " );
                            wp.write(wp.objectLabel(editing));
                        }
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
                    <a class="icon icon-action-edit widgetControlsEditInFull" target="_blank" href="<%= wp.url("") %>">Edit In Full</a>
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
                        !(editing instanceof com.psddev.dari.db.Singleton)) { %>
                    <div class="widget-contentCreate">
                        <div class="action action-create">New</div>
                        <ul>
                            <li><a class="action action-create" href="<%= wp.url("/content/edit.jsp",
                                    "typeId", State.getInstance(editing).getTypeId(),
                                    "templateId", template != null ? template.getId() : null)
                                    %>">New <%= wp.typeLabel(editing) %></a></li>
                            <li><a class="action action-copy" href="<%= wp.url("/content/edit.jsp",
                                    "typeId", State.getInstance(editing).getTypeId(),
                                    "templateId", template != null ? template.getId() : null,
                                    "copyId", State.getInstance(editing).getId())
                                    %>" target="_top">Copy This <%= wp.typeLabel(editing) %></a></li>

                        </ul>
                    </div>
                <% } %>

                <% if (sectionContent != null) { %>
                    <p><a class="icon icon-arrow-left" href="<%= wp.url("", "contentId", null) %>">Back to Layout</a></p>
                <% } %>

                <%
                wp.include("/WEB-INF/objectMessage.jsp", "object", editing);

                if (!editingState.as(Content.ObjectModification.class).isDraft() &&
                        (history != null || draft != null)) {
                    State original = State.getInstance(Query.
                            from(Object.class).
                            where("_id = ?", editing).
                            noCache().
                            first());

                    if (original != null) {
                        wp.writeStart("div", "class", "contentDiff");
                            if (history != null) {
                                wp.writeStart("div", "class", "contentDiffOld contentDiffLeft");
                                    wp.writeStart("h2").writeHtml("History").writeEnd();
                                    wp.writeSomeFormFields(editing, true, null, null);
                                wp.writeEnd();
                            }

                            try {
                                wp.disableFormFields();

                                wp.writeStart("div", "class", "contentDiffCurrent " + (history != null ? "contentDiffRight" : "contentDiffLeft"));
                                    wp.writeStart("h2").writeHtml("Current").writeEnd();
                                    wp.writeSomeFormFields(original.getOriginalObject(), true, null, null);
                                wp.writeEnd();

                            } finally {
                                wp.enableFormFields();
                            }

                            if (draft != null) {
                                wp.writeStart("div", "class", "contentDiffNew contentDiffRight");
                                    wp.writeStart("h2").writeHtml("Draft").writeEnd();
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
            <div class="widget widget-publishing">
                <h1 class="icon icon-action-publish">Publishing</h1>

                <%
                wp.writeStart("div", "class", "widget-controls");
                    if (!wp.getCmsTool().isDisableContentLocking() && !lockedOut && wp.getCmsTool().isOptInContentLocking()) {
                        wp.writeStart("a",
                                "class", "icon icon-only icon-" + (optInLock ? "lock" : "unlock"),
                                "href", wp.url("", "lock", !optInLock));
                            wp.writeHtml("Lock");
                        wp.writeEnd();
                    }

                    wp.writeStart("a",
                            "class", "widget-publishing-tools",
                            "href", wp.objectUrl("/contentTools", editing, "returnUrl", wp.url("")),
                            "target", "contentTools");
                        wp.writeHtml("Tools");
                    wp.writeEnd();
                wp.writeEnd();

                if (workStream != null) {
                    long skipped = workStream.countSkipped(user);
                    long complete = workStream.countComplete();
                    long incomplete = workStream.countIncomplete() - skipped;
                    long total = complete + incomplete + skipped;

                    wp.writeStart("div",
                            "class", "block",
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
                                "href", wp.objectUrl("/content/workStreamEdit.jsp", workStream),
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

                boolean isWritable = wp.hasPermission("type/" + editingState.getTypeId() + "/write");
                Content.ObjectModification contentData = State.getInstance(editing).as(Content.ObjectModification.class);
                boolean isDraft = !editingState.isNew() && (contentData.isDraft() || draft != null);
                boolean isHistory = history != null;
                boolean isTrash = contentData.isTrash();
                Schedule schedule = draft != null ? draft.getSchedule() : null;

                if (isWritable) {

                    // Message and actions if the content is a draft.
                    if (isDraft) {
                        Content.ObjectModification draftContentData = State.
                                getInstance(draft != null ? draft : editing).
                                as(Content.ObjectModification.class);

                        wp.writeStart("div", "class", "message message-warning");
                            wp.writeStart("p");
                                wp.writeHtml("Draft last saved ");
                                wp.writeHtml(wp.formatUserDateTime(draftContentData.getUpdateDate()));
                                wp.writeHtml(" by ");
                                wp.writeObjectLabel(draftContentData.getUpdateUser());
                                wp.writeHtml(".");

                                if (schedule != null) {
                                    Date triggerDate = schedule.getTriggerDate();
                                    ToolUser triggerUser = schedule.getTriggerUser();

                                    if (triggerDate != null || triggerUser != null) {
                                        wp.writeHtml(" Scheduled to be published");

                                        if (triggerDate != null) {
                                            wp.writeHtml(" ");
                                            wp.writeHtml(wp.formatUserDateTime(triggerDate));
                                        }

                                        if (triggerUser != null) {
                                            wp.writeHtml(" by ");
                                            wp.writeObjectLabel(triggerUser);
                                        }

                                        wp.writeHtml(".");
                                    }
                                }
                            wp.writeEnd();

                            wp.writeStart("div", "class", "actions");
                                if (!contentData.isDraft()) {
                                    wp.writeStart("a",
                                            "class", "icon icon-action-edit",
                                            "href", wp.url("", "draftId", null));
                                        wp.writeHtml("Current");
                                    wp.writeEnd();
                                }

                                wp.writeStart("button",
                                        "class", "link icon icon-action-save",
                                        "name", "action-draft",
                                        "value", "true");
                                    wp.writeHtml("Save");
                                wp.writeEnd();

                                wp.writeStart("button",
                                        "class", "link icon icon-action-delete",
                                        "name", "action-delete",
                                        "value", "true");
                                    wp.writeHtml("Delete");
                                wp.writeEnd();
                            wp.writeEnd();
                        wp.writeEnd();

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
                                    wp.writeHtml("Current");
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
                        wp.writeTrashMessage(editing);
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
                                        wp.writeHtml("Ignore Lock");
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
                                (editingState.isNew() ||
                                !editingState.isVisible() ||
                                draft != null ||
                                editingState.as(Workflow.Data.class).getCurrentState() != null)) {
                            Workflow workflow = Query.from(Workflow.class).where("contentTypes = ?", editingState.getType()).first();

                            if (workflow != null) {
                                Workflow.Data workflowData = editingState.as(Workflow.Data.class);
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
                                            where("objectId = ?", editingState.getId()).
                                            sortDescending("date").
                                            first();

                                    wp.writeStart("div", "class", "widget-publishingWorkflow");
                                        if (!ObjectUtils.isBlank(currentState)) {
                                            String workflowStateDisplayName = currentState;

                                            for (WorkflowState s : workflow.getStates()) {
                                                if (ObjectUtils.equals(s.getName(), currentState)) {
                                                    workflowStateDisplayName = s.getDisplayName();
                                                    break;
                                                }
                                            }

                                            wp.writeStart("div", "class", "widget-publishingWorkflowComment");
                                                wp.writeStart("span", "class", "visibilityLabel widget-publishingWorkflowState");
                                                    wp.writeHtml(workflowStateDisplayName);
                                                wp.writeEnd();

                                                if (log != null) {
                                                    String comment = log.getComment();

                                                    wp.writeHtml(" ");
                                                    wp.writeStart("a",
                                                            "target", "workflowLogs",
                                                            "href", wp.cmsUrl("/workflowLogs", "objectId", editingState.getId()));
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
                                            wp.writeEnd();
                                        }

                                        if (!transitionNames.isEmpty()) {
                                            WorkflowLog newLog = new WorkflowLog();

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

                                            for (Map.Entry<String, String> entry : transitionNames.entrySet()) {
                                                wp.writeStart("button",
                                                        "name", "action-workflow",
                                                        "value", entry.getKey());
                                                    wp.writeHtml(entry.getValue());
                                                wp.writeEnd();
                                            }
                                        }
                                    wp.writeEnd();
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
                                if (wp.getUser().getCurrentSchedule() == null) {
                                    if (!contentData.isDraft() && schedule != null) {
                                        boolean newSchedule = wp.param(boolean.class, "newSchedule");

                                        wp.writeStart("div", "style", wp.cssString("margin-bottom", "5px"));
                                            wp.writeStart("select", "name", "newSchedule");
                                                wp.writeStart("option",
                                                        "selected", newSchedule ? null : "selected",
                                                        "value", "");
                                                    wp.writeHtml("Update Existing Schedule");
                                                wp.writeEnd();

                                                wp.writeStart("option",
                                                        "selected", newSchedule ? "selected" : null,
                                                        "value", "true");
                                                    wp.writeHtml("Create New Schedule");
                                                wp.writeEnd();
                                            wp.writeEnd();
                                        wp.writeEnd();
                                    }

                                    DateTime publishDate;

                                    if (schedule != null) {
                                        publishDate = wp.toUserDateTime(schedule.getTriggerDate());

                                    } else {
                                        publishDate = wp.param(DateTime.class, "publishDate");

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
                                            "name", "publishDate",
                                            "size", 9,
                                            "value", publishDate != null ? publishDate.toString("yyyy-MM-dd HH:mm:ss") : "");
                                }

                                wp.writeStart("button",
                                        "name", "action-publish",
                                        "value", "true");
                                    wp.writeHtml("Publish");
                                wp.writeEnd();

                                if (!isDraft &&
                                        !isHistory &&
                                        !editingState.isNew() &&
                                        (editingState.getType() == null ||
                                        !editingState.getType().getGroups().contains(Singleton.class.getName()))) {
                                    wp.writeStart("button",
                                            "class", "link icon icon-action-trash",
                                            "name", "action-trash",
                                            "value", "true");
                                        wp.writeHtml("Archive");
                                    wp.writeEnd();
                                }
                            wp.writeEnd();
                        }
                    }
                }

                wp.writeStart("ul", "class", "widget-publishingExtra");
                    if (isWritable && !isDraft && !isTrash) {
                        wp.writeStart("li");
                            wp.writeStart("button",
                                    "class", "link icon icon-object-draft",
                                    "name", "action-draft",
                                    "value", "true");
                                wp.writeHtml(editingState.isVisible() ? "Save Draft" : "Save");
                            wp.writeEnd();
                        wp.writeEnd();
                    }
                wp.writeEnd();
                %>
            </div>

            <% renderWidgets(wp, editing, CmsTool.CONTENT_RIGHT_WIDGET_POSITION); %>
        </div>
    </form>
</div>

<% if (wp.isPreviewable(selected)) { %>
    <div class="contentPreview">
        <div class="widget widget-preview">
            <h1>Preview</h1>

            <%
            String previewFormId = wp.createId();
            String previewTarget = wp.createId();
            String modeId = wp.createId();
            %>

            <ul class="widget-preview_controls">
                <li>
                    <form enctype="multipart/form-data" action="<%= wp.url("/content/sharePreview.jsp") %>" method="post" target="_blank">
                        <input name="<%= PageFilter.PREVIEW_ID_PARAMETER %>" type="hidden" value="<%= state.getId() %>">
                        <% if (site != null) { %>
                            <input name="<%= PageFilter.PREVIEW_SITE_ID_PARAMETER %>" type="hidden" value="<%= site.getId() %>">
                        <% } %>
                        <input name="<%= PageFilter.PREVIEW_OBJECT_PARAMETER %>" type="hidden">
                        <input type="hidden" name="scheduleId" value="<%= user.getCurrentSchedule() != null ? user.getCurrentSchedule().getId() : "" %>">
                        <input name="previewDate" type="hidden">
                        <button class="action-share">Share</button>
                    </form>
                </li>

                <li>
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

                        ObjectType editingType = editingState.getType();

                        if (editingType != null) {
                            Renderer.TypeModification rendererData = editingType.as(Renderer.TypeModification.class);

                            if (!ObjectUtils.isBlank(rendererData.getEmbedPath())) {
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
                </li>
            </ul>
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
        (function($, win, undef) {
            var PEEK_WIDTH = 160,
                    $win = $(win),
                    doc = win.document,
                    $doc = $(doc),
                    $body = $(doc.body),

                    $edit = $('.content-edit'),
                    oldEditStyle = $edit.attr('style') || '',
                    $publishingExtra = $('.widget-publishingExtra'),
                    $previewAction,
                    appendPreviewAction,
                    removePreviewAction,

                    $preview = $('.contentPreview'),
                    $previewWidget = $preview.find('.widget-preview'),
                    $previewHeading = $preview.find('h1'),
                    showPreview,
                    previewEventsBound,
                    hidePreview,

                    getUniqueColor,
                    fieldHue = Math.random(),
                    GOLDEN_RATIO = 0.618033988749895;

            if ($edit.closest('.popup').length > 0) {
                return;
            }

            // Append a link for activating the preview.
            appendPreviewAction = function() {
                $previewAction = $('<li/>', {
                    'html': $('<a/>', {
                        'class': 'action-preview',
                        'href': '#',
                        'text': 'Preview',
                        'click': function() {
                            removePreviewAction();
                            showPreview();
                            $previewHeading.click();
                            return false;
                        }
                    })
                });

                $publishingExtra.append($previewAction);
            };

            removePreviewAction = function() {
                $previewAction.remove();
                $previewAction = null;
            };

            // Show a peekable preview widget.
            showPreview = function() {
                var $previewForm = $('#<%= previewFormId %>'),
                        $contentForm = $('.contentForm'),
                        action = win.location.href,
                        questionAt = action.indexOf('?'),
                        oldFormData,
                        loadPreview;

                $previewWidget.addClass('widget-loading');
                $preview.show();

                $edit.css({
                    'margin-right': $preview.outerWidth(true),
                    'max-width': 1100
                });

                if (!previewEventsBound) {
                    $preview.append($('<span/>', {
                        'class': 'contentPreviewClose',
                        'text': 'Close',
                        'click': function() {
                            hidePreview();
                            return false;
                        }
                    }));

                    // Preview should be roughly the same width as the window.
                    $win.resize($.throttle(500, function() {
                        var css = $edit.offset(),
                                winWidth = $win.width();

                        css.left += $previewWidget.is('.widget-expanded') ? PEEK_WIDTH : $edit.outerWidth() + 10;
                        css['min-width'] = winWidth - css.left;

                        $preview.css(css);
                        $previewWidget.css('width', winWidth - PEEK_WIDTH);
                    }));

                    // Make the preview expand/collapse when the heading is clicked.
                    $previewHeading.click(function() {
                        var editLeft = $edit.offset().left;

                        // $edit.find('.inputContainer').trigger('fieldPreview-disable');

                        if ($previewWidget.is('.widget-expanded')) {
                            $('.queryField_frames').show();
                            $previewWidget.removeClass('widget-expanded');
                            $preview.animate({ 'left': editLeft + $edit.outerWidth() + 10 }, 300, 'easeOutBack');
                            $preview.css('width', '');

                            $.ajax({
                                'type': 'post',
                                'url': CONTEXT_PATH + '/misc/updateUserSettings',
                                'data': 'action=liveContentPreview-enable'
                            });

                        } else {
                            $('.queryField_frames').hide();
                            $previewWidget.addClass('widget-expanded');
                            $preview.animate({ 'left': editLeft + PEEK_WIDTH }, 300, 'easeOutBack');
                            $preview.css('width', $win.width() - PEEK_WIDTH - 30);

                            // $edit.find('.inputContainer').trigger('fieldPreview-enable');
                        }
                    });

                    previewEventsBound = true;
                }

                $win.resize();

                // Load the preview.
                loadPreview = $.throttle(2000, function() {
                    if (!$preview.is(':visible')) {
                        return;
                    }

                    var newFormData = $contentForm.serialize();

                    // If the form inputs haven't changed, try again later.
                    if (oldFormData === newFormData) {
                        setTimeout(loadPreview, 100);
                        return;
                    }

                    oldFormData = newFormData;
                    $previewWidget.addClass('widget-loading');

                    // Get the correct JSON from the server.
                    $.ajax({
                        'data': newFormData,
                        'type': 'post',
                        'url': CONTEXT_PATH + 'content/state.jsp?id=<%= state.getId() %>&' + (questionAt > -1 ? action.substring(questionAt + 1) : ''),
                        'complete': function(request) {
                            var $previewTarget,
                                    setHeightTimer,
                                    setHeight;

                            // Make sure that the preview IFRAME exists.
                            $(':input[name=<%= PageFilter.PREVIEW_OBJECT_PARAMETER %>]').val(request.responseText);
                            $previewTarget = $('iframe[name=<%= previewTarget %>]');

                            if ($previewTarget.length === 0) {
                                $previewTarget = $('<iframe/>', {
                                    'name': '<%= previewTarget %>',
                                    'css': {
                                        'border-style': 'none',
                                        'height': '1000px',
                                        'margin': 0,
                                        'overflow': 'hidden',
                                        'padding': 0,
                                        'width': $previewForm.find('select.deviceWidthSelect').val() || '100%'
                                    }
                                });
                                $previewWidget.append($previewTarget);
                            }

                            // Resize IFRAME so that there isn't a scrollbar.
                            setHeight = function() {
                                var $body;

                                if ($previewTarget[0]) {
                                    $body = $($previewTarget[0].contentWindow.document.body);
                                    $body.css('overflow', 'hidden');
                                    $previewTarget.height(Math.max($edit.outerHeight(true), $body.outerHeight(true)));

                                } else if (setHeightTimer) {
                                    clearInterval(setHeightTimer);
                                    setHeightTimer = null;
                                }
                            };

                            setHeightTimer = setInterval(setHeight, 100);

                            $previewTarget.load(function() {
                                $previewWidget.removeClass('widget-loading');
                                setHeight();
                                if (setHeightTimer) {
                                    clearInterval(setHeightTimer);
                                    setHeightTimer = null;
                                }
                            });

                            // Really load the preview.
                            $previewForm.submit();
                            setTimeout(loadPreview, 100);
                        }
                    });
                });

                loadPreview();
            };

            hidePreview = function() {
                if ($previewWidget.is('.widget-expanded')) {
                    $previewHeading.click();
                }

                // $edit.find('.inputContainer').trigger('fieldPreview-hide');
                $edit.attr('style', oldEditStyle);
                appendPreviewAction();
                $preview.hide();
                $win.resize();

                $.ajax({
                    'type': 'post',
                    'url': CONTEXT_PATH + '/misc/updateUserSettings',
                    'data': 'action=liveContentPreview-disable'
                });
            };

            <% if (Boolean.TRUE.equals(wp.getUser().getState().get("liveContentPreview"))) { %>
                showPreview();
            <% } else { %>
                appendPreviewAction();
            <% } %>

            // Per-field preview.
            getUniqueColor = function($container) {
                var color = $.data($container[0], 'fieldPreview-color');

                if (!color) {
                    fieldHue += GOLDEN_RATIO;
                    fieldHue %= 1.0;
                    color = 'hsl(' + (fieldHue * 360) + ', 50%, 50%)';
                    $.data($container[0], 'fieldPreview-color', color);
                }

                return color;
            };

            $edit.delegate('.inputContainer', 'mouseenter', function() {
                var $container = $(this),
                        $toggle = $.data($container[0], 'fieldPreview-$toggle');

                if ($preview.is(':visible')) {
                    if (!$toggle) {
                        $toggle = $('<span/>', {
                            'class': 'fieldPreviewToggle'
                        });

                        $.data($container[0], 'fieldPreview-$toggle', $toggle);
                        $container.append($toggle);
                    }

                } else if ($toggle) {
                    $toggle.remove();
                }
            });

            $edit.delegate('.inputContainer .fieldPreviewToggle', 'click', function() {
                var $toggle = $(this),
                        $container = $toggle.closest('.inputContainer');

                $container.find('> .inputLabel').trigger('fieldPreview-toggle', [ $toggle ]);
                $toggle.css('color', $container.is('.fieldPreview-displaying') ? getUniqueColor($container) : '');
            });

            $edit.delegate('.inputContainer', 'fieldPreview-enable', function() {
                $(this).addClass('fieldPreview-enabled');
            });

            $edit.delegate('.inputContainer', 'fieldPreview-disable', function() {
                $(this).trigger('fieldPreview-hide').removeClass('fieldPreview-enabled');
            });

            $edit.delegate('.inputContainer', 'fieldPreview-hide', function() {
                var $container = $(this),
                        name = $container.attr('data-name');

                $container.removeClass('fieldPreview-displaying');
                $container.find('> .inputLabel').css({
                    'background-color': '',
                    'color': ''
                });

                $('.fieldPreviewTarget[data-name="' + name + '"]').remove();
                $('.fieldPreviewPaths[data-name="' + name + '"]').remove();
            });

            $edit.delegate('.inputContainer', 'fieldPreview-toggle', function(event, $source) {
                var $container = $(this),
                        name = $container.attr('data-name'),
                        color,

                        $frame,
                        frameOffset,

                        $paths,
                        pathsCanvas;

                event.stopPropagation();

                if ($container.is('.fieldPreview-displaying')) {
                    $container.trigger('fieldPreview-hide');
                    return;
                }

                color = getUniqueColor($container);

                $frame = $preview.find('iframe');
                frameOffset = $frame.offset();

                $container.addClass('fieldPreview-displaying');
                $container.find('> .inputLabel').css({
                    'background-color': color,
                    'color': 'white'
                });

                // Draw arrows between the label and the previews.
                $paths = $('<canvas/>', {
                    'class': 'fieldPreviewPaths',
                    'data-name': name,
                    'css': {
                        'left': 0,
                        'pointer-events': 'none',
                        'position': 'absolute',
                        'top': 0,
                        'z-index': 5
                    }
                });

                // For browsers that don't support pointer-events.
                $paths.click(function() {
                    $edit.find('.inputContainer').trigger('fieldPreview-hide');
                });

                $paths.attr({
                    'width': $doc.width(),
                    'height': $doc.height()
                });

                $body.append($paths);

                pathsCanvas = $paths[0].getContext('2d');

                var PLACEHOLDER_PREFIX = 'brightspot.field-access '
                var frameDocument = $frame[0].contentDocument;
                var frameCommentWalker = frameDocument.createTreeWalker(frameDocument.body, NodeFilter.SHOW_COMMENT, null, null);

                while (frameCommentWalker.nextNode()) {
                    var placeholder = frameCommentWalker.currentNode;
                    var placeholderValue = placeholder.nodeValue;

                    if (placeholderValue.indexOf(PLACEHOLDER_PREFIX) !== 0) {
                        continue;
                    }

                    var placeholderData = $.parseJSON(placeholderValue.substring(PLACEHOLDER_PREFIX.length));

                    if (placeholderData.name !== name) {
                        continue;
                    }

                    var $placeholder = $(frameCommentWalker.currentNode),
                            $target,
                            targetOffset,
                            pathSourceX, pathSourceY, pathSourceDirection,
                            pathTargetX, pathTargetY, pathTargetDirection,
                            sourceOffset,
                            targetOffset,
                            isBackReference = false,
                            pathSourceControlX,
                            pathSourceControlY,
                            pathTargetControlX,
                            pathTargetControlY;

                    if ($placeholder.parent().is('body')) {
                        continue;
                    }

                    $target = $placeholder.nextAll(':visible:first');

                    if ($target.length === 0) {
                        $target = $placeholder.parent();
                    }

                    if ($target.find('> * [data-name="' + name + '"]').length > 0) {
                        continue;
                    }

                    targetOffset = $target.offset();

                    $body.append($('<span/>', {
                        'class': 'fieldPreviewTarget',
                        'data-name': name,
                        'css': {
                            'outline-color': color,
                            'height': $target.outerHeight(),
                            'left': frameOffset.left + targetOffset.left,
                            'position': 'absolute',
                            'top': frameOffset.top + targetOffset.top,
                            'width': $target.outerWidth()
                        }
                    }));

                    if (!$source) {
                        $source = $container.find('> .inputLabel');
                    }

                    sourceOffset = $source.offset();
                    targetOffset = $target.offset();
                    targetOffset.left += frameOffset.left;
                    targetOffset.top += frameOffset.top;

                    if (sourceOffset.left > targetOffset.left) {
                        var targetWidth = $target.outerWidth();
                        pathTargetX = targetOffset.left + targetWidth + 3;
                        pathTargetY = targetOffset.top + $target.outerHeight() / 2;
                        isBackReference = true;

                        if (targetOffset.left + targetWidth > sourceOffset.left) {
                            pathSourceX = sourceOffset.left + $source.width();
                            pathSourceY = sourceOffset.top + $source.height() / 2;
                            pathSourceDirection = 1;
                            pathTargetDirection = 1;

                        } else {
                            pathSourceX = sourceOffset.left;
                            pathSourceY = sourceOffset.top + $source.height() / 2;
                            pathSourceDirection = -1;
                            pathTargetDirection = 1;
                        }

                    } else {
                        pathSourceX = sourceOffset.left + $source.width();
                        pathSourceY = sourceOffset.top + $source.height() / 2;
                        pathTargetX = targetOffset.left - 3;
                        pathTargetY = targetOffset.top + $target.height() / 2;
                        pathSourceDirection = 1;
                        pathTargetDirection = -1;
                    }

                    pathSourceControlX = pathSourceX + pathSourceDirection * 100;
                    pathSourceControlY = pathSourceY;
                    pathTargetControlX = pathTargetX + pathTargetDirection * 100;
                    pathTargetControlY = pathTargetY;

                    pathsCanvas.strokeStyle = color;
                    pathsCanvas.fillStyle = color;

                    // Reference curve.
                    pathsCanvas.lineWidth = isBackReference ? 0.4 : 1.0;
                    pathsCanvas.beginPath();
                    pathsCanvas.moveTo(pathSourceX, pathSourceY);
                    pathsCanvas.bezierCurveTo(pathSourceControlX, pathSourceControlY, pathTargetControlX, pathTargetControlY, pathTargetX, pathTargetY);
                    pathsCanvas.stroke();

                    // Arrow head.
                    var arrowSize = pathTargetX > pathTargetControlX ? 5 : -5;
                    if (isBackReference) {
                        arrowSize *= 0.8;
                    }
                    pathsCanvas.beginPath();
                    pathsCanvas.moveTo(pathTargetX, pathTargetY);
                    pathsCanvas.lineTo(pathTargetX - 2 * arrowSize, pathTargetY - arrowSize);
                    pathsCanvas.lineTo(pathTargetX - 2 * arrowSize, pathTargetY + arrowSize);
                    pathsCanvas.closePath();
                    pathsCanvas.fill();
                }
            });

            $edit.delegate('.inputContainer', 'click', function() {
                if ($previewWidget.is('.widget-expanded')) {
                    $(this).trigger('fieldPreview-toggle');
                    return false;

                } else {
                    return true;
                }
            });
        })(jQuery, window);
    </script>
<% } %>

<% wp.include("/WEB-INF/footer.jsp"); %><%!

// Renders all the content widgets for the given position.
private static void renderWidgets(ToolPageContext wp, Object object, String position) throws Exception {

    State state = State.getInstance(object);
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
            if (wp.hasPermission(widget.getPermissionId())) {

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
