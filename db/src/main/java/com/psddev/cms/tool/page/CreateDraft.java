package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.Content;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.SearchResultSelectionGeneratable;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = CreateDraft.PATH)
public class CreateDraft extends PageServlet {

    public static final String PATH = "/createDraft";

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        String typeIdAndField = page.param(String.class, "typeIdAndField");
        int commaAt = typeIdAndField.indexOf(',');

        ObjectType type = ObjectType.getInstance(ObjectUtils.to(UUID.class, commaAt == -1 ? typeIdAndField : typeIdAndField.substring(0, commaAt)));
        ObjectField field = commaAt == -1 ? null : type.getField(typeIdAndField.substring(commaAt + 1));
        UUID selectionId = page.param(UUID.class, "selectionId");

        Object draftObject = type.createObject(null);

        SearchResultSelection selection = Query.findById(SearchResultSelection.class, selectionId);

        State state = State.getInstance(draftObject);

        if (field != null) {

            List<Object> items = selection.createItemsQuery().
                    referenceOnly().
                    selectAll();

            state.put(field.getInternalName(), items.size() == 1 ? items.get(0) : items);

        } else if (draftObject instanceof SearchResultSelectionGeneratable) {

            // populate the new object using the SelectionGeneratable interface method, "fromSelection"
            ((SearchResultSelectionGeneratable) draftObject).fromSelection(Query.findById(SearchResultSelection.class, selectionId));
        }

        state.as(Content.ObjectModification.class).setDraft(true);
        state.saveUnsafely();

            Query.
                    from(SearchResultSelection.class).
                    where("_id = ?", selectionId).
                    deleteAll();

        page.getResponse().sendRedirect(
                page.toolUrl(CmsTool.class, "/content/edit.jsp",
                        "id", state.getId()));
    }
}
