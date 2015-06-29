package com.psddev.cms.tool.page;

import com.psddev.cms.db.Content;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.Search;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PaginatedResult;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeReference;
import com.psddev.dari.util.UrlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RoutingFilter.Path(application = "cms", value = BulkArchive.PATH)
public class BulkArchive extends PageServlet {

    public static final String PATH = "bulkArchive";
    private static final String TARGET = "bulkArchive";

    private static final String DEFAULT_ERROR_MESSAGE = "An error has occurred.";

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkArchive.class);

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {

        execute(new Context(page));
    }

    public void execute(ToolPageContext page, Search search, SearchResultSelection selection, WidgetState widgetState, Action action) throws IOException, ServletException {

        Context context = new Context(page, search, selection);
        context.setWidgetState(widgetState);
        context.setAction(action);
        execute(context);
    }

    public static enum Action {
        RESTORE,
        ARCHIVE
    }

    private void execute(Context page) throws IOException, ServletException {

        Action action = page.getAction();

        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }

        long availableCount = Action.RESTORE.equals(action) ? page.getAvailableRestoreCount() : page.getAvailableArchiveCount();

        if (availableCount == 0) {
            return;
        }

        String actionIconClass = "icon-action-" + (Action.RESTORE.equals(action) ? "restore" : "trash");

        switch (page.getWidgetState()) {

            case CONFIRM:
                page.writeStart("form",
                        "method", "post",
                        "target", TARGET,
                        "action", new UrlBuilder(page.getRequest()).absolutePath(page.cmsUrl(PATH)).currentParameters().parameter(Context.WIDGET_STATE_PARAMETER, WidgetState.BUTTON));

                page.writeStart("p");
                page.writeHtml("Click below to confirm bulk " + action.name().toLowerCase() + " of " + availableCount + " items.");
                page.writeEnd();

                page.writeStart("button", "class", actionIconClass).writeHtml("Confirm bulk " + action.name()).writeEnd();
                page.writeEnd();

                break;

            case BUTTON:
            default:

                if (page.isFormPost()) {

                    Iterator queryIterator = page.itemsQuery().noCache().iterable(0).iterator();

                    Map<String, Integer> messageMap = new LinkedHashMap<>();

                    int successCount = 0;

                    try {
                        while (queryIterator.hasNext()) {

                            try {
                                if (Action.RESTORE.equals(action)) {
                                    page.restore(queryIterator.next());
                                } else if (Action.ARCHIVE.equals(action)) {
                                    page.trash(queryIterator.next());
                                }

                                successCount ++;
                            } catch (Exception e) {
                                page.getErrors().add(e);
                            }
                        }
                    } finally {
                        if (queryIterator instanceof Closeable) {
                            ((Closeable) queryIterator).close();
                        }
                    }

                    // Build user notification String from errors' localized messages.
                    // Stack repeat errors and track the counts of each type.
                    if (page.getErrors().size() > 0) {

                        for (Throwable throwable : page.getErrors()) {

                            String message = throwable.getLocalizedMessage() != null ? throwable.getLocalizedMessage() : DEFAULT_ERROR_MESSAGE;

                            int messageCount = ObjectUtils.to(int.class, messageMap.get(message));

                            messageMap.put(message, messageCount + 1);

                            LOGGER.warn("BulkArchive Error: ", throwable);
                        }

                    }

                    List<String> errorMessages = new ArrayList<>();

                    // Display error notifications.
                    if (messageMap.size() > 0) {
                        for (Map.Entry<String, Integer> entry : messageMap.entrySet()) {
                            errorMessages.add(entry.getKey() + (entry.getValue() > 1 ? " (" + entry.getValue() + ")" : ""));
                        }

                        page.writeStart("div", "class", "message message-error");
                        page.writeHtml(StringUtils.join(errorMessages, "<br>"));
                        page.writeEnd(); // end .message-error

                    }

                    // Display success notification.
                    if (successCount > 0) {

                        page.writeStart("div", "class", "message message-success");
                        if (Action.RESTORE.equals(action)) {
                            page.writeHtml("Successfully restored ");
                        } else if (Action.ARCHIVE.equals(action)) {
                            page.writeHtml("Successfully archived ");
                        }
                        page.writeHtml(successCount);
                        page.writeHtml(" items. ");

                        String returnUrl = page.param(String.class, "returnUrl");

                        if (!ObjectUtils.isBlank(returnUrl)) {
                            page.writeStart("a",
                                    "href", returnUrl);
                            page.writeHtml("Return to search.");
                            page.writeEnd();
                        }
                        page.writeEnd(); // end .message-success
                    }
                } else {
                    page.writeStart("div", "class", "searchResult-action-simple");
                    page.writeStart("a",
                            "class", "button " + actionIconClass,
                            "target", TARGET,
                            "href", new UrlBuilder(page.getRequest()).
                                    absolutePath(page.cmsUrl(PATH)).
                                    currentParameters().
                                    parameter(Context.SELECTION_ID_PARAMETER, page.getSelection() != null ? page.getSelection().getId() : null).
                                    parameter("action", action.name()));

                    if (Action.RESTORE.equals(action)) {
                        page.writeHtml("Bulk Restore");
                    } else if (Action.ARCHIVE.equals(action)) {
                        page.writeHtml("Bulk Archive");
                    }
                    page.writeHtml(page.getSelection() != null ? " Selected" : " All");
                    page.writeEnd();
                    page.writeEnd();
                }
                break;
        }
    }

    public static enum WidgetState {
        BUTTON,
        CONFIRM
    }

    /**
     * A private extension of ToolPageContext for use only with the BulkArchive servlet widget.
     */
    private static class Context extends ToolPageContext {

        public static final String WIDGET_STATE_PARAMETER = "bulkArchiveState";
        public static final String ACTION_PARAMETER = "action";
        public static final String SELECTION_ID_PARAMETER = "selectionId";
        public static final String SEARCH_PARAMETER = "search";

        private static final Integer READ_PAGE_SIZE = 20;

        private Search search;
        private SearchResultSelection selection;
        private WidgetState widgetState;
        private Action action;

        public Context(PageContext pageContext) {
            this(pageContext.getServletContext(), (HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(), null, null);
        }

        public Context(ToolPageContext page) {

            this(page.getServletContext(), page.getRequest(), page.getResponse(), null, null);
        }

        public Context(ToolPageContext page, Search search, SearchResultSelection selection) {

            this(page.getServletContext(), page.getRequest(), page.getResponse(), search, selection);
        }

        public Context(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, Search search, SearchResultSelection selection) {

            super(servletContext, request, response);

            String selectionId = param(String.class, SELECTION_ID_PARAMETER);

            this.widgetState = ObjectUtils.firstNonNull(param(WidgetState.class, WIDGET_STATE_PARAMETER), WidgetState.CONFIRM);

            this.action = ObjectUtils.firstNonNull(param(Action.class, ACTION_PARAMETER), Action.ARCHIVE);

            if (selection != null) {

                setSelection(selection);

            } else if (!ObjectUtils.isBlank(selectionId)) {

                LOGGER.debug("Found " + SELECTION_ID_PARAMETER + " query parameter with value: " + selectionId);
                SearchResultSelection queriedSelection = (SearchResultSelection) Query.fromAll().where("_id = ?", selectionId).first();

                if (queriedSelection == null) {
                    throw new IllegalArgumentException("No SearchResultSelection exists for id " + selectionId);
                }

                setSelection(queriedSelection);
            } else if (search != null) {

                setSearch(search);
            } else {

                Search searchFromJson = searchFromJson();

                if (searchFromJson == null) {

                    LOGGER.debug("Could not obtain Search object from JSON query parameter");
                    searchFromJson = new Search();
                }

                setSearch(searchFromJson);
            }
        }

        public Search getSearch() {
            return search;
        }

        public void setSearch(Search search) {
            this.search = search;
        }

        public SearchResultSelection getSelection() {
            return selection;
        }

        public void setSelection(SearchResultSelection selection) {
            this.selection = selection;
        }

        public WidgetState getWidgetState() {
            return widgetState;
        }

        public void setWidgetState(WidgetState widgetState) {
            this.widgetState = widgetState;
        }

        public Action getAction() {
            return action;
        }

        public void setAction(Action action) {
            this.action = action;
        }

        /**
         * Produces a Search object from JSON and prevents errors when the same query parameter name is used for non-JSON Search representation.
         * @return Search if a query parameter specifies valid Search JSON, null otherwise.
         */
        public Search searchFromJson() {

            Search search = null;

            String searchParam = param(String.class, SEARCH_PARAMETER);

            if (searchParam != null) {

                try {

                    Map<String, Object> searchJson = ObjectUtils.to(new TypeReference<Map<String, Object>>() {
                    }, ObjectUtils.fromJson(searchParam));
                    search = new Search();
                    search.getState().setValues(searchJson);

                } catch (Exception ignore) {

                    // Ignore.  Search will be constructed below using ToolPageContext
                }
            }

            return search;
        }

        // Produces a Query for objects to be bulk workflow transitioned.
        public Query itemsQuery() {

            if (getSearch() != null) {

                return getSearch().toQuery(getSite());
            } else if (getSelection() != null) {
                return getSelection().createItemsQuery();
            }

            throw new IllegalStateException("No Search or SearchResultsSelection populated.  Cannot create items Query.");
        }

        private long getAvailableActionCount(boolean archive) {

            if (getSelection() != null) {

                PaginatedResult result = itemsQuery().noCache().select(0, READ_PAGE_SIZE);

                int count = 0;

                do {
                    for (Object item : result.getItems()) {

                        State itemState = State.getInstance(item);
                        String typePermissionId = "type/" + itemState.getTypeId();

                        if (archive ^ itemState.as(Content.ObjectModification.class).isTrash() &&
                                hasPermission(typePermissionId + "/write") &&
                                hasPermission(typePermissionId + "/bulkArchive")) {

                            count ++;
                        }
                    }
                } while (result.hasNext() && (result = itemsQuery().noCache().select(result.getNextOffset(), READ_PAGE_SIZE)) != null);

                return count;
            } else if (getSearch() != null && (archive ^ getSearch().getVisibilities().contains("b.cms.content.trashed"))) {

                ObjectType selectedType = getSearch().getSelectedType();

                if (selectedType == null) {
                    return 0;
                }

                String typePermissionId = "type/" + selectedType.getId();

                if (!hasPermission(typePermissionId + "/write") ||
                        !hasPermission(typePermissionId + "/bulkArchive")) {
                    return 0;
                }

                return getSearch().toQuery(getSite()).count();
            }

            return 0;
        }

        public long getAvailableArchiveCount() {

            return getAvailableActionCount(true);
        }

        public long getAvailableRestoreCount() {

            return getAvailableActionCount(false);
        }
    }
}
