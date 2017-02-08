package com.psddev.cms.tool.page;

import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Application;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.SmsProvider;
import com.psddev.dari.util.StringUtils;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Servlet to handle test sms requests to the phone number provided.
 */
@RoutingFilter.Path(application = "cms", value = "/testSms")
public class TestSms extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        if (page.isAjaxRequest()) {
            String sms = "This is a test message.";
            String url = Application.Static.getInstance(CmsTool.class).getDefaultToolUrl();

            if (!StringUtils.isBlank(url)) {
                sms +=  "\nCMS Url: " + url;
            }

            try {
                SmsProvider.Static.getDefault().send(null, page.param(String.class, "number"), sms);
                page.writeStart("div", "class", "Sms-success").writeHtml(page.localize(TestSms.class, "message.success")).writeEnd();

            } catch (IllegalStateException error) {
                page.writeStart("div", "class", "Sms-error").writeHtml(page.localize(TestSms.class, "message.error")).writeEnd();
            }

        } else {
            throw new IllegalArgumentException("Request must be ajax!");
        }
    }
}
