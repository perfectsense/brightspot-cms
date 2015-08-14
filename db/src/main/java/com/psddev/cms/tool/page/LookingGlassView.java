package com.psddev.cms.tool.page;

import java.io.IOException;

import com.psddev.cms.db.Preview;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.ToolUserAction;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

public abstract class LookingGlassView {

    protected void doRenderAction(ToolPageContext page, Object content, String mirrorUrl) throws IOException {
    }

    public void renderAction(ToolPageContext page, ToolUser user, ToolUserAction action) throws IOException {
        Object content = Query
                .from(Object.class)
                .where("_id = ?", action.getContentId())
                .first();

        Preview preview = Query
                .from(Preview.class)
                .where("_id = ?", user.getCurrentPreviewId())
                .first();

        String mirrorUrl = StringUtils.addQueryParameters(preview != null && ObjectUtils.equals(action.getContentId(), preview.getObjectId())
                ? JspUtils.getAbsolutePath(page.getRequest(), "/_preview", "_cms.db.previewId", preview.getId())
                : action.getUrl(), "_mirror", true);

        doRenderAction(page, content, mirrorUrl);
    }

    private abstract static class QueryParametersView extends LookingGlassView {

        protected abstract String changeMirrorUrl(String mirrorUrl);

        @Override
        protected void doRenderAction(ToolPageContext page, Object content, String mirrorUrl) throws IOException {
            page.writeStart("div", "style", page.cssString("margin", "0 -20px"));
                page.writeStart("iframe",
                        "src", changeMirrorUrl(mirrorUrl),
                        "style", page.cssString(
                                "border-style", "none",
                                "height", "10000px",
                                "width", "100%"));
                page.writeEnd();
            page.writeEnd();
        }
    }

    public static class PreviewView extends QueryParametersView {

        @Override
        protected String changeMirrorUrl(String mirrorUrl) {
            return mirrorUrl;
        }
    }

    public static class DebugView extends QueryParametersView {

        @Override
        protected String changeMirrorUrl(String mirrorUrl) {
            return StringUtils.addQueryParameters(mirrorUrl, "_debug", true);
        }
    }

    public static class GridView extends QueryParametersView {

        @Override
        protected String changeMirrorUrl(String mirrorUrl) {
            return StringUtils.addQueryParameters(mirrorUrl, "_grid", true);
        }
    }

    public static class HtmlApiView extends QueryParametersView {

        @Override
        protected String changeMirrorUrl(String mirrorUrl) {
            return StringUtils.addQueryParameters(mirrorUrl, "_format", "json");
        }
    }

    public static class EditView extends LookingGlassView {

        @Override
        protected void doRenderAction(ToolPageContext page, Object content, String mirrorUrl) throws IOException {
            page.writeStart("div", "style", page.cssString(
                    "margin", "0 -20px",
                    "overflow", "hidden"));
                page.writeStart("iframe",
                        "src", page.cmsUrl("/content/edit.jsp",
                                "_mirror", true,
                                "id", State.getInstance(content).getId()),
                        "style", page.cssString(
                                "border-style", "none",
                                "height", "10000px",
                                "margin-top", "-70px",
                                "width", "100%"));
                page.writeEnd();
            page.writeEnd();
        }
    }
}
