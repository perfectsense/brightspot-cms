.. raw:: html

    <style> .red {color:red}</style>


================================
Previewing Content
================================


For content types associated with a view, the Preview feature shows you how your content will look on the live web page. Changes that you make in the Content Edit page are reflected instantly in the **Preview** window. 


In the Content Edit page, click the **Preview** link in the Publish widget. The **Preview** window appears.

.. image:: ./images/pubpreview.png
      :width: 721px
      :height: 455px

You can perform the following actions with the **Preview** window controls:

- **Pin** shrinks the live preview and places it to the right of the Content Edit page	. To re-expand the Preview pane, click **Preview**.

- **Share** allows you to share the preview with others. Click **Share** to open a preview of the unpublished content in a new window and generate a temporary URL. Send the URL to those with whom you want to share the preview page.

- **Resolution Picker** allows you to preview your content for a variety of device resolutions.


- **View Picker** applies to a content type that you are previewing that is referenced by different content types. For example, there can be a Section content type that is being previewed, which is referenced by a Story content type. If you have multiple Story content items, such as StoryX and StoryY, the current versions of StoryX and StoryY reference the current Section instance. 

  The View Picker allows you to select a content item, for example, StoryX or StoryY that references the content type that you are previewing. The previewed web page reflects the content item selected in the View Picker.

  As show in the following example, several content items of type Story can be selected in the View Picker. The currently published version of the Story content item is reflected in the previewed page.

  .. image:: ./images/pubviewpicker.png
      :width: 509px
      :height: 200px

  Note that the content types listed in the View Picker are also listed in the :doc:`References widget <../publishing-tools/references>` on the Content Edit page.

- **Date Picker** works in conjunction with the View Picker. Whereas the View Picker applies to different content items (StoryX or StoryY) that reference another content type (Section), the Date picker applies to different versions of the content item that is selected in the View Picker, such as StoryX_v1 or StoryX_v2. For the content item selected in the View Picker, the Date Picker allows you to select a date when a new version of the content item is scheduled to be published.

  For example, say that you have a Section content instance that is referenced by a StoryX_v1 version, which is the currently published version for StoryX. In addition, you have scheduled a StoryX_v2 version for future publication, which references the same Section content instance as StoryX_v1. If you preview the Section instance for the current day, the previewed web page reflects the StoryX_v1 version. If you use the Date Picker to select the publication date of the StoryX_v2 version, the previewed web page reflects the StoryX_v2 version.


In addition to the above controls, some Preview windows include a read-only URL field with the URL of the live web page. The URL is set with the :doc:`URLs widget <../publishing-tools/urls>`.
	

| **See also:**
| :doc:`../versioning/all`




  