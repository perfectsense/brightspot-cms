<%@ page session="false" import="

com.psddev.cms.db.Content,
com.psddev.cms.db.Draft,
com.psddev.cms.db.History,
com.psddev.cms.db.Schedule,
com.psddev.cms.db.Template,
com.psddev.cms.db.Trash,
com.psddev.cms.db.Workflow,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Query,
com.psddev.dari.db.State,
com.psddev.dari.util.ObjectUtils,

java.util.Date,
java.util.List,

org.joda.time.DateTime
" %><%

// --- Presentation ---

ToolPageContext wp = new ToolPageContext(pageContext);
Object object = request.getAttribute("object");

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

State state = State.getInstance(object);
Draft draft = wp.getOverlaidDraft(object);
Content.ObjectModification contentData = draft != null
        ? draft.as(Content.ObjectModification.class)
        : state.as(Content.ObjectModification.class);

if (wp.getUser().equals(contentData.getUpdateUser())) {
    Date updateDate = contentData.getUpdateDate();

    if (updateDate != null && updateDate.after(new DateTime().minusSeconds(10).toDate())) {
        String workflowState = state.as(Workflow.Data.class).getCurrentState();

        wp.write("<div class=\"message message-success\"><p>");
            if (!ObjectUtils.isBlank(workflowState)) {
                wp.write("Transitioned to ");
                wp.writeHtml(workflowState);
                wp.writeHtml(" at ");
                wp.writeHtml(wp.formatUserDateTime(updateDate));

            } else {
                if (draft != null || !state.isVisible()) {
                    wp.write("Saved ");

                } else {
                    wp.write("Published ");
                }

                wp.writeHtml(wp.formatUserDateTime(updateDate));
            }
        wp.write(".</p>");
        wp.write("</div>");

        return;
    }
}

Date saved = wp.dateParam("saved");
if (saved != null) {
    wp.write("<div class=\"message message-success\"><p>");
    wp.write("Saved ", saved);
    wp.write(".</p></div>");
    return;
}

if (wp.getOverlaidDraft(object) == null) {
    List<Draft> userDrafts = Query
            .from(Draft.class)
            .and("objectId = ?", state.getId())
            .and("owner = ?", wp.getUser())
            .selectAll();

    if (!userDrafts.isEmpty()) {
        wp.writeStart("div", "class", "message message-info");
            wp.writeStart("p");
                wp.writeHtml("Your drafts:");
            wp.writeEnd();

            wp.writeStart("ul");
                for (Draft userDraft : userDrafts) {
                    wp.writeStart("li");
                        wp.writeStart("a", "href", wp.objectUrl(null, userDraft));
                            wp.writeHtml(wp.formatUserDateTime(userDraft.as(Content.ObjectModification.class).getUpdateDate()));
                        wp.writeEnd();
                    wp.writeEnd();
                }
            wp.writeEnd();
        wp.writeEnd();
    }
}
%>
