# Visual Studio Team Services Plugin for IntelliJ

An IntelliJ plugin for working with Git repositories on Visual Studio Team Services and Team Foundation Server (TFS) 2015 inside IntelliJ.
Supported on Linux, Mac OS X, and Windows.
Compatible with IntelliJ IDEA Community and Ultimate editions (version 14) and Android Studio (version 1.2 - 1.4).

To learn more about installing and using our Team Services IntelliJ plug-in, visit: http://java.visualstudio.com/Docs/tools/intelliJ

## Pre-Reqs
1. Install JDK 8
  * You can find the JDK 8 download on Oracle's web site at <a href="http://www.oracle.com/technetwork/java/javase/downloads" target="_blank">Java SE Downloads</a>.
1. Set JAVA_HOME to the location of JDK 8
1. Install IntelliJ IDEA Community Edition version 16.x

## Create (or Update) the Gradle Properties file
We use Gradle as the build tool for this project.  Before you get started, you will need to either create a `gradle.properties` 
file in your user profile directory or update the file found in the IntelliJ repository root folder.

From a terminal/console window,
1. To create a `gradle.properties` file in your user profile directory, cd into your user profile directory.

1. Use your favorite text editor to create the file.

1. If you are updating the file in the IntelliJ repository, simply open that file in your favorite text editor.

1. Add or update the values of the properties as shown below. (the idea property needs to be set, but is only used for running integration tests)

  * Sample property file on Linux (TODO update these)
  ```
  ideaSdk=/home/user/idea-IC-161.2735.5/lib
  git4idea=/home/user/idea-IC-161.2735.5/plugins/git4idea/lib
  ```
  * Sample property file on Mac (TODO verify these)
  ```
  ideaSdk=/Applications/IntelliJ IDEA 16 CE.app/Contents/lib
  git4idea=/Applications/IntelliJ\ IDEA\ 16\ CE.app/Contents/plugins/git4idea/lib
  idea=/usr/github/intellij-community/bin
  ```
  * Sample property file on Windows
  ```
  ideaSdk=C:\\Program Files (x86)\\JetBrains\\IntelliJ IDEA Community Edition 2016.2.3\\lib
  git4idea=C:\\Program Files (x86)\\JetBrains\\IntelliJ IDEA Community Edition 2016.2.3\\plugins\\git4idea\\lib
  idea=D:\\github\\intellij-community\\bin
  ```

## Build with Gradle
Once your `gradle.properties` file has been updated, run the build.

1. From a terminal/console window, change to the root folder of the IntelliJ repository and then run `./gradlew zip`.
  * If you have multiple versions of the Java JDK installed, you may need to set your `JAVA_HOME` environment variable to the installation folder of the 1.6 JDK.
1. The plugin zip file will be created in the `plugin.idea/build/distributions/` folder.


## Build and Run with IntelliJ
1. Before you can build and run with IntelliJ, you must run `gradlew` to download the project dependencies.  Open a terminal/console window 
and run `./gradlew copyDependencies` from the root directory of the IntelliJ repository.  This will download all of the necessary build dependencies to your local disk.

1. After starting IntelliJ, open the existing IntelliJ project file `com.microsoft.alm.plugin.idea.iml` from the `plugin.idea` directory.

1. Configure a "IntelliJ Platform Plugin SDK" based on JDK 6 and IntelliJ 14.x.
 * File -> Project Structure -> Project Settings -> Project
 * Under Project SDK, select the entry marked "IntelliJ IDEA Community Edition".
    * If this entry does not exist, click New -> IntelliJ Platform Plugin SDK and select the IntelliJ installation location folder.
       * Under Mac, the folder is similar to `/Applications/IntelliJ IDEA 14 CE.app/Contents`
       * Under Linux, the folder is similar to `/home/<user>/idea-IC-141.2735.5` 
       * Under Windows, the folder is similar to `C:\Program Files (x86)\JetBrains\IntelliJ IDEA Community Edition 14.1.5`
 
1. Configure the project and ***each module*** to build with this "IntelliJ Platform Plugin SDK".
 * File -> Project Structure -> Project Settings -> Project.
    * Under Project SDK, select the entry marked "IntelliJ IDEA Community Edition".
 * File -> Project Structure -> Project Settings -> Modules -> Dependencies.
    * For each module beginning with `com.microsoft.alm` (there are three), under Module SDK, select the entry marked "IntelliJ IDEA Community Edition". 

1. Add git4idea.jar lib to `com.microsoft.alm.plugin.idea` module if it doesn't already exist.
  * File -> Project Structure -> Project Settings -> Libraries -> Add new Java library
  * Select `<IntelliJ community edition location on disk>/plugins/git4idea/lib/git4idea.jar`, name it `git4idea`
  * Add this git4idea lib to the `com.microsoft.alm.plugin.idea` module
    * If there is an existing git4idea dependency that is missing (shown in red), remove it from the list
  * IMPORTANT: you ***must*** make sure the scope for `git4idea` is changed from `Compile` to `Provided`
    * Project Settings -> Modules -> com.microsoft.alm.plugin.idea -> Dependencies.  Change `git4idea` to `Provided`

1. Make sure the 'GUI designer' generates Java source code.
  * File -> Settings (on Windows) / Preferences (on Mac) -> Editor -> GUI Designer -> Generate GUI into -> Select `Java Souce Code`

1. Create a "Plugin" configuration to run/debug the code.
  * Run -> Edit Configurations... -> Add a "Plugin" configuration 
  * Provide a name for the configuration (e.g., IntelliJ for TFS)
  * Set "Use classpath of module" to `com.microsoft.alm.plugin.idea`

1. To debug the plugin, click Run -> Debug.

## Contributing

We welcome Pull Requests, please fork this repo and send us your contributions.

Note: This repo is mirrored so any branches created in this repo will be removed.  Please fork.

A few styles we follow:

1. All Java source files must have the following two lines at the top:
 ```
 // Copyright (c) Microsoft. All rights reserved.
 // Licensed under the MIT license. See License.txt in the project root.
 ```
  * Settings -> Editor -> Copyright -> Copyright Profiles -> Create a new "Copyright Profile" with those two lines
  * Assign this profile to the current project

1. No tabs in source code:
  * Settings -> Editor -> Code Style -> Java -> Tabs and Indents -> Uncheck "Use tab character", set "Tab size" and "Indent" to 4

1. No * imports
  * Settings -> Editor -> Code Style -> Java -> Imports -> Set "Class count to use import with '\*'" and "Names count to use static import with '\*'" to sufficient large numbers such as 100
  * On the same page remove `java.awt.*` and `javax.swing.*` from the "Package to Use Import with '*'" list

Those settings are already configured in the `com.microsoft.alm.plugin.idea.iml` project file we provided.  

Gradle build will fail if checkstyle plugin detects a violation.

## Running Integration Tests (L2 tests)

Our Integration tests are in the L2Tests folder. In order to run them correctly, you have to set up the environment and have a VSTS account setup to run against.

Here are the steps to setup your environment:

1. Before you start, you will need to download and build the IntelliJ Community Edition version 2016.
  * You can find the instructions here: https://github.com/JetBrains/intellij-community
  
1. First setup the gradle.properties file. There is an example above. 
  * Specify the location of the git4idea plugin via the `git4idea` property (this can be found in the installation folder of the IntelliJ 16 Community Edition)
  * Specify the location of the IDEA lib folder via the 'ideaSdk' property (this can be found in the installation folder of the IntelliJ 16 Community Edition) 
  * Specify the location of the IDEA bin folder via the `idea` property (this is the bin folder of the IntelliJ github repository that you downloaded)
  
1. Second setup the environment variables that provide the connection information for the tests. If this information is missing the tests will fail with a message that describes the missing information. The values below are examples but you will have to fix them.
  * MSVSTS_INTELLIJ_RUN_L2_TESTS=true
  * MSVSTS_INTELLIJ_TF_EXE=d:\bin\TEE-CLC-14.0.4\tf.cmd
  * MSVSTS_INTELLIJ_VSO_GIT_REPO_URL=https://account.visualstudio.com/_git/projectName
  * MSVSTS_INTELLIJ_VSO_LEGACY_GIT_REPO_URL=https://account.visualstudio.com/defaultcollection/_git/projectName
  * MSVSTS_INTELLIJ_VSO_PASS=PersonalAccessTokenGeneratedFromTheUserSecurityPage
  * MSVSTS_INTELLIJ_VSO_SERVER_URL=https://account.visualstudio.com
  * MSVSTS_INTELLIJ_VSO_TEAM_PROJECT=projectName
  * MSVSTS_INTELLIJ_VSO_USER=EmailAddressForUser

1. Last you probably want to setup a Run Configuration for the L2 Tests inside IntelliJ
  * Create a new JUnit run configuration with the following settings
  * Set VM options to `-ea -Xmx512M -Didea.config.path=..\test-config -Didea.system.path=..\test-system -Didea.test.group=ALL_EXCLUDE_DEFINED`
  * Set the working directory to `D:\github\intellij-community\bin` i.e. the path to the bin folder in your IntelliJ github repository
  * Use classpath of module `L2Tests`

1. Other things to note:
  * You can toggle whether the tests will run or not simply by changing the MSVSTS_INTELLIJ_RUN_L2_TESTS environment variable.
  * The internal CI build will run these tests

## Learn More

Want more information? The following resources are available to help:

* <a href="http://java.visualstudio.com/Downloads/intellijplugin/Index" target="_blank">Instructions</a> on how to install the plugin
* <a href="https://youtu.be/wSdgmQL-Zbg" target="_blank">End-to-end demo</a> video of the plugin's features
* <a href="http://java.visualstudio.com/Docs/tools/intelliJ" target="_blank">Documentation and tutorial</a> on how to use the plugin 

