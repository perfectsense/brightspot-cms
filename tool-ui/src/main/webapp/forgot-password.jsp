<%@ page session="false" import="

com.psddev.cms.db.ToolUser,
com.psddev.cms.tool.AuthenticationFilter,
com.psddev.cms.tool.CmsTool,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Application,
com.psddev.dari.db.Query,
com.psddev.dari.util.HtmlWriter,
com.psddev.dari.util.JspUtils,
com.psddev.dari.util.MailMessage,
com.psddev.dari.util.MailProvider,
com.psddev.dari.util.PasswordException,
com.psddev.dari.util.RoutingFilter,
com.psddev.dari.util.Settings,
com.psddev.dari.util.StringUtils,
com.psddev.dari.util.UrlBuilder,

java.util.UUID
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);

if (wp.getUser() != null) {
    AuthenticationFilter.Static.logOut(response);
    response.sendRedirect(new UrlBuilder(request).
            currentPath().
            currentParameters().
            toString());
    return;
}

PasswordException error = null;
String username = wp.param("username");
boolean submitted = false;

if (wp.isFormPost()) {
    try {
        String emailSender = Settings.get(String.class, "cms/tool/forgotPasswordEmailSender");
        if (StringUtils.isBlank(emailSender)) {
            throw new PasswordException(wp.localize("com.psddev.cms.tool.page.ForgotPassword", "error.noEmailSender"));
        }
        if (StringUtils.isBlank(username)) {
            throw new PasswordException(wp.localize("com.psddev.cms.tool.page.ForgotPassword", "error.noUser"));
        }
        ToolUser user = Query.from(ToolUser.class).where("email = ? or username = ?", username, username).first();
        if (user == null) {
            throw new PasswordException(wp.localize("com.psddev.cms.tool.page.ForgotPassword", "error.noUser"));
        }
        String email = user.getEmail();
        if (StringUtils.isBlank(email) || email.indexOf("@") < 1) {
            throw new PasswordException(wp.localize("com.psddev.cms.tool.page.ForgotPassword", "error.noEmail"));
        }
        if (!user.isAllowedToRequestForgotPassword(Settings.getOrDefault(Long.class, "cms/tool/forgotPasswordIntervalInMinutes", 5L))) {
            throw new PasswordException(wp.localize("com.psddev.cms.tool.page.ForgotPassword", "error.emailSent"));
        }
        String baseUrl = Settings.get(String.class, ToolPageContext.TOOL_URL_PREFIX_SETTING);
        if (StringUtils.isBlank(baseUrl)) {
            baseUrl = JspUtils.getAbsoluteUrl(request, "/");
        }
        if (!baseUrl.startsWith("https") && JspUtils.isSecure(request)) {
            baseUrl = baseUrl.replace("http", "https");
        }

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(baseUrl);
        urlBuilder.append(wp.cmsUrl("/reset-password.jsp"));
        String changePasswordToken = StringUtils.hex(StringUtils.hash("SHA-256", UUID.randomUUID().toString()));

        String url = urlBuilder.toString();
        url = StringUtils.addQueryParameters(url, "rpr", changePasswordToken);

        long expiration = Settings.getOrDefault(long.class, "cms/tool/changePasswordTokenExpirationInHours", 24L);

        StringBuilder body = new StringBuilder();
        body.append("<p>Dear ");
        body.append(user.getName());
        body.append(",</p>");
        body.append("<p>You recently requested to reset your password at ");
        body.append(baseUrl);
        body.append(".</p>");
        body.append("<p>Please click on the link below (expires in ");
        body.append(expiration);
        body.append(" hour");
        if (expiration > 1) {
            body.append("s");
        }
        body.append(") to reset your password:</p>");
        body.append("<a href=\"");
        body.append(url);
        body.append("\" target=\"_blank\">Password Reset</a><br />");
        body.append("<br />");
        body.append("<p>Thank you,<br />");
        body.append("Brightspot Support</p>");

        MailProvider.Static.getDefault().send(new MailMessage(email).
                from(emailSender).
                subject("Password Reset").
                bodyHtml(body.toString()));
        user.setChangePasswordToken(changePasswordToken);
        user.save();
        submitted = true;
    } catch (PasswordException e) {
        if (Settings.get(boolean.class, "cms/tool/suppressErrorMessage")) {
            submitted = true;
        } else {
            error = e;
        }
    }
}

// --- Presentation ---
wp.writeHeader(null, false);
%>

<style type="text/css">
.widget {
    margin: 0 auto;
    width: 30em;
}
body {
    margin-top: 170px;
}
body.hasToolBroadcast {
    margin-top: 195px;
}
</style>

<div class="widget">
    <h1>Forgot Password</h1>

    <% if (submitted) { %>
        <div class="message message-info">
            <p>
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.ForgotPassword", "message.reset"))%>
            </p>
        </div>
    <% } else { %>

    <%
    if (error != null) {
        new HtmlWriter(wp.getWriter()).object(error);
    }
    %>

    <form action="<%= wp.url("") %>" method="post">
        <div class="inputContainer">
            <div class="inputLabel">
                <label for="<%= wp.createId() %>">
                    <%= wp.h(wp.localize("com.psddev.cms.tool.page.ForgotPassword", "label.username"))%>
                </label>
            </div>
            <div class="inputSmall">
                <input class="autoFocus" id="<%= wp.getId() %>" name="username" type="text" value="<%= wp.h(username) %>">
            </div>
        </div>

        <div class="buttons">
            <button class="action">
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.ForgotPassword", "action.submit"))%>
            </button>
            <a href="<%= wp.url("logIn.jsp", AuthenticationFilter.RETURN_PATH_PARAMETER, wp.param(AuthenticationFilter.RETURN_PATH_PARAMETER)) %>">
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.ForgotPassword", "action.back"))%>
            </a>
        </div>
    </form>

    <%
    }
    %>

</div>

<% wp.include("/WEB-INF/footer.jsp"); %>
