package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "contentFormFields")
public class ContentFormFields extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        UUID typeId = page.param(UUID.class, "typeId");
        UUID id = page.param(UUID.class, "id");
        String data = page.param(String.class, "data");
        Object object = ObjectType.getInstance(typeId).createObject(id);

        State.getInstance(object).setResolveInvisible(true);

        if (data != null) {
            State.getInstance(object).putAll((Map<String, Object>) ObjectUtils.fromJson(data));
        }

        List<String> excluded = page.params(String.class, "excluded");

        page.writeSomeFormFields(object, false, null, excluded);
    }
}
