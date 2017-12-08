package com.psddev.cms.tool.widget;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.ToolEntity;
import com.psddev.cms.db.ToolRole;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.search.MixedSearchResultView;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;

@ToolUi.DisplayName("Collections Widget")
public class SelectionsWidget extends AbstractPaginatedResultWidget<SearchResultSelection> {

    private static final String TOOL_ENTITY_TYPE_PARAMETER = "toolEntityType";
    private static final String TOOL_ENTITY_VALUE_PARAMETER = "toolEntity";

    @Override
    public String getTitle(ToolPageContext page) throws IOException {
        return page.localize(SelectionsWidget.class, "title");
    }

    @Override
    public Query<SearchResultSelection> getQuery(ToolPageContext page) {

        Query<SearchResultSelection> query = Query.from(SearchResultSelection.class).where("name != missing");

        ToolEntityType entityType = page.pageParam(ToolEntityType.class, TOOL_ENTITY_TYPE_PARAMETER, ToolEntityType.ANYONE);

        UUID entityId = null;

        if (entityType == ToolEntityType.USER || entityType == ToolEntityType.ROLE) {
            entityId = page.pageParam(UUID.class, TOOL_ENTITY_VALUE_PARAMETER, null);
        } else if (entityType == ToolEntityType.ME) {
            entityId = page.getUser().getId();
        }

        if (entityId != null) {
            query.and("entities = ?", entityId);
        }

        return query;
    }

    @Override
    public void writeFiltersHtml(ToolPageContext page) throws IOException {
        page.writeStart("select",
                "data-bsp-autosubmit", "",
                "name", TOOL_ENTITY_TYPE_PARAMETER,
                "data-searchable", "true");

            ToolEntityType userType = page.pageParam(ToolEntityType.class, TOOL_ENTITY_TYPE_PARAMETER, ToolEntityType.ANYONE);
            for (ToolEntityType t : ToolEntityType.values()) {
                if (t != ToolEntityType.ROLE || Query.from(ToolRole.class).first() != null) {
                    page.writeStart("option",
                            "selected", t.equals(userType) ? "selected" : null,
                            "value", t.name());
                        page.writeHtml(page.localize(null, t.getResourceKey()));
                    page.writeEnd();
                }
            }

        page.writeEnd();

        // TODO: move somewhere reusable (duplicated in other widgets)
        Query<?> toolEntityQuery;

        if (userType == ToolEntityType.ROLE) {
            toolEntityQuery = Query.from(ToolRole.class).sortAscending("name");

        } else if (userType == ToolEntityType.USER) {
            toolEntityQuery = Query.from(ToolUser.class).sortAscending("name");

        } else {
            toolEntityQuery = null;
        }

        if (toolEntityQuery != null) {
            Object toolEntity = Query.from(Object.class).where("_id = ?", page.pageParam(UUID.class, TOOL_ENTITY_VALUE_PARAMETER, null)).first();
            if (toolEntityQuery.hasMoreThan(250)) {
                State toolEntityState = State.getInstance(toolEntity);

                page.writeElement("input",
                        "type", "text",
                        "class", "objectId",
                        "data-bsp-autosubmit", "",
                        "data-editable", false,
                        "data-label", toolEntityState != null ? toolEntityState.getLabel() : null,
                        "data-typeIds", ObjectType.getInstance(ToolRole.class).getId(),
                        "name", TOOL_ENTITY_VALUE_PARAMETER,
                        "value", toolEntityState != null ? toolEntityState.getId() : null);

            } else {
                page.writeStart("select",
                        "name", TOOL_ENTITY_VALUE_PARAMETER,
                        "data-bsp-autosubmit", "",
                        "data-searchable", "true");
                    page.writeStart("option", "value", "").writeEnd();
                    for (Object v : toolEntityQuery.selectAll()) {
                        State userState = State.getInstance(v);

                        page.writeStart("option",
                                "value", userState.getId(),
                                "selected", v.equals(toolEntity) ? "selected" : null);
                            page.writeHtml(userState.getLabel());
                        page.writeEnd();
                    }
                page.writeEnd();
            }
        }
    }

    @Override
    public void writeResultsItemHtml(ToolPageContext page, SearchResultSelection selection) throws IOException {

        Search search = new Search();
        search.setAdditionalPredicate(selection.createItemsQuery().getPredicate().toString());
        search.setLimit(10);

        page.writeStart("tr");
            page.writeStart("td");

                page.writeStart("a",
                        "target", "_top",
                        "href", page.cmsUrl("/searchAdvancedFull",
                                "search", ObjectUtils.toJson(search.getState().getSimpleValues()),
                                "view", MixedSearchResultView.class.getCanonicalName()));
                    page.writeObjectLabel(selection);
                page.writeEnd();

            page.writeEnd();
            page.writeStart("td");
                Set<ToolEntity> entities = selection.getEntities();
                page.writeHtml(!ObjectUtils.isBlank(entities)
                        ? entities.stream().map(e -> e.getState().getLabel()).collect(Collectors.joining(", "))
                        : "");
            page.writeEnd();
            page.writeStart("td");
                page.writeHtml(page.localize(SelectionsWidget.class, ImmutableMap.of("count", selection.size()), "numItems"));
            page.writeEnd();
        page.writeEnd();
    }

    // --- Record Support ---

    @Override
    public String getLabel() {
        return getState().getType().getDisplayName();
    }

    private enum ToolEntityType {

        ANYONE("label.anyone"),
        ME("label.me"),
        ROLE("label.role"),
        USER("label.user");

        private String resourceKey;

        ToolEntityType(String resourceKey) {
            this.resourceKey = resourceKey;
        }

        public String getResourceKey() {
            return resourceKey;
        }
    }
}
