package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.util.AmazonStorageItem;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.WebPageContext;

@RoutingFilter.Path(application = "cms", value = "amazonUploader")
class AmazonUploadServlet extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {

        if (page.requireUser()) {
            return;
        }

        String action = page.param(String.class, "action");

        if (StringUtils.isBlank(action)) {
            throw new ServletException("Missing [action] parameter");
        }

        if (action.equals("sign")) {
            signRequest(page);
        } else if (action.equals("createPath")) {
            createPath(page);
        }
    }

    private void signRequest(WebPageContext page) throws ServletException, IOException {
        String storageSetting = !StringUtils.isBlank(page.param(String.class, "storageSetting"))
                ? page.param(String.class, "storageSetting")
                : StorageItem.DEFAULT_STORAGE_SETTING;

        String secret = Settings.get(String.class, StorageItem.SETTING_PREFIX + "/" + storageSetting + "/" + AmazonStorageItem.SECRET_SETTING);

        if (StringUtils.isBlank(secret)) {
            throw new ServletException(StorageItem.SETTING_PREFIX + storageSetting + "/secret not found in your context.xml");
        }

        byte[] rawHmac = StringUtils.hmacSha1(secret, page.param(String.class, "to_sign"));
        String result = new String(Base64.encodeBase64(rawHmac));

        HttpServletResponse response = page.getResponse();
        response.setContentType("text/html");
        response.getWriter().write(result);
    }

    private void createPath(WebPageContext page) throws ServletException, IOException {
        UUID typeId = page.param(UUID.class, "typeId");
        String fieldName = page.param(String.class, "fieldName");
        String fileName = page.param(String.class, "fileName");

        if (ObjectUtils.isBlank(typeId) || StringUtils.isBlank(fieldName) || StringUtils.isBlank(fileName)) {
            throw new ServletException("Missing parameters:"
                    + (ObjectUtils.isBlank(typeId) ? " [typeId]" : "")
                    + (StringUtils.isBlank(fieldName) ? " [fieldName]" : "")
                    + (StringUtils.isBlank(fileName) ? " [fileName]" : ""));
        }

        ObjectType type = ObjectType.getInstance(typeId);

        if (type == null) {
            throw new ServletException("No ObjectType exists with [id]: " + typeId);
        }

        ObjectField field = type.getField(fieldName);

        if (field == null) {
            throw new ServletException("No ObjectField exists for type [" + typeId + "] with name  [" + fieldName + "]");
        }

        HttpServletResponse response = page.getResponse();
        response.setContentType("application/json");
        page.write(ObjectUtils.toJson(
                ImmutableMap.of(
                        "filePath", field.as(ToolUi.class).getStoragePath(null, fileName))));
    }
}
