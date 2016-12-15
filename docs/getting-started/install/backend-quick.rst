=======================================
Backend: Quick Setup
=======================================

Backend development requires that you use Maven to build and package the Brightspot platform into a WAR and deploy it. 
The quick setup uses the Cargo plugin. At the end of this procedure, the Brightspot platform will be automatically deployed to and running in the Tomcat application server.


**Prerequisites**

Download and install the following software:

- |java_link|

.. |java_link| raw:: html

     <a href="http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html" target="_blank">Java 8 JDK</a>

- |mv_link|

.. |mv_link| raw:: html

    <a href="http://maven.apache.org/download.cgi" target="_blank">Maven 3+</a>

**To build and deploy Brightspot:**

1. Create a starter Brightspot project:

   Run the following archetype on the command line:

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
\
   .. note:: Windows users must run the archetype on one line without breaks (\\), for example:
             
      | ``mvn archetype:generate -B -DarchetypeRepository=http://artifactory.psdops.com/public/ -DarchetypeGroupId=com.psddev -DarchetypeArtifactId=cms-app-archetype -DarchetypeVersion=<snapshotVer> -DgroupId=<groupId> -DartifactId=<artificatId>``
\
   
2. | Navigate to the top-level folder of the Maven project where the pom.xml file resides. 
   | This file defines Brightspot and Dari dependencies.

3. Verify that no applications are running on port 9480 and that MySQL in not running.


4. Build and deploy the Brightspot project:

   ::
   
     mvn -P run clean package cargo:run


   Tomcat is started in the Cargo container.

5. | In a web browser, access Brightspot at ``http://localhost:9480/cms``.

   
   The Brightspot login page appears.

   .. image:: images/bs_login.png

**To stop the Cargo container:**

- From the terminal window, press ``Ctrl-C``.

**To restart the Cargo container:**

- From the top-level folder of the Maven project, rerun the following command:

  ::

    mvn -P run clean package cargo:run


