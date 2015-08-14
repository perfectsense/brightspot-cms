package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.History;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUser;
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

            List<Object> items = selection.createItemsQuery()
                    .referenceOnly()
                    .selectAll();

            state.put(field.getInternalName(), items.size() == 1 ? items.get(0) : items);

        } else if (draftObject instanceof SearchResultSelectionGeneratable) {

            // populate the new object using the SelectionGeneratable interface method, "fromSelection"
            ((SearchResultSelectionGeneratable) draftObject).fromSelection(Query.findById(SearchResultSelection.class, selectionId));
        }

        state.as(Content.ObjectModification.class).setDraft(true);

        publishUnsafely(state, page.getUser().getCurrentSite(), page.getUser());

        page.getUser().deactivateSelection(Query.from(SearchResultSelection.class).where("_id = ?", selectionId).first());

        page.getResponse().sendRedirect(
                page.toolUrl(CmsTool.class, "/content/edit.jsp",
                        "id", state.getId()));
    }

    /**
     * Publishes the given {@code object} in the given {@code site}
     * as the given {@code user} without validation.  This method should be
     * replaced by more general changes to {@link Content.Static#publish(Object, com.psddev.cms.db.Site, com.psddev.cms.db.ToolUser)}
     * that prevent validation on {@link Draft}-state objects that are not requested
     * for scheduling.
     * @param object Can't be {@code null}.
     * @param site May be {@code null}.
     * @param user May be {@code null}.
     * @return Can be used to revert all changes. May be {@code null}.
     */
    private static History publishUnsafely(Object object, Site site, ToolUser user) {

        State state = State.getInstance(object);
        Content.ObjectModification contentData = state.as(Content.ObjectModification.class);
        Site.ObjectModification siteData = state.as(Site.ObjectModification.class);

        if (object instanceof Site) {
            site = (Site) object;
            siteData.setOwner(site);

        } else if (state.isNew()
                && siteData.getOwner() == null) {
            siteData.setOwner(site);
        }

        Date now = new Date();
        Date publishDate = contentData.getPublishDate();
        ToolUser publishUser = contentData.getPublishUser();

        if (publishDate == null) {
            contentData.setPublishDate(now);
        }

        if (publishUser == null) {
            contentData.setPublishUser(user);
        }

        contentData.setUpdateDate(now);
        contentData.setUpdateUser(user);

        if (object instanceof Draft) {
            state.save();
            return null;

        } else {
            try {
                state.beginWrites();
                state.saveUnsafely();

                History history = new History(user, object);

                history.save();
                state.commitWrites();
                return history;

            } finally {
                state.endWrites();
            }
        }
    }
}
