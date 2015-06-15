package com.psddev.cms.tool.page;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import com.psddev.dari.util.AmazonStorageItem;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;

@RoutingFilter.Path(application = "cms", value = "amazonAuth")
class AmazonSigningServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String storageSetting = !StringUtils.isBlank(request.getParameter("storageSetting")) ? request.getParameter("storageSetting") : StorageItem.DEFAULT_STORAGE_SETTING;

        String secret = Settings.get(String.class, StorageItem.SETTING_PREFIX + "/" + storageSetting + "/" + AmazonStorageItem.SECRET_SETTING);

        if (StringUtils.isBlank(secret)) {
            throw new ServletException(StorageItem.SETTING_PREFIX + storageSetting + "/secret not found in your context.xml");
        }

        byte[] rawHmac = StringUtils.hmacSha1(secret, request.getParameter("to_sign"));
        String result = new String(Base64.encodeBase64(rawHmac));

        response.setContentType("text/html");
        response.getWriter().write(result);
    }
}
