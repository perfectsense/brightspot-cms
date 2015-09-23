<%@ page session="false" import="

com.psddev.cms.db.ToolAuthenticationPolicy,
com.psddev.cms.db.ToolUser,
com.psddev.cms.tool.CmsTool,
com.psddev.cms.tool.AuthenticationFilter,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Query,
com.psddev.dari.util.AuthenticationException,
com.psddev.dari.util.AuthenticationPolicy,
com.psddev.dari.util.HtmlWriter,
com.psddev.dari.util.JspUtils,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.Settings,
com.psddev.dari.util.StringUtils,
com.psddev.dari.util.UrlBuilder,

org.slf4j.Logger,
org.slf4j.LoggerFactory,

java.net.MalformedURLException,
java.net.URL,
java.util.UUID
" %><%!
private Logger LOGGER = LoggerFactory.getLogger("logIn.jsp");

private void logAuthRequest(String context, String userId, String domain, String ipAddress, boolean status, boolean enabled) {
    if (enabled) {
        if (status) {
            LOGGER.info(context + " [userId:" + userId + ", status:success, domain:" + domain + ", ipAddress:"
                    + ipAddress + "]");
        } else {
            LOGGER.info(context + " [userId:" + userId + ", status:fail, domain:" + domain + ", ipAddress:"
                    + ipAddress + "]");
        }
    }
}

private static String getDomain(String siteUrl) {
    String domain = siteUrl;
    if (!ObjectUtils.isBlank(siteUrl)) {
        domain = siteUrl.replaceFirst("^(?i)(?:https?://)?(?:www\\.)?", "");
        int slashAt = domain.indexOf('/');

        if (slashAt > -1) {
            domain = domain.substring(0, slashAt);
        }

        int colonAt = domain.indexOf(':');

        if (colonAt > -1) {
            domain = domain.substring(0, colonAt);
        }
    }
    return domain;
}

private static String getIpAddress(String xForReqParam, String remoteAddrReqParam) {
    String ipAddress = xForReqParam;
    if (ipAddress == null) {
        ipAddress = remoteAddrReqParam;
    }
    return ipAddress;
}
%>
<%

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

AuthenticationException authError = null;
String username = wp.param("username");
String returnPath = wp.param(AuthenticationFilter.RETURN_PATH_PARAMETER);
ToolUser user = ToolUser.Static.getByTotpToken(wp.param(String.class, "totpToken"));

if (wp.isFormPost()) {
    
    String siteUrl = Query.from(CmsTool.class).first().getDefaultSiteUrl();
    String domain = getDomain(siteUrl);
    String ipAddress = getIpAddress(request.getHeader("X-FORWARDED-FOR"), request.getRemoteAddr());
    boolean isAuthLogged = Settings.get(boolean.class, "cms/tool/isAuthenticationLogged");

    try {

        if (user != null) {
            if (!user.verifyTotp(wp.param(int.class, "totpCode"))) {
                throw new AuthenticationException("The code you've entered is either invalid or has already been used.");
            }

        } else {
            AuthenticationPolicy authPolicy = AuthenticationPolicy.Static.getInstance(Settings.get(String.class, "cms/tool/authenticationPolicy"));

            if (authPolicy == null) {
                authPolicy = new ToolAuthenticationPolicy();
            }

            user = (ToolUser) authPolicy.authenticate(username, wp.param(String.class, "password"));

            logAuthRequest("ToolAuthentication", username, domain, ipAddress, true, isAuthLogged);

            if (user.isTfaEnabled()) {
                String totpToken = UUID.randomUUID().toString();

                user.setTotpToken(totpToken);
                user.save();
                wp.redirect("", "totpToken", totpToken);
                return;
            }
        }

        if (user.isChangePasswordOnLogIn()) {
            String changePasswordToken = UUID.randomUUID().toString();
            user.setChangePasswordToken(changePasswordToken);
            user.save();
            wp.redirect("change-password.jsp", "changePasswordToken", changePasswordToken, AuthenticationFilter.RETURN_PATH_PARAMETER, returnPath);
            return;
        }

        if (user.getChangePasswordToken() != null) {
            user.setChangePasswordToken(null);
            user.save();
        }

        AuthenticationFilter.Static.logIn(request, response, user);

        if (!StringUtils.isBlank(returnPath)) {
            try {
                wp.redirect(new URL(JspUtils.getAbsoluteUrl(request, returnPath)).toString());
            } catch (MalformedURLException e) {
                wp.redirect("/");
            }
        } else {
            wp.redirect("/");
        }

        return;

    } catch (AuthenticationException error) {
        logAuthRequest("ToolAuthentication", username, domain, ipAddress, false, isAuthLogged);
        authError = error;
    }
}

// --- Presentation ---

%><% wp.include("/WEB-INF/header.jsp"); %>

<style type="text/css">
.toolHeader {
    background-color: transparent;
    border-style: none;
    box-shadow: none;
}
.toolTitle {
    float: none;
    height: 100px;
    margin: 30px 0 0 0;
    text-align: center;
    width: auto;
}
.toolTitle a img {
    max-width: 390px;
}
.toolFooter {
    border-style: none;
    text-align: center;
}
.toolFooter .build {
    background-position: top center;
    text-align: center;
}
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
.inputContainer {
    background: transparent !important;
}
.buttons {
    border-top-style: none;
}
</style>

<div class="widget widget-logIn">
    <h1>
        <%= wp.h(wp.localize("com.psddev.cms.tool.page.LogIn", "title")) %>
    </h1>

    <%
    if (wp.param(boolean.class, "forced")) {
        wp.writeStart("div", "class", "message message-warning");
            wp.writeHtml(wp.localize("com.psddev.cms.tool.page.LogIn", "message.inactive"));
        wp.writeEnd();
    }

    if (authError != null) {
        new HtmlWriter(wp.getWriter()).object(authError);
    }
    %>

    <% if (!Query.from(ToolUser.class).hasMoreThan(0)) { %>
        <div class="message message-info">
            <p><%= wp.h(wp.localize("com.psddev.cms.tool.page.LogIn", "message.welcome")) %></p>
        </div>
    <% } %>

    <form action="<%= wp.url("", "forced", null) %>" method="post">
        <% if (user == null) { %>
            <div class="inputContainer">
                <div class="inputLabel">
                    <label for="<%= wp.createId() %>"><%= wp.h(wp.localize("com.psddev.cms.tool.page.LogIn", "label.username")) %></label>
                </div>
                <div class="inputSmall">
                    <input class="autoFocus" id="<%= wp.getId() %>" name="username" type="text" value="<%= wp.h(username) %>" placeholder="<%= wp.h(wp.localize("com.psddev.cms.tool.page.LogIn", "placeholder.username")) %>">
                </div>
            </div>

            <div class="inputContainer">
                <div class="inputLabel">
                    <label for="<%= wp.createId() %>">
                        <%= wp.h(wp.localize("com.psddev.cms.tool.page.LogIn", "label.password")) %>
                    </label>
                </div>
                <div class="inputSmall">
                    <input id="<%= wp.getId() %>" name="password" type="password" placeholder="<%= wp.h(wp.localize("com.psddev.cms.tool.page.LogIn", "placeholder.password")) %>">
                </div>
            </div>

        <% } else { %>
            <div class="inputContainer">
                <div class="inputLabel">
                    <label for="<%= wp.createId() %>">
                        <%= wp.h(wp.localize("com.psddev.cms.tool.page.LogIn", "label.code")) %>
                    </label>
                </div>
                <div class="inputSmall">
                    <input class="autoFocus" id="<%= wp.getId() %>" name="totpCode" type="text" placeholder="<%= wp.h(wp.localize("com.psddev.cms.tool.page.LogIn", "placeholder.code")) %>">
                </div>
            </div>
        <% } %>

        <div class="buttons">
            <button class="action action-logIn"><%= wp.h(wp.localize("com.psddev.cms.tool.page.LogIn", "action.login")) %></button>
            <% if (!StringUtils.isBlank(Settings.get(String.class, "cms/tool/forgotPasswordEmailSender")) && user == null) {%>
            <a href="<%= wp.url("forgot-password.jsp", AuthenticationFilter.RETURN_PATH_PARAMETER, returnPath) %>">
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.LogIn", "action.forgotPassword")) %>
            </a>
            <% } %>
        </div>
    </form>
</div>

<% wp.include("/WEB-INF/footer.jsp"); %>
