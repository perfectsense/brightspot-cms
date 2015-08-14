package com.psddev.cms.tool.widget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.tool.QueryRestriction;
import org.joda.time.DateTime;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.ToolRole;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DefaultDashboardWidget;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.QueryFilter;
import com.psddev.dari.db.State;
import com.psddev.dari.util.PaginatedResult;

public class RecentActivityWidget extends DefaultDashboardWidget {

    private static final int[] LIMITS = { 10, 20, 50 };

    @Override
    public int getColumnIndex() {
        return 0;
    }

    @Override
    public int getWidgetIndex() {
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void writeHtml(ToolPageContext page, Dashboard dashboard) throws IOException, ServletException {
        ObjectType itemType = Query.findById(ObjectType.class, page.pageParam(UUID.class, "itemType", null));
        Type type = page.pageParam(Type.class, "type", Type.ANYONE);
        String valueParameter = type + ".value";
        Object valueObject = Query.from(Object.class).where("_id = ?", page.pageParam(UUID.class, valueParameter, null)).first();
        long offset = page.param(long.class, "offset");
        int limit = page.pageParam(Integer.class, "limit", 20);

        if (type == null) {
            type = Type.ANYONE;
        }

        PaginatedResult<?> result;

        List<String> visibilities = page.pageParams(String.class, Search.VISIBILITIES_PARAMETER, new ArrayList<>());

        if (valueObject == null && (type == Type.ROLE || type == Type.USER)) {
            result = null;

        } else {
            Query<?> contentQuery = (itemType != null ? Query.fromType(itemType) : Query.fromGroup(Content.SEARCHABLE_GROUP))
                    .where(page.siteItemsSearchPredicate())
                    .and(Content.UPDATE_DATE_FIELD + " != missing")
                    .sortDescending(Content.UPDATE_DATE_FIELD);

            switch (type) {
                case ROLE :
                    contentQuery.and(Content.UPDATE_USER_FIELD + " = ?", Query.from(ToolUser.class).where("role = ?", valueObject));
                    break;

                case USER :
                    contentQuery.and(Content.UPDATE_USER_FIELD + " = ?", valueObject);
                    break;

                case ME :
                    contentQuery.and(Content.UPDATE_USER_FIELD + " = ?", page.getUser());
                    break;

                default :
                    break;
            }

            Predicate visibilitiesPredicate = Search.getVisibilitiesPredicate(itemType, visibilities, null, false);
            QueryFilter<Object> visibilitiesFilter = null;

            if (visibilitiesPredicate != null) {
                contentQuery.and(visibilitiesPredicate);
            } else {
                visibilitiesFilter = item -> State.getInstance(item).isVisible();
            }

            QueryRestriction.updateQueryUsingAll(contentQuery, page);

            result = contentQuery.and("_any matches *").selectFiltered(offset, limit, visibilitiesFilter);
        }

        page.writeStart("div", "class", "widget");
            page.writeStart("h1", "class", "icon icon-list");
                page.writeHtml("Recent Activity");
            page.writeEnd();

            page.writeStart("div", "class", "widget-filters");
                for (Class<? extends QueryRestriction> c : QueryRestriction.classIterable()) {
                    page.writeQueryRestrictionForm(c);
                }

                page.writeStart("form",
                        "method", "get",
                        "action", page.url(null));

                    page.writeTypeSelect(
                            com.psddev.cms.db.Template.Static.findUsedTypes(page.getSite()),
                            itemType,
                            "Any Types",
                            "data-bsp-autosubmit", "",
                            "name", "itemType",
                            "data-searchable", "true");

                    page.writeStart("select", "data-bsp-autosubmit", "", "name", "type");
                        for (Type t : Type.values()) {
                            if (t != Type.ROLE || Query.from(ToolRole.class).first() != null) {
                                page.writeStart("option",
                                        "selected", t.equals(type) ? "selected" : null,
                                        "value", t.name());
                                    page.writeHtml(t.getDisplayName());
                                page.writeEnd();
                            }
                        }
                    page.writeEnd();

                    Query<?> valueQuery;

                    if (type == Type.ROLE) {
                        valueQuery = Query.from(ToolRole.class).sortAscending("name");

                    } else if (type == Type.USER) {
                        valueQuery = Query.from(ToolUser.class).sortAscending("name");

                    } else {
                        valueQuery = null;
                    }

                    if (valueQuery != null) {
                        if (valueQuery.hasMoreThan(250)) {
                            State valueState = State.getInstance(valueObject);

                            page.writeElement("input",
                                    "data-bsp-autosubmit", "",
                                    "type", "text",
                                    "class", "objectId",
                                    "data-editable", false,
                                    "data-label", valueState != null ? valueState.getLabel() : null,
                                    "data-typeIds", ObjectType.getInstance(valueQuery.getGroup()).getId(),
                                    "name", valueParameter,
                                    "value", valueState != null ? valueState.getId() : null);

                        } else {
                            page.writeStart("select",
                                    "data-bsp-autosubmit", "",
                                    "name", valueParameter,
                                    "data-searchable", "true");

                                page.writeStart("option", "value", "").writeEnd();

                                for (Object v : valueQuery.selectAll()) {
                                    State state = State.getInstance(v);

                                    page.writeStart("option",
                                            "value", state.getId(),
                                            "selected", v.equals(valueObject) ? "selected" : null);
                                        page.writeHtml(state.getLabel());
                                    page.writeEnd();
                                }

                            page.writeEnd();
                        }
                    }

                    page.writeTag("input", "type", "hidden", "name", Search.VISIBILITIES_PARAMETER, "value", ""); // extra hidden field so ToolPageContext#pageParams() knows we are intentionally submitting nothing
                    page.writeMultipleVisibilitySelect(itemType, visibilities, "name", Search.VISIBILITIES_PARAMETER, "data-bsp-autosubmit", "");
                page.writeEnd();
            page.writeEnd();

            if (result == null) {
                page.writeStart("div", "class", "message message-warning");
                    page.writeStart("p");
                        page.writeHtml("Please select a ");
                        page.writeHtml(type.getDisplayName());
                        page.writeHtml(".");
                    page.writeEnd();
                page.writeEnd();

            } else if (!result.hasPages()) {
                page.writeStart("div", "class", "message message-info");
                    page.writeStart("p");
                        page.writeHtml("No recent activity!");
                    page.writeEnd();
                page.writeEnd();

            } else {
                page.writeStart("ul", "class", "pagination");

                    if (result.hasPrevious()) {
                        page.writeStart("li", "class", "first");
                            page.writeStart("a",
                                    "href", page.url("", "offset", result.getFirstOffset()));
                                page.writeHtml("Newest");
                            page.writeEnd();
                        page.writeEnd();

                        page.writeStart("li", "class", "previous");
                            page.writeStart("a",
                                    "href", page.url("", "offset", result.getPreviousOffset()));
                                page.writeHtml("Newer ").writeHtml(limit);
                            page.writeEnd();
                        page.writeEnd();
                    }

                    if (result.getOffset() > 0
                            || result.hasNext()
                            || result.getItems().size() > LIMITS[0]) {
                        page.writeStart("li");
                            page.writeStart("form",
                                    "data-bsp-autosubmit", "",
                                    "method", "get",
                                    "action", page.url(null));
                                page.writeStart("select", "name", "limit");
                                    for (int l : LIMITS) {
                                        page.writeStart("option",
                                                "value", l,
                                                "selected", limit == l ? "selected" : null);
                                            page.writeHtml("Show ");
                                            page.writeHtml(l);
                                        page.writeEnd();
                                    }
                                page.writeEnd();
                            page.writeEnd();
                        page.writeEnd();
                    }

                    if (result.hasNext()) {
                        page.writeStart("li", "class", "next");
                            page.writeStart("a",
                                    "href", page.url("", "offset", result.getNextOffset()));
                                page.writeHtml("Older ").writeHtml(limit);
                            page.writeEnd();
                        page.writeEnd();
                    }

                page.writeEnd();

                page.writeStart("table", "class", "links table-striped pageThumbnails").writeStart("tbody");

                    String lastUpdateDate = null;

                    for (Object content : result.getItems()) {
                        State contentState = State.getInstance(content);
                        String permalink = contentState.as(Directory.ObjectModification.class).getPermalink();
                        Content.ObjectModification contentData = contentState.as(Content.ObjectModification.class);
                        DateTime updateDateTime = page.toUserDateTime(contentData.getUpdateDate());
                        String updateDate = page.formatUserDate(updateDateTime);
                        ToolUser updateUser = contentData.getUpdateUser();

                        page.writeStart("tr", "data-preview-url", permalink);
                            page.writeStart("td", "class", "date");
                                if (!updateDate.equals(lastUpdateDate)) {
                                    page.writeHtml(updateDate);
                                    lastUpdateDate = updateDate;
                                }
                            page.writeEnd();

                            page.writeStart("td", "class", "time");
                                page.writeHtml(page.formatUserTime(updateDateTime));
                            page.writeEnd();

                            page.writeStart("td");
                                page.writeTypeLabel(content);
                            page.writeEnd();

                            page.writeStart("td", "data-preview-anchor", "");
                                page.writeStart("a",
                                        "href", page.objectUrl("/content/edit.jsp", content),
                                        "target", "_top");
                                    page.writeObjectLabel(content);
                                page.writeEnd();
                            page.writeEnd();

                            page.writeStart("td");
                                page.writeObjectLabel(updateUser);
                            page.writeEnd();
                        page.writeEnd();
                    }

                page.writeEnd().writeEnd();
            }

        page.writeEnd();
    }

    private enum Type {

        ANYONE("Anyone"),
        ME("Me"),
        ROLE("Role"),
        USER("User");

        private String displayName;

        private Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
