package com.psddev.cms.tool.page;

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
                page.writeHtml("Bulk Edit Status");
                page.writeEnd();

                Date finishDate = submission.getFinishDate();

                if (finishDate == null) {
                    page.writeStart("div", "class", "message message-warning");
                    page.writeHtml("Running... ");
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
                        page.writeHtml("Return to the search result");
                        page.writeEnd();
                        page.writeEnd();
                    }

                    page.writeStart("div", "class", "message message-success");
                    page.writeHtml("Finished running! ");
                    writeSubmission(page, submission);
                    page.writeEnd();
                }
            }
            page.writeEnd();
        }
        page.writeFooter();
    }

    private void writeSubmission(ToolPageContext page, ContentEditBulkSubmission submission) throws IOException {
        page.writeHtml(submission.getSuccesses());
        page.writeHtml(" successes and ");
        page.writeHtml(submission.getFailures());
        page.writeHtml(" failures out of ");
        page.writeHtml(submission.getCount());
        page.writeHtml(" items.");
    }
}
