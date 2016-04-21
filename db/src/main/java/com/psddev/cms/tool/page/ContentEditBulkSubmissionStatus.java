package com.psddev.cms.tool.page;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@RoutingFilter.Path(application = "cms", value = "/contentEditBulkSubmissionStatus")
public class ContentEditBulkSubmissionStatus extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        ContentEditBulkSubmission submission = Query
                .from(ContentEditBulkSubmission.class)
                .where("_id = ?", page.param(UUID.class, "id"))
                .first();

        page.writeHeader();
        {
            page.writeStart("div", "class", "widget");
            {
                page.writeStart("h1");
                    page.writeHtml(page.localize(ContentEditBulkSubmissionStatus.class, "title"));
                page.writeEnd();

                Date finishDate = submission.getFinishDate();

                if (finishDate == null) {
                    page.writeStart("div", "class", "message message-warning");
                    page.writeHtml(page.localize(ContentEditBulkSubmissionStatus.class, "message.running"));
                    writeSubmission(page, submission);
                    page.writeEnd();

                    page.writeStart("script", "type", "text/javascript");
                    {
                        page.writeRaw("setTimeout(function() {");
                        page.writeRaw("window.location.reload();");
                        page.writeRaw("}, 10000);");
                    }
                    page.writeEnd();

                } else {
                    String returnUrl = page.param(String.class, "returnUrl");

                    if (!ObjectUtils.isBlank(returnUrl)) {
                        page.writeStart("p");
                        page.writeStart("a", "class", "icon icon-arrow-left", "href", returnUrl);
                        page.writeHtml(page.localize(ContentEditBulkSubmissionStatus.class, "action.returnToSearch"));
                        page.writeEnd();
                        page.writeEnd();
                    }

                    String status;

                    if (submission.getSuccesses() == 0) {
                        status = "error";

                    } else if (submission.getFailures() > 0) {
                        status = "warning";

                    } else {
                        status = "success";
                    }

                    page.writeStart("div", "class", "message message-" + status);
                    page.writeHtml(page.localize(ContentEditBulkSubmissionStatus.class, "message.finished"));
                    page.writeHtml(' ');
                    writeSubmission(page, submission);
                    page.writeEnd();
                }
            }
            page.writeEnd();
        }
        page.writeFooter();
    }

    private void writeSubmission(ToolPageContext page, ContentEditBulkSubmission submission) throws IOException {
        page.writeHtml(
                page.localize(
                        ContentEditBulkSubmissionStatus.class,
                        ImmutableMap.of(
                                "successCount", submission.getSuccesses(),
                                "failureCount", submission.getFailures(),
                                "totalCount", submission.getCount()),
                        "message.submissionStatus"));
    }
}
