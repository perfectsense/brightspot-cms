.. Merged with content from Ryan's development.rst. Dev may prefer to call this "Development Installation". If that's the case, does that mean that the quick install is not a development install?

=======================================
Manual Installation
=======================================

This procedure requires that you manually install and configure the software prerequisites, use Maven to build and package the Brightspot platform into a WAR, and deploy it in the Tomcat application server. 

**Prerequisites**

Download and install the following software:

- |java_link|

.. |java_link| raw:: html

     <a href="http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html" target="_blank">Java 8 JDK</a>

- |mv_link|

.. |mv_link| raw:: html

     <a href="http://maven.apache.org/download.cgi" target="_blank">Maven 3+</a>

- |sql_link|

.. |sql_link| raw:: html

     <a href="http://dev.mysql.com/downloads/" target="_blank">MySQL 5.6.*</a>

- |solr_link|

.. |solr_link| raw:: html

     <a href="http://lucene.apache.org/solr/downloads.html" target="_blank">Solr 4.8.1</a>

- |tc_link|

.. |tc_link| raw:: html

     <a href="https://tomcat.apache.org/download-80.cgi" target="_blank">Tomcat 8</a>

\

**To set up a development environment:**

1. Create an empty database in :ref:`MySQL <mysql-label>`. 

2. Configure :ref:`Tomcat <tc-label>` to run the Brightspot platform.

3. Install :ref:`Solr <solr-label>` into Tomcat.

4. :ref:`Build <build-label>` a Brightspot project with Maven.

5. :ref:`Start <start-label>` the application server.

.. _mysql-label:

MySQL
-----

Run MySQL locally, and create an empty database to be used by the Brightspot platform. You can perform MySql operations from an optional GUI tool such as MySQLWorkbench. Alternatively, you can use the MySQL command-line tool.

You can give the database any name. The following command creates a database called "brightspot":

::
  
  CREATE DATABASE brightspot CHARACTER SET utf8 COLLATE utf8_general_ci;

Record the database name; you will specify it in the Tomcat context.xml file.

.. _tc-label: 

Tomcat
-------

**Configure Tomcat to run Brightspot projects:**

1. Add MySQL connector.

   |conn_link| the MySQL Connector JAR file for Tomcat and place it in the Tomcat ``lib`` folder. For example:

   .. |conn_link| raw:: html

    <a href="http://dev.mysql.com/downloads/connector/j/" target="_blank">Download</a>

   ::

      cp mysql-connector-java-5.*.jar <TomcatRoot>/lib

2. Add a local storage directory.

   Brightspot can store uploaded files locally in a ``media`` directory. Create this directory in the Tomcat webapps directory. For example:

   ::
      
       mkdir <TomcatRoot>/webapps/media

3. Replace the default context.xml file in Tomcat with a new file containing the default Brightspot configurations:

   #. In the Tomcat ``conf`` folder, make a copy of the default context.xml file and rename it.
   
   #. Create a new context.xml in the Tomcat ``conf`` folder.
   
   #. Open the |context_link| and copy the contents.

      .. |context_link| raw:: html

        <a href="sampleContext.html" target="_blank">sample context.xml file</a>

   #. Paste the contents into the new context.xml file in the Tomcat ``conf`` folder.


.. todo:: How best to give user a reference copy of context.xml?

    1) In the original documentation, there was an issue going to this public Git site:  <a href="https://gist.githubusercontent.com/kphenix/54ca0f473ef7e034811a/raw/29acee59ecc2e431cd1bfc46a4bcb049c52e1e8d/default-context-2.4.xml" target="_blank">Brightspot context.xml file</a>. (I was told not to use this.)

    2) It was suggested that I put a copy of context.xml in the PSD docs repo, but this is a private site: <a href="https://github.com/perfectsense/docs/blob/master/brightspot/developers-guide/pages/Setup/sampleContext.xml" target="_blank">sample context.xml file</a>

    3) The method currently used is to use a rst-formatted version of context.xml in the source. It is then built into an html version that's accessed on the doc server. 

\

4. In context.xml, replace the following placeholders:

   
   | ``DATABASE_NAME`` with the name of the empty MySQL database that you previously created.
   | ``DATABASE_USER`` with the name of the user that created the MySQL database.
   | ``DATABASE_PASS`` with the password that created the MySQL database.
   | ``TOMCAT_PATH``  with the path to Tomcat.
\
   
.. note:: The context.xml file referenced in this topic is a basic version of the Brightspot configuration. However, you can expand context.xml for future projects, or use multiple context.xml files for multiple Brightspot projects. The recommended best practice is to run an instance of Tomcat for each Brightspot project. The context.xml file will contain project-specific settings and point to a project specific database. When running multiple projects locally, you can stop Tomcat or use a different port for each project to run them concurrently.

  For additional context.xml settings, see |adv_link|.

.. |adv_link| raw:: html

 <a href="http://documentation.brightspot.com/docs/3.0/advanced-configuration/tomcat" target="_blank">Advanced Configuration</a>

.. _solr-label:

Solr
------

Solr is used as a text matching database in the Brightspot platform. It contains the same data that is stored in the SQL database.


**Install Solr into Tomcat:**

1. Place the solr.war file in the Tomcat ``webapps`` directory, for example:

   ::
    
    cp <SolrRoot>/example/webapps/solr.war <TomcatRoot>/webapps

2. Copy the Solr database directory into the Tomcat root directory, for example:

   ::
   
    cp -r <SolrRoot>/example/solr <TomcatRoot>

3. Replace two Solr configuration files with Brightspot specific configurations.

   a) |dl_link| the Brightspot versions of the Solr config file and the Solr schema file.

      .. |dl_link| raw:: html

        <a href="https://github.com/perfectsense/dari/tree/master/etc/solr" target="_blank">Download</a>


   b) Rename the config file to "solrconfig.xml". Rename the schema file to "schema.xml".

   c) In the ``<TomcatRoot>/solr/collection1/conf`` folder, replace solrconfig.xml and schema.xml with the two Brightspot versions.

4. Edit the solr.xml file in the Tomcat ``solr`` folder: 

   Replace the default host post with the Tomcat port ``<int name="hostPort">${jetty.port:9480}</int>``.

   
5. Copy all of the files in the ``<SolrRoot>/example/lib/ext`` folder into the Tomcat ``lib`` directory, for example:

   ::

     cp <SolrRoot>/example/lib/ext/* <TomcatRoot>/lib


.. _build-label:

Build a Brightspot Project
------------------------------

You build a Brightspot project from a Maven archetype. The target of the Maven build is the Brightspot platform packaged in a WAR file and the Styleguide developer platform.

.. note::

    For information about Brightspot releases or upgrading to a new version, see the `Brightspot Releases <http://www.brightspot.com/docs/3.2/updates/about-brightspot-upgrades>`_.

1. Get the starter Brightspot project.

   You can use either Git or Maven to get the project. Use Maven if no Git repository exists.


   **To use Git:**

   a) |clone_link| the brightspot-cms repository on your local drive.

      .. |clone_link| raw:: html

        <a href="https://github.com/perfectsense/brightspot-cms" target="_blank">Clone</a>

   b) Navigate to the top-level folder of the repository where the pom.xml file resides. This file defines Brightspot and Dari dependencies.

   **To use Maven:**

   a) Run the following archetype on the command line:

      ::

       mvn archetype:generate -B \
       -DarchetypeRepository=http://artifactory.psdops.com/public/ \
       -DarchetypeGroupId=com.psddev \
       -DarchetypeArtifactId=cms-app-archetype \
       -DarchetypeVersion=<snapshotVer> \
       -DgroupId=<groupId> \
       -DartifactId=<artificatId>

      |   Replace:
      |   *snapshotVer* with the Brightspot build version, for example, ``3.2-SNAPSHOT``.
      |
      |   *groupId* with a value that will serve as a Java package name for any Brightspot classes that you might add. Maven will create a source directory structure based on the package name. For example, if you specify ``com.brightspot``, the Brightspot project will include this directory for adding Brightspot classes: ``src/main/java/com/brightspot``.
      |
      |   *artificatId* with a project name like ``brightspot``. This will be used for the top-level folder of the Brightspot project.

      .. note:: Windows users must run the archetype on one line without breaks (\\), for example:
             
       | ``mvn archetype:generate -B -DarchetypeRepository=http://artifactory.psdops.com/public/ -DarchetypeGroupId=com.psddev -DarchetypeArtifactId=cms-app-archetype -DarchetypeVersion=<snapshotVer> -DgroupId=<groupId> -DartifactId=<artificatId>``

   
   b) Navigate to the top-level folder of the Maven project where the pom.xml file resides. This file defines Brightspot and Dari dependencies.


2. Build the Brightspot project with Maven:

   ::
   
     mvn clean package


   This generates a target directory with the Brightspot platform packaged in a WAR file.

3. Copy the generated WAR file from the target directory to the Tomcat ``webapps`` directory and rename it as desired. For example:

   ::

     cd brightspot/target
     cp brightspot-3.2-SNAPSHOT.war ../../<TomcatRoot>/webapps/bsPlatform.war


.. _start-label: 

Start the Application Server
---------------------------------

1. Navigate to the Tomcat root folder and start the application server:

   ::
    
     ./bin/startup.sh or ./bin/startup.bat
   
   Tomcat deploys the Brightspot platform. 

2. | In a web browser, access Brightspot at ``http://localhost:<port>/<contextPath>/cms``, where:
   | *port* is the port number that you specified in context.xml.
   | *contextPath* reflects the name of the WAR file, for example: ``http://localhost:8080/bsPlatform/cms``.

\    
 
.. note:: If the name of your WAR file is ROOT.war, then do not specify a context path, for example ``http://localhost:8080/cms``.


The Brightspot login page appears. This is the default landing page.

.. image:: images/bs_login.png

3. Follow up the Brightspot deployment with the following actions:

  
   - If Java heap size errors appear in the Tomcat logs, change the memory allocation in the Tomcat catalina.sh file, found at ``<TomcatRoot>/bin/catalina.sh``. Add the following line directly above the ``# OS specific support`` section:

     ::

       # ----- Adding more Memory
       CATALINA_OPTS="-Xmx1024m -XX:MaxPermSize=256M -Djava.awt.headless=true"


   - Insufficent free space warnings like the following might appear in the Tomcat logs:

     ::

       org.apache.catalina.webresources.Cache.getResource 
       Unable to add the resource at [/WEB-INF/lib/aws-java-sdk-workspaces.jar] to the cache because there was insufficient free space available after evicting expired cache entries - 
       consider increasing the maximum size of the cache

   
     To prevent these warnings, add the following setting to ``<TomcatRoot>/conf/context.xml``.

     ::

      <!-- Set caching allowed -->
      <Resources cachingAllowed="true" cacheMaxSize="100000" />


   - Check for new Brightspot versions with which to upgrade your development enviornment.
     To get the most current release of Brightspot, see :doc:`../../updates/about`.
