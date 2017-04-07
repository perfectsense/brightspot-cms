package com.psddev.cms.tool.content;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.Content;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.History;
import com.psddev.cms.db.Localization;
import com.psddev.cms.db.Schedule;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.ContentEditWidget;
import com.psddev.cms.tool.ContentEditWidgetPlacement;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PaginatedResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RevisionsWidget extends ContentEditWidget {

    @Override
    public boolean shouldDisplay(ToolPageContext page, Object content) {
        return true;
    }

    @Override
    public ContentEditWidgetPlacement getPlacement(ToolPageContext page, Object content) {
        return ContentEditWidgetPlacement.RIGHT;
    }

    @Override
    public double getPosition(ToolPageContext page, Object content, ContentEditWidgetPlacement placement) {
        return 20.0;
    }

    @Override
    public String getHeading(ToolPageContext page, Object content) {
        return Localization.currentUserText(RevisionsWidget.class, "title");
    }

    @Override
    public void display(ToolPageContext page, Object content, ContentEditWidgetPlacement placement) throws IOException {
        State state = State.getInstance(content);

        if (state.isNew()) {
            return;
        }

        List<Draft> scheduled = new ArrayList<>();
        List<Draft> drafts = new ArrayList<>();
        List<History> namedHistories = new ArrayList<>();
        List<History> histories = new ArrayList<>();

        Object selected = page.getOverlaidHistory(content);

        if (selected == null) {
            selected = page.getOverlaidDraft(content);

            if (selected == null) {
                selected = content;
            }
        }

        for (Draft d : Query
                .from(Draft.class)
                .where("objectId = ?", state.getId())
                .selectAll()) {
            if (d.getSchedule() != null) {
                scheduled.add(d);

            } else {
                drafts.add(d);
            }
        }

        scheduled.sort((x, y) -> ObjectUtils.compare(
                x.getSchedule().getTriggerDate(),
                y.getSchedule().getTriggerDate(),
                true));

        drafts.sort((x, y) -> ObjectUtils.compare(
                x.as(Content.ObjectModification.class).getUpdateDate(),
                y.as(Content.ObjectModification.class).getUpdateDate(),
                true));

        for (History h : Query
                .from(History.class)
                .where("name != missing and objectId = ?", state.getId())
                .sortAscending("name")
                .selectAll()) {
            namedHistories.add(h);
        }

        PaginatedResult<History> historiesResult;

        if (page.getCmsTool().isUseOldHistoryIndex()) {
            historiesResult = Query
                    .from(History.class)
                    .where("name = missing and objectId = ?", state.getId())
                    .sortDescending("updateDate")
                    .select(0, 10);

        } else {
            historiesResult = Query
                    .from(History.class)
                    .where("name = missing and getObjectIdUpdateDate ^= ?", state.getId().toString())
                    .sortDescending("getObjectIdUpdateDate")
                    .select(0, 10);
        }

        for (History h : historiesResult.getItems()) {
            histories.add(h);
        }

        State originalState = State.getInstance(Query.fromAll()
                .where("_id = ?", content)
                .noCache()
                .first());

        page.writeStart("ul", "class", "links");
            page.writeStart("li", "class", content.equals(selected) ? "selected" : null);
                page.writeStart("a", "href", page.originalUrl(null, content));
                    page.writeHtml(ObjectUtils.firstNonNull(
                            originalState.getVisibilityLabel(),
                            page.localize(RevisionsWidget.class, "action.viewLive")));
                page.writeEnd();
            page.writeEnd();
        page.writeEnd();

        if (!scheduled.isEmpty()) {
            page.writeStart("h2");
                page.writeHtml(page.localize(RevisionsWidget.class, "subtitle.scheduled"));
            page.writeEnd();

            page.writeStart("ul", "class", "links pageThumbnails");
                for (Draft d : scheduled) {
                    Schedule s = d.getSchedule();
                    String sn = s.getName();

                    page.writeStart("li",
                            "class", d.equals(selected) ? "selected" : null,
                            "data-preview-url", JspUtils.getAbsolutePath(page.getRequest(), "/_preview", "_cms.db.previewId", d.getId()));
                        page.writeStart("a", "href", page.objectUrl(null, d));
                            if (ObjectUtils.isBlank(sn)) {
                                // TODO: LOCALIZE
                                page.writeHtml(page.formatUserDateTime(s.getTriggerDate()));
                                page.writeHtml(" by ");
                                page.writeObjectLabel(s.getTriggerUser());

                            } else {
                                page.writeHtml(sn);
                            }
                        page.writeEnd();
                    page.writeEnd();
                }
            page.writeEnd();
        }

        ObjectType type = state.getType();

        if (type != null && type.as(ToolUi.class).isPublishable()) {
            page.writeStart("h2");
                page.writeHtml("Drafts");
            page.writeEnd();

            page.writeStart("ul", "class", "links pageThumbnails");
                page.writeStart("li", "class", "new");
                    page.writeStart("a",
                            "href", page.cmsUrl("/content/edit/new-draft", "id", state.getId()),
                            "target", "content-edit-new-draft");
                        page.writeHtml(page.localize(Draft.class, "action.newType"));
                    page.writeEnd();
                page.writeEnd();

                for (Draft d : drafts) {
                    String name = d.getName();
                    Content.ObjectModification dcd = d.as(Content.ObjectModification.class);

                    page.writeStart("li",
                            "class", d.equals(selected) ? "selected" : null,
                            "data-preview-url", JspUtils.getAbsolutePath(page.getRequest(), "/_preview", "_cms.db.previewId", d.getId()));
                    page.writeStart("a", "href", page.objectUrl(null, d));
                    // TODO: LOCALIZE
                            if (!ObjectUtils.isBlank(name)) {
                                page.writeHtml(name);
                                page.writeHtml(" - ");
                            }
                            page.writeHtml(page.formatUserDateTime(dcd.getUpdateDate()));
                            page.writeHtml(" by ");
                            page.writeObjectLabel(dcd.getUpdateUser());
                        page.writeEnd();
                    page.writeEnd();
                }
            page.writeEnd();
        }

        if (!namedHistories.isEmpty()) {
            page.writeStart("h2");
                page.writeHtml(page.localize(RevisionsWidget.class, "subtitle.namedPast"));
            page.writeEnd();

            page.writeStart("ul", "class", "links pageThumbnails");
                for (History h : namedHistories) {
                    page.writeStart("li",
                            "class", h.equals(selected) ? "selected" : null,
                            "data-preview-url", JspUtils.getAbsolutePath(page.getRequest(), "/_preview", "_cms.db.previewId", h.getId()));
                        page.writeStart("a", "href", page.objectUrl(null, h));
                            writeHistoryLabel(page, h);
                        page.writeEnd();
                    page.writeEnd();
                }
            page.writeEnd();
        }

        if (!histories.isEmpty()) {
            page.writeStart("h2").writeHtml("Past").writeEnd();

            if (historiesResult.hasNext()) {
                page.writeStart("p");
                    page.writeStart("a",
                            "class", "icon icon-action-search",
                            "target", "_top",
                            "href", page.cmsUrl("/searchAdvancedFull",
                                    Search.IGNORE_SITE_PARAMETER, "true",
                                    Search.SELECTED_TYPE_PARAMETER, ObjectType.getInstance(History.class).getId(),
                                    Search.ADVANCED_QUERY_PARAMETER, "objectId = " + state.getId()));
                        page.writeHtml(page.localize(
                                RevisionsWidget.class,
                                ImmutableMap.of("count", historiesResult.getCount()),
                                "action.viewAll"));
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("h2");
                    page.writeHtml("Past 10");
                page.writeEnd();
            }

            page.writeStart("ul", "class", "links pageThumbnails");
                for (History h : histories) {
                    page.writeStart("li",
                            "class", h.equals(selected) ? "selected" : null,
                            "data-preview-url", JspUtils.getAbsolutePath(page.getRequest(), "/_preview", "_cms.db.previewId", h.getId()));

                        page.writeStart("a", "href", page.objectUrl(null, h));
                            writeHistoryLabel(page, h);
                        page.writeEnd();
                    page.writeEnd();
                }
            page.writeEnd();
        }
    }

    private void writeHistoryLabel(ToolPageContext page, History history) throws IOException {
        Object original = history.getObject();
        String visibilityLabel = page.createVisibilityLabel(original);

        if (!ObjectUtils.isBlank(visibilityLabel)) {
            page.writeStart("span", "class", "visibilityLabel");
            page.writeHtml(visibilityLabel);
            page.writeEnd();
            page.writeHtml(" ");
        }

        page.writeObjectLabel(history);
    }
}
