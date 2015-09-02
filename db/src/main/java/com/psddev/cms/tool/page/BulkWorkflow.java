package com.psddev.cms.tool.page;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.Workflow;
import com.psddev.cms.db.WorkflowLog;
import com.psddev.cms.db.WorkflowState;
import com.psddev.cms.db.WorkflowTransition;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RoutingFilter.Path(application = "cms", value = BulkWorkflow.PATH)
public class BulkWorkflow extends PageServlet {

    public static final String PATH = "bulkWorkflow";

    public static final String TARGET = "_top";

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkWorkflow.class);
    private static final String DEFAULT_ERROR_MESSAGE = "An error has occurred.";

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {

        execute(new Context(page));
    }

    public void execute(ToolPageContext page, Search search, SearchResultSelection selection, WidgetState widgetState) throws IOException, ServletException {

        Context context = new Context(page, selection, search);

        context.setWidgetState(widgetState);

        execute(context);
    }

    private void execute(Context page) throws IOException, ServletException {

        if (WidgetState.BUTTON.equals(page.getWidgetState()) && !page.hasAnyTransitions()) {
            return;
        }

        switch (page.getWidgetState()) {

        // BUTTON state is used to display a button to invoke the bulk workflow detail page
        case BUTTON:

            page.writeStart("div", "class", "searchResult-action-simple");
            page.writeStart("a",
                    "class", "button",
                    "target", TARGET,
                    "href", getActionUrl(page, null, null, null, WidgetState.DETAIL));
            page.writeHtml("Bulk Workflow");
            page.writeHtml(page.getSelection() != null ? " Selected" : "");
            page.writeEnd();
            page.writeEnd();

            break;

        // CONFIRM state is shown to the ToolUser before action is taken
        case CONFIRM:

            page.writeHeader();

            writeConfirmStateHtml(page);

            page.writeFooter();

            break;

        // DETAIL state shows all available bulk workflow transition options for the provided Search or SearchResultSelection.
        // Transitions for which the ToolUser does not have permission are not displayed.
        case DETAIL:

        default:

            page.writeHeader();

            if (page.isFormPost()) {

                Search search = page.getSearch();

                List<String> originalVisibilities = new ArrayList<>();

                // Ensure that the Search object only returns results with the specified workflow state.
                // Store original visibilities to present a new DETAIL view after action is taken.
                if (search != null) {

                    originalVisibilities.addAll(search.getVisibilities());

                    if (search.getVisibilities() != null) {
                        search.getVisibilities().clear();
                    } else {
                        search.setVisibilities(new ArrayList<>());
                    }

                    search.getVisibilities().add("w." + page.param(String.class, Context.WORKFLOW_STATE_PARAMETER));
                }

                Map<String, Integer> messageMap = new LinkedHashMap<>();

                int successCount = 0;

                for (Object item : page.itemsQuery().selectAll()) {

                    State itemState = State.getInstance(item);

                    // Skip selected items or query results that aren't in the specified workflow state.
                    if (!ObjectUtils.equals(page.param(String.class, Context.WORKFLOW_STATE_PARAMETER), itemState.as(Workflow.Data.class).getCurrentState())) {
                        continue;
                    }

                    if (page.tryWorkflowOnly(item)) {
                        successCount ++;
                    }
                }

                // Build user notification String from errors' localized messages.
                // Stack repeat errors and track the counts of each type.
                if (page.getErrors().size() > 0) {

                    for (Throwable throwable : page.getErrors()) {

                        String message = throwable.getLocalizedMessage() != null ? throwable.getLocalizedMessage() : DEFAULT_ERROR_MESSAGE;

                        int messageCount = ObjectUtils.to(int.class, messageMap.get(message));

                        messageMap.put(message, messageCount + 1);

                        LOGGER.warn("Bulk Workflow Error: ", throwable);
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
                    page.writeHtml("Successfully transitioned ");
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

                // Trigger the Context to rebuild its internal data structure by re-setting the SearchResultSelection or Search.
                if (page.getSelection() != null) {
                    page.setSelection(page.getSelection());
                } else {
                    Search originalSearch = page.getSearch();
                    // Reset the original visibilities that were set on the Search object.
                    if (originalSearch != null) {
                        originalSearch.setVisibilities(originalVisibilities);
                    }
                    page.setSearch(originalSearch);
                }
            }

            writeDetailStateHtml(page);

            page.writeFooter();

            break;
        }
    }

    public static enum WidgetState {
        BUTTON,
        DETAIL,
        CONFIRM,
        DEFAULT
    }

    /**
     * Writes the WidgetState.DETAIL view of the BulkWorkflow widget.  This view displays options for available workflow transitions
     * based on the SearchResultSelection or Search and the user's bulk workflow and individual workflow transition permissions.
     * @param page an instance of Context
     * @throws IOException
     * @throws ServletException
     */
    private void writeDetailStateHtml(Context page) throws IOException, ServletException {

        page.writeStart("div", "class", "widget");
        page.writeStart("h1", "class", "icon icon-object-workflow");
        page.writeHtml("Workflow Options");
        page.writeEnd();

        if (!page.hasAnyTransitions()) {
            page.writeStart("p");
            page.writeHtml("No workflow transitions are available for the specified " + (page.getSelection() != null ? "selection" : "search") + ".");
            page.writeEnd();
        }

        for (Workflow workflow : page.workflows()) {

            for (ObjectType workflowType : page.workflowTypes(workflow)) {

                for (WorkflowState workflowState : page.workflowStates(workflow, workflowType)) {

                    Set<WorkflowTransition> availableTransitions = page.getAvailableTransitions(workflow, workflowType, workflowState);

                    if (availableTransitions.size() == 0) {
                        continue;
                    }

                    page.writeStart("h3");
                    page.writeHtml(page.getWorkflowStateCount(workflowType, workflowState.getName()) + " " + workflowType.getDisplayName() + " ");
                    page.writeStart("span", "class", "visibilityLabel");
                    page.writeHtml(workflowState.getDisplayName());
                    page.writeEnd(); // end .visibilityLabel
                    page.writeEnd();

                    for (WorkflowTransition transition : availableTransitions) {

                        page.writeStart("a",
                                "class", "button",
                                "target", TARGET,
                                "href", getActionUrl(page, workflow, workflowType, workflowState, WidgetState.CONFIRM, "action-workflow", transition.getName()));
                        page.writeHtml(transition.getDisplayName());
                        page.writeEnd();
                    }
                }

            }
        }

        page.writeEnd(); // end .-widget
    }

    /**
     * Writes the WidgetState.CONFIRM view of the BulkWorkflow widget.  This view displays a summary of the requested workflow transition
     * and prompts the user for confirmation to proceed.
     * @param page an instance of Context
     * @throws IOException
     * @throws ServletException
     */
    private void writeConfirmStateHtml(Context page) throws IOException, ServletException {

        page.writeStart("div", "class", "widget");
        page.writeStart("h1");
        page.writeHtml("Confirm Bulk Workflow");
        page.writeEnd();

        ObjectType transitionSourceType = ObjectType.getInstance(page.param(UUID.class, Context.TYPE_ID_PARAMETER));

        String workflowStateName = page.param(String.class, Context.WORKFLOW_STATE_PARAMETER);
        WorkflowState workflowState = null;

        Workflow workflow = Query.from(Workflow.class).where("_id = ?", page.param(String.class, Context.WORKFLOW_ID_PARAMETER)).first();

        for (WorkflowState workflowStateValue : workflow.getStates()) {
            if (ObjectUtils.equals(workflowStateValue.getName(), workflowStateName)) {
                workflowState = workflowStateValue;
                break;
            }
        }

        WorkflowTransition workflowTransition = page.getWorkflowTransition(workflow, transitionSourceType, workflowState, page.param(String.class, "action-workflow"));

        if (workflowTransition == null) {
            return;
        }

        page.writeStart("form",
                "method", "post",
                "action", getActionUrl(page, workflow, transitionSourceType, workflowState, WidgetState.DETAIL));

        page.writeStart("table", "class", "table-striped");

        page.writeStart("tr");
        page.writeStart("td").writeHtml("Type").writeEnd();
        page.writeStart("td").writeHtml(transitionSourceType.getDisplayName()).writeEnd();
        page.writeEnd(); // end row

        page.writeStart("tr");
        page.writeStart("td").writeHtml("Count").writeEnd();
        page.writeStart("td").writeHtml(page.getWorkflowStateCount(transitionSourceType, workflowStateName)).writeEnd();
        page.writeEnd(); // end row

        page.writeStart("tr");
        page.writeStart("td").writeHtml("Current State").writeEnd();
        page.writeStart("td").writeHtml(workflowTransition.getSource().getDisplayName()).writeEnd();
        page.writeEnd(); // end row

        page.writeStart("tr");
        page.writeStart("td").writeHtml("New State").writeEnd();
        page.writeStart("td").writeHtml(workflowTransition.getTarget().getDisplayName()).writeEnd();
        page.writeEnd(); // end row

        page.writeEnd(); // end table

        WorkflowLog log = new WorkflowLog();

        page.writeStart("div", "class", "widget-publishingWorkflowLog");
        page.writeElement("input",
                "type", "hidden",
                "name", "workflowLogId",
                "value", log.getId());

        page.writeFormFields(log);
        page.writeEnd();

        page.writeStart("button", "name", "action-workflow", "value", workflowTransition.getName());
        page.writeHtml("Confirm Bulk " + workflowTransition.getDisplayName());
        page.writeEnd();
        page.writeEnd();

        page.writeEnd();
    }

    /**
     * Helper method for generating a stateful BulkWorkflow servlet URL for forms and anchors.
     * @param page an instance of Context
     * @param workflow A Workflow for which to display or effect transitions.
     * @param workflowType An ObjectType for which the workflow is specified.
     * @param workflowState A WorkflowState of the specified workflow.
     * @param widgetState A state of the BulkWorkflow servlet widget to render.
     * @param params Additional query parameters to attach to the returned URL.
     * @return the requested URL
     */
    private String getActionUrl(Context page, Workflow workflow, ObjectType workflowType, WorkflowState workflowState, WidgetState widgetState, Object... params) {

        UrlBuilder urlBuilder = new UrlBuilder(page.getRequest())
                .absolutePath(page.cmsUrl(PATH));

        urlBuilder.parameter("action-workflow", null);

        if (page.getSearch() != null) {
            // Search uses current page parameters
            urlBuilder.currentParameters();
        }

        // SearchResultSelection uses an ID parameter
        urlBuilder.parameter(Context.SELECTION_ID_PARAMETER, page.getSelection() != null ? page.getSelection().getId() : null);

        urlBuilder.parameter(Context.WORKFLOW_ID_PARAMETER, workflow != null ? workflow.getId() : null);

        urlBuilder.parameter(Context.TYPE_ID_PARAMETER, workflowType != null ? workflowType.getId() : null);

        urlBuilder.parameter(Context.WORKFLOW_STATE_PARAMETER, workflowState != null ? workflowState.getName() : null);

        urlBuilder.parameter(Context.WIDGET_STATE_PARAMETER, widgetState);

        for (int i = 0; i < params.length / 2; i++) {

            urlBuilder.parameter(params[i], params[i + 1]);
        }

        return urlBuilder.toString();
    }

    /**
     * A private extension of ToolPageContext for use only with the BulkWorkflow servlet widget.
     */
    private static class Context extends ToolPageContext {

        public static final String WIDGET_STATE_PARAMETER = "bulkWorkflowState";
        public static final String SELECTION_ID_PARAMETER = "selectionId";
        public static final String TYPE_ID_PARAMETER = "sourceTypeId";
        public static final String WORKFLOW_ID_PARAMETER = "workflowId";
        public static final String WORKFLOW_STATE_PARAMETER = "sourceWorkflowState";
        public static final String SEARCH_PARAMETER = "search";

        private Search search;
        private SearchResultSelection selection;
        private WidgetState widgetState;

        private Map<Workflow, Map<ObjectType, Map<WorkflowState, Map<String, WorkflowTransition>>>> availableTransitionsMap = new LinkedHashMap<>();
        private Map<ObjectType, Map<String, Integer>> workflowStateCounts = new LinkedHashMap<>();
        private boolean hasAnyTransitions = false;

        public Context(PageContext pageContext) {
            this(pageContext.getServletContext(), (HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(), null, null);
        }

        public Context(ToolPageContext page) {

            this(page.getServletContext(), page.getRequest(), page.getResponse(), null, null);
        }

        public Context(ToolPageContext page, SearchResultSelection selection, Search search) {

            this(page.getServletContext(), page.getRequest(), page.getResponse(), selection, search);
        }

        public Context(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response, SearchResultSelection selection, Search search) {

            super(servletContext, request, response);

            String selectionId = param(String.class, SELECTION_ID_PARAMETER);

            this.widgetState = ObjectUtils.firstNonNull(param(WidgetState.class, WIDGET_STATE_PARAMETER), WidgetState.DETAIL);

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
            buildTransitionMap(search);
        }

        public SearchResultSelection getSelection() {
            return selection;
        }

        public void setSelection(SearchResultSelection selection) {
            this.selection = selection;
            buildTransitionMap(selection);
        }

        public WidgetState getWidgetState() {
            return widgetState;
        }

        public void setWidgetState(WidgetState widgetState) {
            this.widgetState = widgetState;
        }

        // Utility methods for navigating the availableTransitionsMap

        public Set<Workflow> workflows() {
            return availableTransitionsMap.keySet();
        }

        private Map<ObjectType, Map<WorkflowState, Map<String, WorkflowTransition>>> getTypeMap(Workflow workflow) {

            Map<ObjectType, Map<WorkflowState, Map<String, WorkflowTransition>>> typeMap = availableTransitionsMap.get(workflow);
            if (typeMap == null) {
                return new HashMap<>();
            }
            return typeMap;
        }

        public Set<ObjectType> workflowTypes(Workflow workflow) {

            return getTypeMap(workflow).keySet();
        }

        private Map<WorkflowState, Map<String, WorkflowTransition>> getStateMap(Workflow workflow, ObjectType workflowType) {

            Map<WorkflowState, Map<String, WorkflowTransition>> stateMap = getTypeMap(workflow).get(workflowType);

            if (stateMap == null) {
                return new HashMap<>();
            }

            return stateMap;
        }

        public Set<WorkflowState> workflowStates(Workflow workflow, ObjectType workflowType) {

            return getStateMap(workflow, workflowType).keySet();
        }

        private Map<String, WorkflowTransition> getTransitionMap(Workflow workflow, ObjectType workflowType, WorkflowState workflowState) {

            Map<String, WorkflowTransition> transitionMap = getStateMap(workflow, workflowType).get(workflowState);

            if (transitionMap == null) {
                return new HashMap<>();
            }

            return transitionMap;
        }

        public Set<WorkflowTransition> getAvailableTransitions(Workflow workflow, ObjectType workflowType, WorkflowState workflowState) {

            return new HashSet<>(getTransitionMap(workflow, workflowType, workflowState).values());
        }

        public WorkflowTransition getWorkflowTransition(Workflow workflow, ObjectType workflowType, WorkflowState workflowState, String transitionName) {

            return getTransitionMap(workflow, workflowType, workflowState).get(transitionName);
        }

        public int getWorkflowStateCount(ObjectType workflowType, String workflowStateName) {

            Map<String, Integer> countMap = workflowStateCounts.get(workflowType);

            if (countMap == null) {
                return 0;
            }

            return ObjectUtils.to(int.class, countMap.get(workflowStateName));
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

        public boolean hasAnyTransitions() {

            return hasAnyTransitions;
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

        /**
         * Clears and sets the internal workflowStateCounts, availableTransitionsMap, and hasAnyTransitions state variables using the
         * SearchResultSelection provided.
         * @param selection a SearchResultSelection representing the objects to be analyzed for Workflow transition availability.
         */
        private void buildTransitionMap(SearchResultSelection selection) {

            workflowStateCounts.clear();
            availableTransitionsMap.clear();
            hasAnyTransitions = false;

            if (selection == null) {

                return;
            }

            Set<ObjectType> itemTypes = new HashSet<>();

            for (Object item : selection.createItemsQuery().selectAll()) {

                State itemState = State.getInstance(item);

                ObjectType itemType = itemState.getType();

                if (itemType == null) {
                    continue;
                }

                if (workflowStateCounts.get(itemType) == null) {
                    workflowStateCounts.put(itemType, new LinkedHashMap<>());
                }

                String itemWorkflowCurrentState = itemState.as(Workflow.Data.class).getCurrentState();

                if (!ObjectUtils.isBlank(itemWorkflowCurrentState)) {
                    int count = ObjectUtils.to(int.class, workflowStateCounts.get(itemType).get(itemWorkflowCurrentState));
                    workflowStateCounts.get(itemType).put(itemWorkflowCurrentState, count + 1);
                }

                itemTypes.add(itemType);
            }

            if (itemTypes.size() == 0) {

                return;
            }

            for (Workflow workflow : Query.from(Workflow.class).where("contentTypes = ?", itemTypes).selectAll()) {

                for (ObjectType workflowType : workflow.getContentTypes()) {

                    Map<ObjectType, Map<WorkflowState, Map<String, WorkflowTransition>>> stateTransitionsByType = availableTransitionsMap.get(workflow);
                    if (stateTransitionsByType == null) {
                        stateTransitionsByType = new LinkedHashMap<>();
                        availableTransitionsMap.put(workflow, stateTransitionsByType);
                    }

                    for (WorkflowState workflowState : workflow.getStates()) {

                        Map<WorkflowState, Map<String, WorkflowTransition>> stateTransitions = stateTransitionsByType.get(workflowType);
                        if (stateTransitions == null) {
                            stateTransitions = new LinkedHashMap<>();
                            stateTransitionsByType.put(workflowType, stateTransitions);
                        }

                        Map<String, Integer> counts = workflowStateCounts.get(workflowType);

                        if (counts != null && ObjectUtils.to(int.class, counts.get(workflowState.getName())) > 0) {

                            Map<String, WorkflowTransition> transitionsFrom = new HashMap<>();
                            for (Map.Entry<String, WorkflowTransition> entry : workflow.getTransitionsFrom(workflowState.getName()).entrySet()) {

                                String typePermissionId = "type/" + workflowType.getId();

                                if (hasPermission(typePermissionId + "/bulkWorkflow") && hasPermission(typePermissionId + "/" + entry.getKey())) {
                                    transitionsFrom.put(entry.getKey(), entry.getValue());
                                }
                            }

                            if (transitionsFrom.size() > 0) {
                                hasAnyTransitions = true;
                            }

                            stateTransitions.put(workflowState, transitionsFrom);
                        }
                    }
                }
            }
        }

        /**
         * Clears and sets the internal workflowStateCounts, availableTransitionsMap, and hasAnyTransitions state variables using the
         * Search provided.
         * @param search a Search representing the objects to be analyzed for Workflow transition availability.
         */
        private void buildTransitionMap(Search search) {

            workflowStateCounts.clear();
            availableTransitionsMap.clear();
            hasAnyTransitions = false;

            if (search == null) {
                return;
            }

            Query<?> query = search.toQuery(getSite());

            ObjectType selectedType = search.getSelectedType();

            if (selectedType == null) {
                return;
            }

            String typePermissionId = "type/" + selectedType.getId();

            workflowStateCounts.put(selectedType, new HashMap<>());

            // check that a single visibility (workflow) has been refined
            if (search.getVisibilities() == null || search.getVisibilities().size() == 0) {
                return;
            }

            Set<String> refinedStates = new HashSet<>();

            for (String currentState : search.getVisibilities()) {
                if (currentState.startsWith("w.")) {
                    refinedStates.add(currentState.substring(2));
                }
                refinedStates.add(currentState);
            }

            if (refinedStates.size() == 0) {
                return;
            }

            for (Workflow workflow : Query.from(Workflow.class).where("contentTypes = ?", selectedType).selectAll()) {

                Map<ObjectType, Map<WorkflowState, Map<String, WorkflowTransition>>> stateTransitionsByType = availableTransitionsMap.get(workflow);
                if (stateTransitionsByType == null) {
                    stateTransitionsByType = new LinkedHashMap<>();
                    availableTransitionsMap.put(workflow, stateTransitionsByType);
                }

                for (WorkflowState workflowState : workflow.getStates()) {

                    // skip workflow states that are not part of the current Search refinement
                    if (!refinedStates.contains(workflowState.getName()) && !refinedStates.contains("w")) {
                        continue;
                    }

                    Map<WorkflowState, Map<String, WorkflowTransition>> stateTransitions = stateTransitionsByType.get(selectedType);
                    if (stateTransitions == null) {
                        stateTransitions = new LinkedHashMap<>();
                        stateTransitionsByType.put(selectedType, stateTransitions);
                    }

                    long stateCount = Query.fromQuery(query).where("cms.workflow.currentState = ?", workflowState.getName()).noCache().count();

                    workflowStateCounts.get(selectedType).put(workflowState.getName(), ObjectUtils.to(Integer.class, stateCount));

                    if (stateCount > 0) {

                        Map<String, WorkflowTransition> transitionsFrom = new HashMap<>();
                        for (Map.Entry<String, WorkflowTransition> entry : workflow.getTransitionsFrom(workflowState.getName()).entrySet()) {

                            if (hasPermission(typePermissionId + "/bulkWorkflow") && hasPermission(typePermissionId + "/" + entry.getKey())) {
                                transitionsFrom.put(entry.getKey(), entry.getValue());
                            }
                        }

                        if (transitionsFrom.size() > 0) {
                            hasAnyTransitions = true;
                        }

                        stateTransitions.put(workflowState, transitionsFrom);
                    }
                }
            }
        }

        /**
         * Tries to apply a workflow action to the given {@code object} if the
         * user has asked for it in the current request.
         *
         * @param object Can't be {@code null}.
         * @param {@code true} if the application of a workflow action is tried.
         */
        public boolean tryWorkflowOnly(Object object) {
            if (!isFormPost()) {
                return false;
            }

            String action = param(String.class, "action-workflow");

            if (ObjectUtils.isBlank(action)) {
                return false;
            }

            State state = State.getInstance(object);
            Draft draft = getOverlaidDraft(object);
            Workflow.Data workflowData = state.as(Workflow.Data.class);
            String oldWorkflowState = workflowData.getCurrentState();

            try {
                state.beginWrites();

                Workflow workflow = Query.from(Workflow.class).where("contentTypes = ?", state.getType()).first();

                if (workflow != null) {
                    WorkflowTransition transition = workflow.getTransitions().get(action);

                    if (transition != null) {

                        if (!hasPermission("type/" + state.getTypeId() + "/bulkWorkflow") || !hasPermission("type/" + state.getTypeId() + "/" + transition.getName())) {
                            throw new IllegalAccessException("You do not have permission to " + transition.getDisplayName() + " " + state.getType().getDisplayName());
                        }

                        WorkflowLog log = new WorkflowLog();

                        UUID logId = log.getId();

                        state.as(Content.ObjectModification.class).setDraft(false);
                        log.getState().setId(param(UUID.class, "workflowLogId"));
                        updateUsingParameters(log);

                        // keep unique ID
                        log.getState().setId(logId);

                        workflowData.changeState(transition, getUser(), log);

                        if (draft == null) {
                            publish(object);

                        } else {
                            draft.as(Workflow.Data.class).changeState(transition, getUser(), log);
                            draft.update(Draft.findOldValues(object), object);
                            publish(draft);
                        }

                        state.commitWrites();
                    }
                }

                return true;

            } catch (Exception error) {

                if (draft != null) {
                    draft.as(Workflow.Data.class).revertState(oldWorkflowState);
                }

                workflowData.revertState(oldWorkflowState);
                getErrors().add(error);
                return false;

            } finally {
                state.endWrites();
            }
        }
    }
}

