<%@ page session="false" import="

com.psddev.cms.db.ToolUser,
com.psddev.cms.tool.AuthenticationFilter,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.util.HtmlWriter,
com.psddev.dari.util.JspUtils,
com.psddev.dari.util.Password,
com.psddev.dari.util.PasswordException,
com.psddev.dari.util.PasswordPolicy,
com.psddev.dari.util.Settings,
com.psddev.dari.util.StringUtils,
com.psddev.dari.util.UrlBuilder,
com.psddev.dari.util.UserPasswordPolicy,

java.net.MalformedURLException,
java.net.URL
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
ToolUser user = ToolUser.Static.getByChangePasswordToken(wp.param(String.class, "changePasswordToken"));
if (user != null) {
    if (wp.isFormPost()) {
        String currentPassword = wp.param("current_password");
        String newPassword1 = wp.param("password1");
        String newPassword2 = wp.param("password2");

        try {
            if (StringUtils.isBlank(currentPassword)) {
                throw new PasswordException(wp.localize("com.psddev.cms.tool.page.ChangePassword", "error.missingPassword"));
            }
            if (StringUtils.isBlank(newPassword1) || StringUtils.isBlank(newPassword2) || !newPassword1.equals(newPassword2)) {
                throw new PasswordException(wp.localize("com.psddev.cms.tool.page.ChangePassword", "error.passwordMatch"));
            }
            Password current = user.getPassword();
            if (!current.check(currentPassword)) {
                throw new PasswordException(wp.localize("com.psddev.cms.tool.page.ChangePassword", "error.invalidPassword"));
            }

            UserPasswordPolicy userPasswordPolicy = UserPasswordPolicy.Static.getInstance(Settings.get(String.class, "cms/tool/userPasswordPolicy"));
            PasswordPolicy passwordPolicy = null;
            if (userPasswordPolicy == null) {
                passwordPolicy = PasswordPolicy.Static.getInstance(Settings.get(String.class, "cms/tool/passwordPolicy"));
            }
            Password hashedPassword;
            if (userPasswordPolicy != null || (userPasswordPolicy == null && passwordPolicy == null)) {
                hashedPassword = Password.validateAndCreateCustom(userPasswordPolicy, user, current.getAlgorithm(), null, newPassword1);
            } else {
                hashedPassword = Password.validateAndCreateCustom(passwordPolicy, current.getAlgorithm(), null, newPassword1);
            }
            user.updatePassword(hashedPassword);
            user.setChangePasswordOnLogIn(false);
            user.save();

            AuthenticationFilter.Static.logIn(request, response, user);

            String returnPath = wp.param(String.class, AuthenticationFilter.RETURN_PATH_PARAMETER);

            if (!StringUtils.isBlank(returnPath) && returnPath.startsWith("/")) {
                try {
                    response.sendRedirect(returnPath);
                } catch (MalformedURLException e) {
                    wp.redirect("/");
                }
            } else {
                wp.redirect("/");
            }

            return;
        } catch (PasswordException e) {
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
    <h1>Change Password</h1>

    <%
    if (user == null) {
    %>
    <div class="message">
        You've been inactive for too long.<br />
        Please <a href="<%= wp.url("logIn.jsp") %>">log in</a> again.
    </div>
    <%
    } else {
    %>

    <div class="message">
        <%= wp.h(wp.localize("com.psddev.cms.tool.page.ChangePassword", "message.expired"))%>
    </div>

    <%
    if (error != null) {
        new HtmlWriter(wp.getWriter()).object(error);
    }
    %>

    <form action="<%= wp.url("") %>" method="post">
        <div class="inputContainer">
            <div class="inputLabel">
                <label for="<%= wp.createId() %>">
                    <%= wp.h(wp.localize("com.psddev.cms.tool.page.ChangePassword", "label.password"))%>
                </label>
            </div>
            <div class="inputSmall">
                <input class="autoFocus" id="<%= wp.getId() %>" name="current_password" type="password">
            </div>
        </div>

        <div class="inputContainer">
            <div class="inputLabel">
                <label for="<%= wp.createId() %>">
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.ChangePassword", "label.newPassword"))%>
                </label>
            </div>
            <div class="inputSmall">
                <input id="<%= wp.getId() %>" name="password1" type="password">
            </div>
        </div>

        <div class="inputContainer">
            <div class="inputLabel">
                <label for="<%= wp.createId() %>">
                    <%= wp.h(wp.localize("com.psddev.cms.tool.page.ChangePassword", "label.confirmPassword"))%>
                </label>
            </div>
            <div class="inputSmall">
                <input id="<%= wp.getId() %>" name="password2" type="password">
            </div>
        </div>

        <div class="buttons">
            <button class="action">
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.ChangePassword", "label.confirmPassword"))%>
            </button>
        </div>
    </form>

    <%
    }
    %>

</div>

<% wp.include("/WEB-INF/footer.jsp"); %>
