# Visual Studio Team Services Plugin for IntelliJ, Android Studio, & other Jetbrains IDEs

This is a plugin for working with Git and TFVC repositories on Visual Studio Team Services (VSTS) and Team Foundation Server (TFS) 2015+ inside IntelliJ, Android Studio, 
and various other Jetbrains IDEs. It is supported on Linux, Mac OS X, and Windows.
It is compatible with IntelliJ IDEA Community and Ultimate editions (version 14.1.7+) and Android Studio (version 1.2+).

To learn more about installing and using our Team Services IntelliJ plug-in, visit: http://java.visualstudio.com/Docs/tools/intelliJ

## Pre-Reqs
1. Install JDK 8. 
   * You can find the JDK 8 download on Oracle's web site at <a href="http://www.oracle.com/technetwork/java/javase/downloads" target="_blank">Java SE Downloads</a>.
2. Set JAVA_HOME to the location of JDK 8.
3. Install IntelliJ IDEA Community Edition version 2017.x.
4. Clone the repository (if planning on building yourself).

## Create (or Update) the Gradle Properties file
We use Gradle as the build tool for this project.  Before you get started, you will need to either create a `gradle.properties` file in your Gradle user home directory or update the file found in the repository's root folder.

From a terminal/console window,
1. To create a `gradle.properties` file in your Gradle user home directory, cd to `USER_HOME/.gradle`
and use your favorite text editor to create the file.

1. If you are updating the file in the IntelliJ repository, simply open that file in your favorite text editor.

1. Add or update the values of the properties as shown below. The path and version should be changed to match the IDE installed on the machine.

   * Sample property file on Linux:
      ```
      ideaSdk=/home/user/idea-IC-161.2735.5/lib
      git4idea=/home/user/idea-IC-161.2735.5/plugins/git4idea/lib
      ``` 
   * Sample property file on Mac:
      ```
      ideaSdk=//Applications//IntelliJ IDEA.app//Contents//lib
      git4idea=//Applications//IntelliJ IDEA.app//Contents//plugins//git4idea//lib
      idea=//usr//github//intellij-community//bin
      ```
   * Sample property file on Windows:
      ```
      ideaSdk=C:\\Program Files (x86)\\JetBrains\\IntelliJ IDEA Community Edition 2016.2.x\\lib
      git4idea=C:\\Program Files (x86)\\JetBrains\\IntelliJ IDEA Community Edition 2016.2.x\\plugins\\git4idea\\lib
      idea=D:\\github\\intellij-community\\bin
      ```
  ***Note:*** The `idea` property is only used for running integration tests so it isn't needed otherwise

## Building the Plugin

### Download Dependencies
Before you can build and run the plugin, you must download the project dependencies to your local disk. 

1. Open a terminal/console window. 
1. Run `./gradlew copyDependencies` from the root directory of the repository. 


## Build with Gradle
Once your `gradle.properties` file has been updated and you've downloaded the dependencies, run the build by:

1. Open a terminal/console window. 
1. Navigate to the repository root.
1. Run `./gradlew zip`.
   * If you have multiple versions of the Java JDK installed, you may need to set your `JAVA_HOME` environment variable to the installation folder of the 1.8 JDK.
1. The plugin zip file will be created in the `plugin.idea/build/distributions/` folder.


## Build and Run with IntelliJ
Once you've downloaded the dependencies, run the build by:

1. Start IntelliJ and open the existing IntelliJ project file `com.microsoft.alm.plugin.idea.iml` from the `plugin.idea` directory.

2. Configure a "IntelliJ Platform Plugin SDK" based on JDK 8 and IntelliJ 2017.x.
   * File -> Project Structure -> Project Settings -> Project
   * Under Project SDK, select the entry marked "IntelliJ IDEA <version number>" if it exists.
     * If this entry does not exist, click New -> IntelliJ Platform Plugin SDK and select the IntelliJ installation location folder.
       * Under Mac, the folder is similar to `/Applications/IntelliJ IDEA.app/Contents`
       * Under Linux, the folder is similar to `<IntelliJ installation location on disk>/idea-IC-172.3968.16` 
       * Under Windows, the folder is similar to `C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2017.x`

3. Configure the project to use language level JDK 6
   * File -> Project Structure -> Project Settings -> Project
   * Under Project Language Level, select "6 - @Override in interface"

4. Configure the project and ***each module*** to build with this "IntelliJ Platform Plugin SDK".
   * File -> Project Structure -> Project Settings -> Project.
     * Under Project SDK, select the entry marked "IntelliJ IDEA <version number>".
   * File -> Project Structure -> Project Settings -> Modules -> Dependencies.
     * For each module beginning with `com.microsoft.alm` (there are three), under Module SDK, select the entry marked "IntelliJ IDEA <version number>". 

5. Add git4idea.jar lib to `com.microsoft.alm.plugin.idea` module if it doesn't already exist.
   * File -> Project Structure -> Project Settings -> Libraries -> Add new Java library
   * Select `<IntelliJ installation location on disk>/plugins/git4idea/lib/git4idea.jar`, name it `git4idea`
   * Add this git4idea lib to the `com.microsoft.alm.plugin.idea` module
     * If there is an existing git4idea dependency that is missing (shown in red), remove it from the list
     * Add -> Library -> git4idea
   * IMPORTANT: you ***must*** make sure the scope for `git4idea` is changed from `Compile` to `Provided`
     * Project Settings -> Modules -> com.microsoft.alm.plugin.idea -> Dependencies.  Change `git4idea` to `Provided`

6. Make sure the 'GUI designer' generates Java source code.
   * File -> Settings (on Windows) / Preferences (on Mac) -> Editor -> GUI Designer -> Generate GUI into -> Select `Java Souce Code`

7. Create a "Plugin" configuration to run/debug the code.
   * Run -> Edit Configurations... -> Add -> Plugin 
   * Provide a name for the configuration (e.g., IntelliJ for TFS)
   * Set "Use classpath of module" to `com.microsoft.alm.plugin.idea`

8. Run the plugin by selecting Run -> Run <configuration you used above>.

9. Debug the plugin by selecting Run -> Debug <configuration you used above>.

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

2. No tabs in source code:
   * Settings -> Editor -> Code Style -> Java -> Tabs and Indents -> Uncheck "Use tab character", set "Tab size" and "Indent" to 4

3. No wildcard imports (ex. `java.awt.*`):
   * Settings -> Editor -> Code Style -> Java -> Imports -> Set "Class count to use import with '\*'" and "Names count to use static import with '\*'" to sufficient large numbers such as 100
   * On the same page remove `java.awt.*` and `javax.swing.*` from the "Package to Use Import with '*'" list

Those settings are already configured in the `com.microsoft.alm.plugin.idea.iml` project file we provided.  

Gradle build will fail if checkstyle plugin detects a violation.

## Running Integration Tests (L2 tests)

Our Integration tests are in the L2Tests folder. In order to run them correctly, you have to set up the environment and have a VSTS account setup to run against.

Here are the steps to setup your environment:

1. Before you start, you will need to download and build the IntelliJ Community Edition version 2017.
   * You can find the instructions here: https://github.com/JetBrains/intellij-community
  
2. First setup the gradle.properties file. There is an example above. 
   * Specify the location of the git4idea plugin via the `git4idea` property (this can be found in the installation folder of the IntelliJ 16 Community Edition)
   * Specify the location of the IDEA lib folder via the `ideaSdk` property (this can be found in the installation folder of the IntelliJ 16 Community Edition) 
   * Specify the location of the IDEA bin folder via the `idea` property (this is the bin folder of the IntelliJ GitHub repository that you downloaded)
  
3. Second setup the environment variables that provide the connection information for the tests. If this information is missing the tests will fail with a message that describes the missing information. The values below are examples but you will have to fix them.
   * MSVSTS_INTELLIJ_RUN_L2_TESTS=true
   * MSVSTS_INTELLIJ_TF_EXE=d:\bin\TEE-CLC-14.0.4\tf.cmd
   * MSVSTS_INTELLIJ_VSO_GIT_REPO_URL=https://account.visualstudio.com/_git/projectName
   * MSVSTS_INTELLIJ_VSO_LEGACY_GIT_REPO_URL=https://account.visualstudio.com/defaultcollection/_git/projectName
   * MSVSTS_INTELLIJ_VSO_PASS=PersonalAccessTokenGeneratedFromTheUserSecurityPage
   * MSVSTS_INTELLIJ_VSO_SERVER_URL=https://account.visualstudio.com
   * MSVSTS_INTELLIJ_VSO_TEAM_PROJECT=projectName
   * MSVSTS_INTELLIJ_VSO_USER=EmailAddressForUser

4. Last you want to setup a Run Configuration for the L2 Tests inside IntelliJ
   * Create a new JUnit run configuration with the following settings
   * Set VM options to `-ea -Xmx2048M -Didea.config.path=..\test-config -Didea.system.path=..\test-system -Didea.test.group=ALL_EXCLUDE_DEFINED`
   * Set the working directory to `D:\github\intellij-community\bin` i.e. the path to the bin folder in your IntelliJ github repository
   * Use classpath of module `L2Tests`

5. Other things to note:
   * You can toggle whether the tests will run or not simply by changing the MSVSTS_INTELLIJ_RUN_L2_TESTS environment variable.
   * The internal CI build will run these tests

## Learn More

Want more information? The following resources are available to help:

* <a href="http://java.visualstudio.com/Downloads/intellijplugin/Index" target="_blank">Instructions</a> on how to install the plugin
* <a href="https://youtu.be/wSdgmQL-Zbg" target="_blank">End-to-end demo</a> video of the plugin's features
* <a href="http://java.visualstudio.com/Docs/tools/intelliJ" target="_blank">Documentation and tutorial</a> on how to use the plugin 
