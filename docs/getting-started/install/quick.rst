******************
Quick Installation
******************

This option should be used for developers looking to experiment with Brightspot locally. The sole dependency for running this project is Java 8.

Step 1. Download and Install Java Development Kit (JDK)
=======================================================

The JDK is necessary to run Britghtspot locally.

Go to the `Java SE Downloads <http://www.oracle.com/technetwork/java/javase/downloads/index.html>`_ page and click **JDK**.

.. note::

    Ensure your PATH variable points to bin/java or bin/java.exe.

Step 2. Download
================

`Download the ZIP <https://github.com/perfectsense/brightspot-tutorial/archive/master.zip>`_ for the `Brightspot Tutorial <https://github.com/perfectsense/brightspot-tutorial>`_ project.

.. note::

    If you have **git** installed, you can alternatively ``git clone`` `the repository <https://github.com/perfectsense/brightspot-tutorial>`_.

Step 3. Extract
===============

If you've downloaded the ZIP from Step 2, extract the ZIP in a location of your choice.

Step 4. Run
===========

The Brightspot Demo project contains scripts for OSX/Linux and Windows environments.

**OSX / Linux**

From the root of the project, execute the ``run.sh`` script from your terminal:

::

    ./run.sh

**Windows**

From the root of the project, execute the ``run.cmd`` script from the Command Prompt:

::

    run.cmd

Once the script completes, the application will be running at ``http://localhost:9480``. To login to the Brightspot CMS, navigate to ``http://localhost:9480/cms``.
