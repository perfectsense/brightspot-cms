<%@ page session="false" import="

com.psddev.cms.db.Content,
com.psddev.cms.db.Draft,
com.psddev.cms.db.History,
com.psddev.cms.db.Site,
com.psddev.cms.db.Schedule,
com.psddev.cms.db.Template,
com.psddev.cms.db.Trash,
com.psddev.cms.db.Workflow,
com.psddev.cms.db.WorkflowLog,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Database,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Query,
com.psddev.dari.db.State,
com.psddev.dari.util.ObjectUtils,

java.io.IOException,
java.util.Date,
java.util.Iterator,
java.util.List,

org.joda.time.DateTime
, com.google.common.collect.ImmutableMap" %><%

// --- Presentation ---

ToolPageContext wp = new ToolPageContext(pageContext);
Object object = request.getAttribute("object");
State state = State.getInstance(object);

if (wp.getOverlaidDraft(object) == null) {
    List<Object> contentUpdates = Query
            .fromAll()
            .and("com.psddev.cms.db.Draft/objectId = ?", state.getId())
            .and("cms.content.updateDate > ?", new DateTime().minusDays(1).getMillis())
            .sortDescending("cms.content.updateDate")
            .selectAll();

    for (Iterator<Object> i = contentUpdates.iterator(); i.hasNext();) {
        Draft contentUpdate = (Draft) i.next();

        if (contentUpdate.isNewContent()) {
            i.remove();
        }
    }

    if (!contentUpdates.isEmpty()) {
        wp.writeStart("div", "class", "message message-info");
        wp.writeStart("p");
        wp.writeHtml(wp.localize(
                "com.psddev.cms.tool.page.content.ObjectMessage",
                "message.recentDraftsHeading"));
        wp.writeEnd();

        wp.writeStart("ul");
        for (Object contentUpdateObject : contentUpdates) {
            if (contentUpdateObject instanceof Draft) {
                Draft contentUpdate = (Draft) contentUpdateObject;
                String contentUpdateName = contentUpdate.getName();

                wp.writeStart("li");
                wp.writeStart("a", "href", wp.objectUrl(null, contentUpdate));

                if (!ObjectUtils.isBlank(contentUpdateName)) {
                    wp.writeHtml(contentUpdateName);
                    wp.writeHtml(" last saved ");
                }

                wp.writeHtml(wp.formatUserDateTime(contentUpdate.as(Content.ObjectModification.class).getUpdateDate()));
                wp.writeHtml(" by ");
                wp.writeObjectLabel(contentUpdate.getUpdateUser());
                wp.writeEnd();
                wp.writeEnd();
            }
        }
        wp.writeEnd();
        wp.writeEnd();
    }
}

List<Throwable> errors = wp.getErrors();
if (errors != null && errors.size() > 0) {
    wp.include("/WEB-INF/errors.jsp");
    return;
}

Trash deleted = Query.findById(Trash.class, wp.uuidParam("deleted"));
if (deleted != null) {
    wp.write("<div class=\"message message-warning\"><p>");
    wp.write("Deleted ", deleted.getDeleteDate());
    wp.write(" by ", wp.objectLabel(deleted.getDeleteUser()));
    wp.write(".<p></div>");
    return;
}

Draft draft = wp.getOverlaidDraft(object);
Content.ObjectModification contentData = draft != null && !draft.isNewContent()
        ? draft.as(Content.ObjectModification.class)
        : state.as(Content.ObjectModification.class);

if (!state.isNew() && !contentData.isTrash() && wp.getUser().equals(contentData.getUpdateUser())) {
    Date tenSecondsAgo = new DateTime(Database.Static.getDefault().now()).minusSeconds(10).toDate();
    Date updateDate = contentData.getUpdateDate();

    if (updateDate != null && updateDate.after(tenSecondsAgo)) {
        WorkflowLog log = Query.from(WorkflowLog.class)
                .and("objectId = ?", draft != null ? draft.getId() : state.getId())
                .and("date > ?", tenSecondsAgo.getTime())
                .sortDescending("date")
                .first();

        wp.write("<div class=\"message message-success\"><p>");
            if (log != null && !ObjectUtils.isBlank(log.getNewWorkflowState())) {
                Workflow workflow = Workflow.findWorkflow(state.as(Site.ObjectModification.class).getOwner(), state);
                String workflowStateDisplayName = workflow == null ? log.getNewWorkflowState() : workflow.getStateDisplayName(log.getNewWorkflowState());
                wp.writeHtml(wp.localize(
                        "com.psddev.cms.tool.page.content.ObjectMessage",
                        ImmutableMap.of(
                                "state", workflowStateDisplayName,
                                "date", wp.formatUserDateTime(log.getDate())),
                        "message.transition"));
                writeReturnToDashboard(wp);

            } else {

                wp.writeHtml(wp.localize(
                        "com.psddev.cms.tool.page.content.ObjectMessage",
                        ImmutableMap.of("date", wp.formatUserDateTime(updateDate)),
                        draft != null || !state.isVisible() ? "message.saved" : "message.published"));
                writeReturnToDashboard(wp);
            }
        wp.write("</p>");
        wp.write("</div>");

        return;
    }
}

Date saved = wp.dateParam("saved");
if (saved != null) {
    wp.write("<div class=\"message message-success\"><p>");
    wp.writeHtml(wp.localize(
            "com.psddev.cms.tool.page.content.ObjectMessage",
            ImmutableMap.of("date", saved),
            "message.saved"));
    writeReturnToDashboard(wp);
    wp.write("</p></div>");
    return;
}
%><%!

private static void writeReturnToDashboard(ToolPageContext wp) throws IOException {
    wp.writeHtml(' ');
    wp.writeStart("a", "class", "Message-returnToDashboard", "href", wp.cmsUrl("/"));
    wp.writeHtml(wp.localize("com.psddev.cms.tool.page.content.ObjectMessage", "action.returnToDashboard"));
    wp.writeEnd();
}
%>
