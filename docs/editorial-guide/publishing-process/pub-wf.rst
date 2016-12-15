.. raw:: html

    <style> .red {color:red}</style>


================================
Using Workflows
================================

With the Workflow tool, you can attach a custom workflow approval process to any content type in Brightspot. Content associated with a workflow is published as the final step in the workflow. Depending on how user roles are defined, workflow users can participate in some or all of the steps in a workflow. Typically, only a subset of the workflow participants belong to a role with permission to publish the content.

As a content item is routed through a workflow, a separate draft version is created for each workflow state (for example, submitted, rejected, resubmitted).  With each completed step in the workflow, the status of the workflow is recorded in the Revisions widget.


.. image:: ./images/pubrevisionswidget3.png
      :width: 324px
      :height: 446px


A workflow can be a simple process, such as contributors submitting content for one level of review before the content is published. Conversely, you can define a complex workflow that routes content in multiple directions, from department to department, and through various levels of editorial and legal approval. Simple or complex, a workflow consists of three general phases: launch, transitioning between states, and publish.


**To launch a workflow:**

#. Create an instance of a content type associated with a workflow.

#. In the comment and editors fields, enter a message to a specified user or group who will participate in the next step of the workflow. 

#. Click the workflow button to launch the workflow to the first step.

In the following example, clicking the **Submit for Approval** button sends the workflow to users defined in a role called "Editorial Group". Note that only workflow options appear on the Publish widget. This indicates that the user launching the workflow does not have the right to publish content of the type associated with the workflow. 


.. image:: ./images/pubpublishwidget.png
      :width: 326px
      :height: 263px


**To transition a workflow:**

- Click the workflow button that advances the workflow to the desired state.

In the following example, the editorial user could send the workflow back to the initiating user by clicking the **Send Back for Edits** button. Note that the **Publish** tab appears on the widget, indicating that the editorial user has the publication right for the content type associated with the workflow. At this state in the workflow, the editorial user could, alternatively, publish the content. 

.. image:: ./images/pubpublishwidget2.png
      :width: 325px
      :height: 391px


**To publish the workflow content:**

- On the **Publish** tab, either click the **Publish** button or schedule publication for a later time.


| **See also:**
| :doc:`../workflows/all`
| :doc:`../versioning/all`


  