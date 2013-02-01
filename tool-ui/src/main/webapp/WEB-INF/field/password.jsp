<%@ page import="

com.psddev.cms.db.ToolAuthenticationPolicy,
com.psddev.cms.tool.ToolPageContext,
com.psddev.cms.db.ToolUser,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.State,
com.psddev.dari.util.AuthenticationException,
com.psddev.dari.util.AuthenticationPolicy,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.Password,
com.psddev.dari.util.PasswordException,
com.psddev.dari.util.PasswordPolicy,
com.psddev.dari.util.Settings,
com.psddev.dari.util.ValidationException
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));

ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();
Object fieldValue = state.getValue(fieldName);

String inputName = (String) request.getAttribute("inputName");
String isNewName = inputName + "/isNew";
String currentPasswordName = inputName + "/currentPassword";
String password1Name = inputName + "/password1";
String password2Name = inputName + "/password2";
    
    if ((Boolean) request.getAttribute("isFormPost")) {
        if (wp.boolParam(isNewName)) {
            String password = wp.param(password1Name);
            if (ObjectUtils.isBlank(password)) {
                state.addError(field, "Password can't be blank!");
            } else {
                String currentPassword = wp.param(currentPasswordName);                
                ToolUser user = wp.getUser();
                if (ObjectUtils.isBlank(currentPassword)) {
                    state.addError(field, "Must supply your current password!");
                } else {
                    if (!password.equals(wp.param(password2Name))) {
                        state.addError(field, "Passwords don't match!");
                    } else {
                        AuthenticationPolicy authPolicy = AuthenticationPolicy.Static.getInstance(Settings.get(String.class, "cms/tool/authenticationPolicy"));
                        if (authPolicy == null) {
                            authPolicy = new ToolAuthenticationPolicy();
                        }
                        try {
                            authPolicy.authenticate(user.getEmail(), currentPassword);
                            PasswordPolicy policy = PasswordPolicy.Static.getInstance(Settings.get(String.class, "cms/tool/passwordPolicy"));
                            state.putValue(fieldName, Password.validateAndCreateCustom(policy, null, null, password).toString());
                        } catch (PasswordException error) {
                            state.addError(field, error.getMessage());
                        } catch (AuthenticationException authError) {
                            state.addError(field, "Invalid current password!");
                        }
                    }
                }
            }
        }
        return;
    }

// --- Presentation ---

String isNewInputId = wp.getId();
String passwordContainerId = wp.createId();

%><div class="smallInput">
    <select class="toggleable" id="<%= isNewInputId %>" name="<%= isNewName %>">
        <option data-hide="#<%= passwordContainerId %>" value="false"<%= wp.boolParam(isNewName) ? "" : " selected" %>>Keep Same</option>
        <option data-show="#<%= passwordContainerId %>" value="true"<%= wp.boolParam(isNewName) ? " selected" : "" %>>Change</option>
    </select>

    <div id="<%= passwordContainerId %>" style="margin-top: 10px;">
        <div>Your Current Password (for security):</div>
        <input name="<%= currentPasswordName %>" type="password">
        <div>New Password:</div>
        <input name="<%= password1Name %>" type="password">
        <div>Confirm Password:</div>
        <input name="<%= password2Name %>" type="password">
    </div>
</div>

<% if (state.isNew()) { %>
    <script type="text/javascript">
    if (typeof jQuery !== 'undefined') jQuery(function($) {
        $('#<%= isNewInputId %>').val('true').change();
    });
    </script>
<% } %>
