Publishing with a Named Schedule
---------------------------------

You can schedule multiple revisions for simultaneous publication with a named schedule. This feature allows you to implement broad changes to the content of your site for a specific event, like a holiday, major news story, or product launch.

A named schedule is an object with a trigger publication date. When a named schedule is launched, the Dashboard operates in event-schedulng mode. Any revision that you publish while the Dashboard is in this mode becomes a scheduled event associated with the named schedule object.



**To create a named schedule:**

#. In the :doc:`../dashboard/schedule` widget on the main Dashboard page, click **New**.

#. Set the **New Schedule** options.

   The **Trigger Date** is the date when all events associated with this schedule are published.
 
   .. image:: ./images/pubnewschedule.png
      :width: 348px
      :height: 221px


   Saving the new schedule immediately puts the Dashboard into event-scheduling mode, as indicated by the message bar at the top of the Dashboard.

   .. image:: ./images/pubmessagebar.png
      :width: 658px
      :height: 60px



**To schedule publish events:**

#. If the Dashboard is not running in event-scheduling mode, click **View All** in the Scheduled Event widget.

   You are prompted to select a schedule.

   .. image:: ./images/pubschedules.png
      :width: 350px
      :height: 132px


#. Click the named schedule that you want to start.

   This action puts the Dashboard into event-scheduling mode.
    

#. Create a new instance of a content type, or edit an existing revision.

   On the Content Edit pane, the Publish widget includes a **Schedule** button for adding publish events to the currently running named schedule.
 

#. Add or modify content and click the **Schedule** button.
   
   A new scheduled draft is created, as indicated in the Revisions widget.

#. Return to the main Dashboard page. 
   
   The new event appears in the Scheduled Event widget.


#. Continue publishing content as described in the previous steps.

   The Scheduled Event widget reflects all of the events scheduled for publication for the named schedule.

   .. image:: ./images/pubscheduledevents.png
      :width: 389px
      :height: 452px

#. To stop publishing with the named schedule, click **Stop Scheduling** in the message bar at the top of the Dashboard.


**See also:**
:doc:`../versioning/all`






