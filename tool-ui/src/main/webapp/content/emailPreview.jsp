<%@ page session="false" import="

com.psddev.cms.db.PageFilter,
com.psddev.cms.db.Preview,
com.psddev.cms.db.Site,
com.psddev.cms.db.ToolUser,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ObjectType,
com.psddev.dari.util.JspUtils,
com.psddev.dari.util.MailMessage,
com.psddev.dari.util.MailProvider,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.Settings,
com.psddev.dari.util.StringUtils,

java.util.Date,
java.util.Map,
java.util.UUID,
java.util.regex.Matcher,
java.util.regex.Pattern

" %><%
    ToolPageContext wp = new ToolPageContext(pageContext);
    if (wp.requireUser()) {
        return;
    }

    ToolUser user = wp.getUser();
    Site site = wp.getSite();
    boolean submitted = false;
    boolean failedChecks = false;
    String message = "";
    String messageStatus = "";
    String objectString = wp.param(PageFilter.PREVIEW_OBJECT_PARAMETER);

    if (wp.isFormPost()) {
        try {
            String toEmail = request.getParameter("shareEmail");

            if (ObjectUtils.isBlank(objectString)) {
                failedChecks = true;
                message = "The preview link was unable to be rendered.";
                throw new Exception();
            }

            if (StringUtils.isBlank(toEmail)) {
                failedChecks = true;
                message = "Please enter an email.";
                throw new Exception();
            } else {
                String email_pattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
                Pattern pattern = Pattern.compile(email_pattern);
                Matcher matcher = pattern.matcher(toEmail);

                if (!matcher.matches()) {
                    failedChecks = true;
                    message = "Email is invalid.";
                    throw new Exception();
                }
            }

            if (user.getEmail() != null) {
                failedChecks = true;
                message = "User does not have an email set up!";
                throw new Exception();
            }

            Map<String, Object> objectMap = (Map<String, Object>) ObjectUtils.fromJson(objectString.trim());
            Preview preview = new Preview();
            preview.setCreateDate(new Date());
            preview.setObjectType(ObjectType.getInstance(ObjectUtils.to(UUID.class, objectMap.remove("_typeId"))));
            preview.setObjectId(ObjectUtils.to(UUID.class, objectMap.remove("_id")));
            preview.setObjectValues(objectMap);
            preview.setSite(wp.getSite());
            wp.publish(preview);

            String url = wp.getCmsTool().getPreviewUrl() + "?" + PageFilter.PREVIEW_ID_PARAMETER + "=" + preview.getId();
            if (!request.getParameter("previewDate").isEmpty()) {
                url += "&_date=" + request.getParameter("previewDate");
            }
            String baseUrl = Settings.get(String.class, ToolPageContext.TOOL_URL_PREFIX_SETTING);
            if (StringUtils.isBlank(baseUrl)) {
                baseUrl = JspUtils.getAbsoluteUrl(request, "/");
            }
            if (!baseUrl.startsWith("https") && JspUtils.isSecure(request)) {
                baseUrl = baseUrl.replace("http", "https");
            }
            String finalUrl = baseUrl.substring(0, baseUrl.length()-1) + url;
            //System.out.println(finalUrl);

            String subject = "Content Preview";
            if (site != null) {
                subject = subject + " for " + site.getName();
            }

            String comments = StringUtils.escapeHtml(request.getParameter("shareComments"));

            StringBuilder body = new StringBuilder();
            body.append("<p>User <b>");
            body.append(user.getName());
            body.append("</b> has sent you a link to preview content.</p>");
            body.append("<p>User says: </p>");
            body.append("<p><b>");
            body.append(comments);
            body.append("</p></b>");
            body.append("<br />");
            body.append("<a href =\"");
            body.append(finalUrl);
            body.append("\" target=\"_blank\">Click Here to view content preview</a><br />");
            body.append("<br />");
            body.append("<p>Thank you,<br />");
            body.append("Brightspot Support</p>");

            System.out.println(body.toString());

            MailProvider.Static.getDefault().send(new MailMessage(toEmail).
                    from(user.getEmail()).
                    subject(subject).
                    bodyHtml(body.toString()));

            messageStatus = "message-success";
            message = "Email has been sent";
        } catch (Exception ex) {
            wp.getErrors().add(ex);
            messageStatus = "message-error";
            if (!failedChecks) {
                message = "Email was unable to be sent. Please try again later";
            }
        } finally {
            submitted = true;
        }
    }

%>
<h1>Send Email of Preview Link</h1>
<% if (submitted) { %>
    <div class="message <%= messageStatus %>">
        <h2><%= message %></h2>
    </div>
<% } %>

<div id="sendSharePreview">
    <form enctype="multipart/form-data" action="<%= wp.url("/content/emailPreview.jsp") %>" method="post">
        <input name="<%= PageFilter.PREVIEW_ID_PARAMETER %>" type="hidden" value="<%= request.getParameter("id") %>">
        <% if (site != null) { %>
        <input name="<%= PageFilter.PREVIEW_SITE_ID_PARAMETER %>" type="hidden" value="<%= site.getId() %>">
        <% } %>
        <input name="<%= PageFilter.PREVIEW_OBJECT_PARAMETER %>" type="hidden">
        <p><label for="shareEmail">Email To: </label><input id="shareEmail" name="shareEmail"></p>
        <p><label for="shareComments">Comments: </label><textarea id="shareComments" name="shareComments"></textarea></p>
        <input type="hidden" name="scheduleId" value="<%= user.getCurrentSchedule() != null ? user.getCurrentSchedule().getId() : "" %>">
        <input name="previewDate" type="hidden">
        <button class="action-share">Send Email</button>
    </form>
</div>

<script>
    (function($, win) {
        CONTEXT_PATH = window.CONTEXT_PATH || '';
        var $contentForm = $('.contentForm');
        var action = win.location.href;
        var questionAt = action.indexOf('?');
        var newFormData = $contentForm.serialize();

        $.ajax({
            'data': newFormData,
            'type': 'post',
            'url': CONTEXT_PATH + 'content/state.jsp?id=<%= request.getParameter("id") %>&' + (questionAt > -1 ? action.substring(questionAt + 1) : ''),
            'complete': function(request) {
                $(':input[name=<%= PageFilter.PREVIEW_OBJECT_PARAMETER %>]').val(request.responseText);
            }
        });
    })(jQuery, window);
</script>
