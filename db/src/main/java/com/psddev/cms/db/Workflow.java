package com.psddev.cms.db;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.db.VisibilityLabel;
import com.psddev.dari.db.VisibilityValues;

@ToolUi.IconName("object-workflow")
@Record.BootstrapPackages(value = "Workflows", depends = ObjectType.class)
public class Workflow extends Record {

    @Indexed(unique = true)
    @Required
    private String name;

    @Indexed
    @ToolUi.Note("Leave blank to apply this workflow to all sites.")
    private Set<Site> sites;

    @Indexed
    @Required
    private Set<ObjectType> contentTypes;

    @Indexed(unique = true)
    @ToolUi.Hidden
    private Set<String> siteContentTypeIds;

    @ToolUi.FieldDisplayType("workflowActions")
    private Map<String, Object> actions;

    /** Returns the name. */
    public String getName() {
        return name;
    }

    /** Sets the name. */
    public void setName(String name) {
        this.name = name;
    }

    public Set<Site> getSites() {
        if (sites == null) {
            sites = new LinkedHashSet<>();
        }
        return sites;
    }

    public void setSites(Set<Site> sites) {
        this.sites = sites;
    }

    public Set<ObjectType> getContentTypes() {
        if (contentTypes == null) {
            contentTypes = new LinkedHashSet<ObjectType>();
        }
        return contentTypes;
    }

    public void setContentTypes(Set<ObjectType> contentTypes) {
        this.contentTypes = contentTypes;
    }

    public Map<String, Object> getActions() {
        if (actions == null) {
            actions = new LinkedHashMap<String, Object>();
        }
        return actions;
    }

    public void setActions(Map<String, Object> actions) {
        this.actions = actions;
    }

    /**
     * Returns a set of all states in this workflow.
     *
     * @return Never {@code null}. Modifiable.
     */
    public Set<WorkflowState> getStates() {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, List<Map<String, Object>>> actions = (Map) getActions();
        List<Map<String, Object>> rawStates = actions.get("states");
        Set<WorkflowState> states = new HashSet<WorkflowState>();

        if (rawStates != null) {
            for (Map<String, Object> s : rawStates) {
                WorkflowState state = new WorkflowState();

                state.setName((String) s.get("name"));
                state.setDisplayName((String) s.get("displayName"));
                states.add(state);
            }
        }

        return states;
    }

    public Map<String, WorkflowTransition> getTransitions() {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, List<Map<String, Object>>> actions = (Map) getActions();
        List<Map<String, Object>> rawStates = actions.get("states");
        List<Map<String, Object>> rawTransitions = actions.get("transitions");
        Map<String, WorkflowTransition> transitions = new HashMap<String, WorkflowTransition>();

        if (rawStates != null && rawTransitions != null) {
            Map<String, WorkflowState> states = new HashMap<String, WorkflowState>();
            WorkflowState state;

            for (Map<String, Object> s : rawStates) {
                state = new WorkflowState();
                state.setName((String) s.get("name"));
                state.setDisplayName((String) s.get("displayName"));
                states.put((String) s.get("id"), state);
            }

            state = new WorkflowState();
            state.setName("New");
            states.put("initial", state);

            state = new WorkflowState();
            state.setName("Published");
            states.put("final", state);

            for (Map<String, Object> t : rawTransitions) {
                WorkflowTransition transition = new WorkflowTransition();
                String name = (String) t.get("name");

                transition.setName(name);
                transition.setDisplayName((String) t.get("displayName"));
                transition.setSource(states.get(t.get("source")));
                transition.setTarget(states.get(t.get("target")));
                transitions.put(name, transition);
            }
        }

        return transitions;
    }

    public Map<String, WorkflowTransition> getTransitionsFrom(String from) {
        if (from == null) {
            from = "New";
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, List<Map<String, Object>>> actions = (Map) getActions();
        List<Map<String, Object>> rawStates = actions.get("states");
        List<Map<String, Object>> rawTransitions = actions.get("transitions");
        Map<String, WorkflowTransition> transitions = new HashMap<String, WorkflowTransition>();

        if (rawStates != null && rawTransitions != null) {
            Map<String, WorkflowState> states = new HashMap<String, WorkflowState>();
            WorkflowState state;

            for (Map<String, Object> s : rawStates) {
                state = new WorkflowState();
                state.setName((String) s.get("name"));
                state.setDisplayName((String) s.get("displayName"));
                states.put((String) s.get("id"), state);
            }

            state = new WorkflowState();
            state.setName("New");
            states.put("initial", state);

            state = new WorkflowState();
            state.setName("Published");
            states.put("final", state);

            for (Map<String, Object> t : rawTransitions) {
                WorkflowTransition transition = new WorkflowTransition();
                String name = (String) t.get("name");
                WorkflowState source = states.get(t.get("source"));

                transition.setName(name);
                transition.setDisplayName((String) t.get("displayName"));
                transition.setSource(source);
                transition.setTarget(states.get(t.get("target")));

                if (!(source == null
                        || !from.equals(source.getName())
                        || "Published".equals(transition.getTarget().getName()))) {
                    transitions.put(name, transition);
                }
            }
        }

        return transitions;
    }

    public Map<String, WorkflowTransition> getTransitionsTo(String to) {
        if (to == null) {
            to = "Published";
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, List<Map<String, Object>>> actions = (Map) getActions();
        List<Map<String, Object>> rawStates = actions.get("states");
        List<Map<String, Object>> rawTransitions = actions.get("transitions");
        Map<String, WorkflowTransition> transitions = new HashMap<>();

        if (rawStates != null && rawTransitions != null) {
            Map<String, WorkflowState> states = new HashMap<>();
            WorkflowState state;

            for (Map<String, Object> s : rawStates) {
                state = new WorkflowState();
                state.setName((String) s.get("name"));
                state.setDisplayName((String) s.get("displayName"));
                states.put((String) s.get("id"), state);
            }

            state = new WorkflowState();
            state.setName("New");
            states.put("initial", state);

            state = new WorkflowState();
            state.setName("Published");
            states.put("final", state);

            for (Map<String, Object> t : rawTransitions) {
                WorkflowTransition transition = new WorkflowTransition();
                String name = (String) t.get("name");
                WorkflowState target = states.get(t.get("target"));

                transition.setName(name);
                transition.setDisplayName((String) t.get("displayName"));
                transition.setSource(states.get(t.get("source")));
                transition.setTarget(target);

                if (!(target == null
                        || !to.equals(target.getName())
                        || "New".equals(transition.getSource().getName()))) {
                    transitions.put(name, transition);
                }
            }
        }

        return transitions;
    }

    public String getStateDisplayName(String workflowState) {
        return getStates()
                .stream()
                .filter(st -> st != null && workflowState.equals(st.getName()))
                .map(WorkflowState::getDisplayName)
                .findFirst()
                .orElse(workflowState);
    }

    @Override
    protected void beforeSave() {
        super.beforeSave();

        siteContentTypeIds = new LinkedHashSet<>();

        for (Site site : getSites()) {
            for (ObjectType contentType : getContentTypes()) {
                siteContentTypeIds.add(site.getId() + ":" + contentType.getId());
            }
        }
    }

    public static Workflow findWorkflow(Site site, State state) {

        if (state == null) {
            return null;
        }

        Workflow workflow = null;
        ObjectType type = state.getType();

        if (site != null) {
            workflow = Query
                    .from(Workflow.class)
                    .and("sites = ?", site)
                    .and("contentTypes = ?", type)
                    .first();
        }

        if (workflow == null) {
            workflow = Query
                    .from(Workflow.class)
                    .and("sites = missing")
                    .and("contentTypes = ?", type)
                    .first();
        }

        if (workflow == null) {
            Site owner = state.as(Site.ObjectModification.class).getOwner();
            if (owner != null) {
                workflow = Query
                        .from(Workflow.class)
                        .and("sites = ?", owner)
                        .and("contentTypes = ?", type)
                        .first();
            }
        }

        return workflow;
    }

    @FieldInternalNamePrefix("cms.workflow.")
    public static class Data extends Modification<Object> implements VisibilityLabel, VisibilityValues {

        @Indexed(visibility = true)
        @ToolUi.Hidden
        private String currentState;

        @Embedded
        @ToolUi.Hidden
        private WorkflowLog currentLog;

        public String getCurrentState() {
            return currentState;
        }

        public WorkflowLog getCurrentLog() {
            return currentLog;
        }

        public void setCurrentLog(WorkflowLog currentLog) {
            this.currentLog = currentLog;
        }

        /**
         * @param transition If {@code null}, makes the object visible.
         * @param user May be {@code null}.
         * @param log May be {@code null}.
         * @return New workflow state. May be {@code null}.
         */
        public String changeState(WorkflowTransition transition, Object user, WorkflowLog log) {
            String previousState = currentState;
            String transitionName;
            String transitionTarget;

            if (transition == null) {
                transitionName = "Publish";
                transitionTarget = null;

            } else {
                transitionName = transition.getName();
                transitionTarget = transition.getTarget().getName();

                if ("Published".equals(transitionTarget)) {
                    transitionTarget = null;

                } else if ("New".equals(transitionTarget)) {
                    transitionTarget = null;
                    as(Content.ObjectModification.class).setDraft(true);
                }
            }

            currentState = transitionTarget;

            if (transition != null || previousState != null) {
                if (log == null) {
                    log = new WorkflowLog();
                }

                log.setObject(getOriginalObject());
                log.setDate(new Date());
                log.setTransition(transitionName);
                log.setOldWorkflowState(previousState);
                log.setNewWorkflowState(currentState);

                if (user != null) {
                    if (user instanceof Recordable) {
                        log.setUserId(((Recordable) user).getState().getId().toString());
                    } else {
                        log.setUserId(user.toString());
                    }
                }

                log.save();
            }

            return currentState;
        }

        /**
         * @param transition If {@code null}, makes the object visible.
         * @param user May be {@code null}.
         * @param comment May be {@code null}.
         * @return New workflow state. May be {@code null}.
         * @deprecated Use {@link #changeState(WorkflowTransition, Object, WorkflowLog)} instead.
         */
        @Deprecated
        public String changeState(WorkflowTransition transition, Object user, String comment) {
            WorkflowLog log = new WorkflowLog();

            log.setComment(comment);

            return changeState(transition, user, log);
        }

        /**
         * @param state If {@code null}, makes the object visible.
         */
        public void revertState(String state) {
            this.currentState = state;
        }

        // --- VisibilityLabel support ---

        @Override
        public String createVisibilityLabel(ObjectField field) {
            String currentState = getCurrentState();

            if (currentState != null) {
                Workflow workflow = findWorkflow(as(Site.ObjectModification.class).getOwner(), getState());

                if (workflow != null) {
                    for (WorkflowState s : workflow.getStates()) {
                        if (currentState.equals(s.getName())) {
                            return s.getDisplayName();
                        }
                    }
                }
            }

            return currentState;
        }

        @Override
        public Iterable<?> findVisibilityValues(ObjectIndex index) {
            Set<Object> visibilityValues = new HashSet<Object>();

            for (Workflow workflow : Query.from(Workflow.class).where("contentTypes = ?", State.getInstance(getOriginalObject()).getType()).selectAll()) {
                visibilityValues.add(workflow.getStates());
            }
            return visibilityValues;
        }
    }
}
