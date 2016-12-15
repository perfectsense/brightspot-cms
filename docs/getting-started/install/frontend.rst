=======================================
Frontend
=======================================

.. Can't get enumerated sublists to work. It worked in previous enviornments.

If you only need to focus on client-side development, you can set up a frontend development environment that excludes the Brightspot server and other backend components. The Styleguide development platform is included for creating, testing, and validating frontends.

You can use either Git or Maven to create the Brightspot project. Use Maven if no Git repository exists.


**Prerequisites**

- To use Maven to create the Brightspot project, |mv_link| and install Maven.

.. |mv_link| raw:: html

    <a href="http://maven.apache.org/download.cgi" target="_blank">download</a>

- |node_link| and install node.js.

.. |node_link| raw:: html

    <a href="https://nodejs.org/en/download/" target="_blank">Download</a>


**To set up a frontend development environment:**


1. Create the Brightspot project using either Git or Maven.

   **To use Git:**

   a. Clone the Brightspot repository on your local drive.

   b. Navigate to the top-level folder of the repository where the pom.xml file resides. This file defines Brightspot and Dari dependencies.

   **To use Maven:**

   a. Create a starter Brightspot project.

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
   b. Navigate to the top-level folder of the Maven project where the pom.xml file resides. 
   This file defines Brightspot and Dari dependencies.

2. Build the Brightspot project with npm:

   ::
   
     npm install && npm run


   This generates a target directory that includes the Styleguide developer platform.

3. Run Styleguide:
   
   ::

     npm run styleguide

   The Styleguide server starts up.
      
   .. image:: images/sg_startup.png

   
4. To access the Styleguide Development Tool, specify ``http://localhost:3000`` in a web browser.


