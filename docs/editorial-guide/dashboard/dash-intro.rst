==========================================
Custom Dashboards
==========================================

The CMS provides a default dashboard. Set at the global level of the system, the default dashboard is available to all users of the system.

If you have the required permission, you can modify the global dashboard as an alternative to the system default dashboard. The customized result becomes the new global dashboard. In addition to the global dashboard, you can create a dashboard at a different system level, which determines the user scope of the dashboard.

--------------------------
Dashboard Scoping
--------------------------

A custom dashboard is scoped to one of the following system levels, ranging from global to personal. If a dashboard is set on more than one system level, then an order-of-precedence scheme is used to determine which dashboard is made available to the user.

- The global dashboard, which is the dashboard that every user sees by default if no other dashboard is set. From the system menu, go to **Admin -> Settings -> Dashboard**.

- The site dashboard, which is the dashboard that only users of a specific site see. It takes precedence over the global dashboard. From the system menu, go to **Admin -> Site -> Dashboard**.

- The role dashboard, which is the dashboard that only users in a certain role see by default. A role dashboard takes precedence over a site or global dashboard. From the system menu, go to **Admin -> Users & Roles -> Dashboard**.

- The personal dashboard, which is a dashboard that only the account user sees. A personal dashboard has the highest precedence. In the Profile page, select **Dashboard**.

.. _db-config-anchor:

-------------------------------
Dashboard Configuration Options
-------------------------------

The dashboard scoping determines the availability of dashboard configuration options. All options are available at the global level:

- Type – the type of dashboard, either shared (available to all users) or one off (limited for use by the creator only). To configure, see :doc:`dash-oneoff` or :doc:`dash-shared`.

- Layout – the selection of widgets and their arrangement on the dashboard.

- Resources – the configuration of the :doc:`resources` widget.

- Common Content – the configuration of the :doc:`common-content` widget. You can set the content types for which to create new instances, and you can set existing instances of content types to access from this widget. 

- Bulk Upload – the configuration of the :doc:`bulk-upload` widget.

The other system levels exclude some configuration options. The following table shows the dashboard configuration options available at each system level.


+-------------------------+----------------------------------------------+ 
| Dashboard               |                System Scope                  |
| Configuration Option    |                                              | 
+=========================+============+===========+==========+==========+
|                         | **Global** | **Site**  | **Role** | **User** |
+-------------------------+------------+-----------+----------+----------+
| Widget type and layout  | x          | \-        | x        | x        | 
+------------+------------+------------+-----------+----------+----------+ 
| Resources widget        | x          | x         | \-       | \-       | 
+------------+------------+------------+-----------+----------+----------+ 
| Common Content widget:  | |          | |         |  |       | |        |
|  | New instances        | | x        | | x       |  | x     | | \-     |
|  | Existing instances   | | x        | | x       |  | x     | | x      | 
+-------------------------+------------+-----------+----------+----------+
| Bulk Upload widget      | x          | x         | \-       | \-       | 
+-------------------------+------------+-----------+----------+----------+

The system applies the order-of-precedence scheme described above to build a user’s dashboard. That is, if the same configuration option is set on multiple dashboards, then the system uses the setting on the dashboard at the highest level of precedence. If a configuration option is not available on the dashboard of highest precedence, then the configuration setting on the dashboard of next highest precedence is used.

The following table provides an example of the system logic applied when two dashboards are configured, at the global and site levels. Because the widget–type-and-layout option is not available at the site level, the setting at the global level is used by default. Where there are settings at both the global and site levels, the site settings override the global settings. 

+-------------------------+------------------------+--------------+ 
| Dashboard               |       Settings         | Setting Used |
| Configuration Option    |                        |              | 
+=========================+============+===========+==========+===+
|                         | **Global** | **Site**  |              |
+-------------------------+------------+-----------+----------+---+
| Widget type and layout  | set        | n/a       | global       | 
+------------+------------+------------+-----------+----------+---+ 
| Resources widget        | set        | set       | site         | 
+------------+------------+------------+-----------+----------+---+ 
| Common Content widget:  | |          | |         |  |           |
|  | New instances        | | set      | | set     |  | site      |
|  | Existing instances   | | set      | | set     |  | site      | 
+-------------------------+------------+-----------+----------+---+
| Bulk Upload widget      | set        | not set   | global       | 
+-------------------------+------------+-----------+----------+---+
\

| **See also:**
| :doc:`../versioning/all`

.. toctree:: 
   :hidden:		

    dash-oneoff
    dash-shared
    dash-editpage
   





