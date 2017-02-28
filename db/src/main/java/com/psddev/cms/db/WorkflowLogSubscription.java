package com.psddev.cms.db;

import com.psddev.cms.notification.Receiver;
import com.psddev.cms.notification.Subscription;
import com.psddev.dari.db.Recordable;

@Recordable.DisplayName("Workflow Subscription")
public class WorkflowLogSubscription extends Subscription<WorkflowLog> {

    @Override
    protected String getDeliveryOptionsLabel() {
        return "Sends " + getDeliveryOptionTypesLabel() + " notification when you're assigned to content moving through a workflow.";
    }

    @Override
    protected String toStringFormat(Receiver receiver, WorkflowLog workflowLog) {

        Recordable object = (Recordable) workflowLog.getObject();
        String objectLabel = object.getState().getLabel();
        String objectTypeLabel = object.getState().getType().getLabel();

        String oldWorkflowState = workflowLog.getOldWorkflowState();
        String newWorkflowState = workflowLog.getNewWorkflowState();
        String transition = workflowLog.getTransition();
        String comment = workflowLog.getComment();

        String message = "Workflow: "
                + objectTypeLabel
                + " - "
                + object.getState().getId()
                + " - "
                + objectLabel
                + " | Old State: "
                + oldWorkflowState
                + " | Transition: "
                + transition
                + " | New State: "
                + newWorkflowState
                + " | Comment: "
                + comment;

        return message;
    }
}
