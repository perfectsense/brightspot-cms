.. raw:: html

    <style> .red {color:red}</style>


================================
Publishing to a Live Site
================================

Using the Revisions and Publish widgets on the Content Edit page, you can publish new content that has not been previously published. You can also republish, that is, replace live content with a new version. The live version of the content is  updated immediately to reflect your changes.

A published version, live or past, cannot be moved back to draft status. Published revisions can be renamed or saved as new drafts. They cannot be modified or deleted.

The Revision widget lists the current live version. Under a section labeled **Past**, the Revisions widget lists past revisions that were previously published to a live site. It also lists any initial draft or workflow versions, which reflect an intermediate version of the content prior to publishing. 

.. image:: ./images/pubrevisionswidget4.png
      :width: 250px
      :height: 407px


You can publish any draft version or republish any previous live version listed in the Revisions widget. You can also make changes to the live version and republish it immediately, without first saving the changed live version as a draft.

You can publish a version immediately, or you can schedule it for a future publication date. Note that you can separately schedule publication for publishable content types that are referenced by a containing content type. For example, if Story type has a header field that references an instance of Header content type, the Header content type can separtely be updated and scheduled for publication, say in two weeks. If an updated Story type instance goes live in one week, its header will reflect the currently published Header instance. However, in two weeks, the header for the Story will automatically be updated, reflecting the newly published Header instance.

You can schedule a revision for publication in two ways. One way is to directly set a publication date on a single revision. Another way is to set a publication date on a named schedule, and then associate multiple revisions with the named schedule. The result is that the multiple revisions will be published simultaneously on the same date. 
See :doc:`pub-live-group`.

A revision scheduled for publication is a scheduled published event, and such events are tracked in the :doc:`../dashboard/schedule` widget on the main Dashboard page.


**To publish a revision now:**

1. Create a new instance of a content type, or edit an existing revision.

2. On the Publish widget, click **Publish**.


   .. image:: ./images/pubpublishwidget3.png
      :width: 310px
      :height: 272px


**To schedule publication for a single revision:**

1. Create a new instance of a content type, or edit an existing revision.

2. Click the calendar icon.

3. Select a date and time for the content to be published to the site and click **Set**.

   .. image:: ./images/pubscheduleaction.png
      :width: 297px
      :height: 355px

   The Publish button now reads **Schedule**.

4. Click the **Schedule** button.
   
   The Publish widget reflects that the version is scheduled for publication.


   .. image:: ./images/pubscheduledscreen.png
      :width: 244px
      :height: 272px

\
   .. note:: If you click **Delete Draft**, the version is unscheduled and deleted permanently. To unschedule a draft without losing your content, click **Unschedule**.



| **See also:**
| :doc:`../versioning/all`


.. toctree:: 
   :hidden:
    
   pub-live-group

.. In the current build system, this sub-toctree is ignored.

