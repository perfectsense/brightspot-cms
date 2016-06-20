package com.psddev.cms.tool.page.content;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.SearchResultField;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;

import java.io.IOException;
import java.util.List;

public class OpenContentSearchResultField implements SearchResultField {

    @Override
    public String getDisplayName() {
        return "Viewers";
    }

    @Override
    public boolean isSupported(ObjectType objectType) {
        return false;
    }

    @Override
    public String createDataCellText(Object o) {
        return null;
    }

    @Override
    public void writeTableDataCellHtml(ToolPageContext page, Object item) throws IOException {

        State state = State.getInstance(item);
        List<OpenContent> openContentList = Query.from(OpenContent.class).where("contentId = ?", state.getId()).selectAll();

        page.writeStart("td", "style", "width: 120px;");
        {
            page.writeStart("div",
                "class", "toolViewers" + (openContentList.size() > 0 ? " hasViewers" : ""),
                "data-content-id", state.getId().toString()
            );
            {
                for (OpenContent openContent : openContentList) {

                    ToolUser user = Query.from(ToolUser.class).where("id = ?", openContent.getUserId()).first();

                    page.writeStart("div",
                        "class", "toolViewer" + (openContent.isClosed() ? " viewerClosed" : ""),
                        "data-user-id", user.getId().toString(),
                        "title", user.getName());
                    {
                        page.writeUserAvatar(user, null, null);
                    }
                    page.writeEnd();
                }
            }
            page.writeEnd();
        }
        page.writeEnd();
    }

    @Override
    public boolean isDefault(ObjectType type) {
        return false;
    }
}
