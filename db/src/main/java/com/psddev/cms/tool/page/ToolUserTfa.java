package com.psddev.cms.tool.page;

import java.io.IOException;
import java.security.SecureRandom;

import javax.servlet.ServletException;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;

@RoutingFilter.Path(application = "cms", value = "/toolUserTfa")
@SuppressWarnings("serial")
public class ToolUserTfa extends PageServlet {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        ToolUser user = page.getUser();
        boolean verifyError = false;

        if (page.isFormPost()
                && page.param(String.class, "action-verify") != null) {
            if (!user.verifyTotp(page.param(int.class, "totpCode"))) {
                verifyError = true;

            } else {
                user.setTfaEnabled(!user.isTfaEnabled());
                user.save();

                page.writeStart("script", "type", "text/javascript");
                    page.writeRaw("window.location = ");
                    String redirectUrl = page.param(String.class, AuthenticationFilter.RETURN_PATH_PARAMETER);
                    if (!StringUtils.isBlank(redirectUrl)) {
                        page.writeRaw('"' + redirectUrl + "\";");
                    } else {
                        page.writeRaw("window.location;");
                    }
                page.writeEnd();
                return;
            }
        }

        if (user.getTotpSecret() == null) {
            byte[] secret = new byte[20];

            RANDOM.nextBytes(secret);
            user.setTotpSecretBytes(secret);
            user.save();
        }

        page.writeHeader();
            page.writeStart("div",
                    "class", "widget",
                    "style", "overflow: hidden;");
                page.writeStart("h1", "class", "icon icon-key");
                    page.writeHtml(user.isTfaEnabled() ? "Disable" : "Enable");
                    page.writeHtml(" Two Factor Authentication");
                page.writeEnd();

                StringBuilder keyUri = new StringBuilder("otpauth://totp/");
                String companyName = page.getCmsTool().getCompanyName();

                if (!ObjectUtils.isBlank(companyName)) {
                    keyUri.append(StringUtils.encodeUri(companyName));
                    keyUri.append(StringUtils.encodeUri(" - "));
                }

                keyUri.append(StringUtils.encodeUri(!StringUtils.isEmpty(user.getUsername()) ? user.getUsername() : user.getEmail()));
                keyUri.append("?secret=");
                keyUri.append(user.getTotpSecret());

                page.writeStart("div", "style", "float: right; margin-left: 10px; margin-top: -20px;");
                    page.writeElement("img",
                            "width", 200,
                            "height", 200,
                            "src", page.cmsUrl("/qrCode", "data", keyUri),
                            "style", "margin-left: 30px;");
                    page.writeStart("div");
                        page.writeHtml("Secret Key: ");
                        page.writeTag("br");
                        page.writeRaw(user.getTotpSecret().replaceAll("(.{4})", "$1 "));
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("div", "style", "margin-right: 280px;");
                    if (verifyError) {
                        page.writeStart("div", "class", "message message-error");
                            page.writeHtml("Code you've entered isn't valid! Try re-entering the code or re-scan the QR code.");
                        page.writeEnd();

                    } else {
                        page.writeStart("div", "class", "message message-info");
                            if (user.isTfaEnabled()) {
                                page.writeHtml("Enter the displayed code from your Google Authenticator to disable two factor authentication.");

                            } else {
                                page.writeHtml("Use Google Authenticator to scan the QR code or type in the secret key, then enter the displayed code to enable two factor authentication.");
                            }
                        page.writeEnd();
                    }
                page.writeEnd();

                page.writeStart("form",
                        "method", "post",
                        "action", page.url(""));
                    page.writeStart("div", "class", "inputContainer");
                        page.writeStart("div", "class", "inputLabel");
                            page.writeStart("label", "for", page.createId());
                                page.writeHtml("Code");
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("div", "class", "inputSmall");
                            page.writeElement("input",
                                    "type", "text",
                                    "id", page.getId(),
                                    "name", "totpCode");
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("div", "class", "actions");
                        page.writeStart("button",
                                "class", "action icon icon-action-save",
                                "name", "action-verify",
                                "value", true);
                            page.writeHtml("Verify");
                        page.writeEnd();
                    page.writeEnd();
                    page.writeElement("input",
                            "type", "hidden",
                            "name", AuthenticationFilter.RETURN_PATH_PARAMETER,
                            "value", page.param(String.class, AuthenticationFilter.RETURN_PATH_PARAMETER));
                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }
}
