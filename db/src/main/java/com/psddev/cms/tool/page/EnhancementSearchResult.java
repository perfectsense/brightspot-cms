package com.psddev.cms.tool.page;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.cms.db.RichTextReference;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultRenderer;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Reference;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "enhancementSearchResult")
public class EnhancementSearchResult extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Search search = new Search(page);
        SearchResultRenderer resultRenderer = new SearchResultRenderer(page, search) {

            @Override
            public void renderBeforeItem(Object item) throws IOException {
                Reference enhancement = null;

                State state = State.getInstance(item);

                ObjectType refType = null;
                Class<? extends Reference> referenceClass = state.getType().as(ToolUi.class).getReferenceableViaClass();
                if (referenceClass != null) {
                    refType = Database.Static.getDefault().getEnvironment().getTypeByClass(referenceClass);
                }

                if (refType != null) {
                    Object refObject = refType.createObject(null);
                    if (refObject instanceof Reference) {
                        enhancement = (Reference) refObject;
                    }
                }

                if (enhancement == null) {
                    enhancement = new Reference();
                }

                RichTextReference rt = enhancement.as(RichTextReference.class);

                enhancement.setObject(item);
                rt.setLabel(state.getLabel());
                rt.setPreview(page.getPreviewThumbnailUrl(item));

                page.writeStart("a",
                        "data-enhancement", ObjectUtils.toJson(enhancement.getState().getSimpleValues()),
                        "href", "#");
            }

            @Override
            public void renderAfterItem(Object item) throws IOException {
                page.writeEnd();
            }
        };

        resultRenderer.render();
    }
}
