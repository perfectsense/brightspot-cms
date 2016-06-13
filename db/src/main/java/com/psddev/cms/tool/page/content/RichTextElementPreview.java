package com.psddev.cms.tool.page.content;

import com.psddev.cms.db.RichTextElement;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

@RoutingFilter.Path(application = "cms", value = "/content/rte-preview")
public class RichTextElementPreview extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        String className = page.param(String.class, "className");

        className = StringUtils.removeStart(className, "rte2-style-");
        className = className.replace('-', '.');

        Class<?> rteClass = ObjectUtils.getClassByName(className);
        RichTextElement rte = (RichTextElement) TypeDefinition.getInstance(rteClass).newInstance();

        rte.fromAttributes((Map<String, String>) ObjectUtils.fromJson(page.param(String.class, "attributes")));

        try {
            rte.writePreviewHtml(page);

        } catch (RuntimeException error) {
            // No preview.
        }
    }
}
