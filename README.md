# Azure DevOps Plugin for IntelliJ, Android Studio, & other JetBrains IDEs

This is a plugin for working with Git and TFVC repositories on Azure DevOps and Team Foundation Server (TFS) 2015+ inside IntelliJ, Android Studio, 
and various other JetBrains IDEs. It is supported on Linux, Mac OS X, and Windows.
It is compatible with IntelliJ IDEA Community and Ultimate editions (version 2018.3+) and Android Studio (version 3.4+).

To learn more about installing and using our Azure DevOps IntelliJ plug-in, visit: https://docs.microsoft.com/en-us/azure/devops/java/download-intellij-plug-in

## Pre-Reqs
1. Install JDK 8. 
   * You can find the JDK 8 download on Oracle's web site at <a href="http://www.oracle.com/technetwork/java/javase/downloads" target="_blank">Java SE Downloads</a>.
2. Set JAVA_HOME to the location of JDK 8.
3. Clone the repository (if planning on building yourself).

## Building the Plugin

## Build with Gradle
Run the build by:

1. Open a terminal/console window. 
2. Navigate to the repository root.
3. Run `./gradlew buildPlugin`
   * If you have multiple versions of the Java JDK installed, you may need to set your `JAVA_HOME` environment variable to the installation folder of the 1.8 JDK.
4. The plugin zip file will be created in the `plugin/build/distributions/` folder.


## Build and Run with IntelliJ
Once you've downloaded the dependencies, run the build by:

1. Start IntelliJ and open the Gradle project from the root project directory.

2. Configure the project to use language level JDK 8
   * File -> Project Structure -> Project Settings -> Project
   * Under Project Language Level, select "8 - Lambdas, type annotations etc."

3. Configure the project and ***each module*** to build with this "IntelliJ Platform Plugin SDK".
   * File -> Project Structure -> Project Settings -> Project.
     * Under Project SDK, select the entry marked "IntelliJ IDEA <version number>".
   * File -> Project Structure -> Project Settings -> Modules -> Dependencies.
     * For each module beginning with `com.microsoft.alm` (there are three), under Module SDK, select the entry marked "IntelliJ IDEA <version number>". 

4. Make sure the 'GUI designer' generates Java source code.
   * File -> Settings (on Windows) / Preferences (on Mac) -> Editor -> GUI Designer -> Generate GUI into -> Select `Java Souce Code`

5. Create a "Plugin" configuration to run/debug the code.
   * Run -> Edit Configurations... -> Add -> Gradle 
   * Provide a name for the configuration (e.g., IntelliJ for TFS)
   * Set Gradle project to `azure-devops-intellij`
   * Set Tasks to `:plugin:runIde`

6. Run the plugin by selecting Run -> Run <configuration you used above>.

7. Debug the plugin by selecting Run -> Debug <configuration you used above>.

8. To run tests please check options on the page `Preferences | Build, Execution, Deployment | Build Tools | Gradle | Runner`
    * `Delegate IDE build/run actions to gradle` should checked.
    * In `Run tests using` should select `Gradle Test Runner`

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

## Running Integration Tests (L2 tests and reactive client tests)

Our Integration tests are in the L2Tests folder. In order to run them correctly, you have to set up the environment and have an Azure DevOps Services organization setup to run against.

You'll need to add a test project into your account, and add both git and TFVC repositories into it. Git repository should also include a `README.md` file in the repository root, and TFVC repository should include a `readme.txt` file.

Here are the steps to setup your environment:
1. First create run configuration from `L2Tests` gradle project:
   * tasks: `cleanTest test`
   * arguments: `--tests *`
2. Second setup the environment variables that provide the connection information for the tests. If this information is missing the tests will fail with a message that describes the missing information. The values below are examples but you will have to fix them.
   * `MSVSTS_INTELLIJ_RUN_L2_TESTS=true`
   * `MSVSTS_INTELLIJ_TF_EXE=d:\bin\TEE-CLC-14.0.4\tf.cmd`
   * `MSVSTS_INTELLIJ_VSO_GIT_REPO_URL=https://organization.visualstudio.com/projectName/_git/repoName`
   * `MSVSTS_INTELLIJ_VSO_LEGACY_GIT_REPO_URL=https://organization.visualstudio.com/defaultcollection/_git/projectName`
   * `MSVSTS_INTELLIJ_VSO_PASS=PersonalAccessTokenGeneratedFromTheUserSecurityPage`
   * `MSVSTS_INTELLIJ_VSO_SERVER_URL=https://organization.visualstudio.com` (make sure no trailing slash here)
   * `MSVSTS_INTELLIJ_VSO_TEAM_PROJECT=projectName`
   * `MSVSTS_INTELLIJ_VSO_USER=EmailAddressForUser`
   * `MSVSTS_INTELLIJ_UNIQUE_SUFFIX=""`: you may leave it empty; if not empty, it will be used as a suffix for various names in tests; introduced for simultaneous test execution on agents
   
   _Note_: Do not use https://dev.azure.com/account/ addresses in these environment variables, make sure to use https://account.visualstudio.com/

3. Other things to note:
   * You can toggle whether the tests will run or not simply by changing the MSVSTS_INTELLIJ_RUN_L2_TESTS environment variable.
   * The internal CI build will run these tests

4. To run the reactive client integration tests, create a run configuration for `:client:backend:test` task, or use Gradle to run it. It uses the same environment variables as L2 tests.

## Learn More

Want more information? The following resources are available to help:

* <a href="https://docs.microsoft.com/en-us/azure/devops/java/download-intellij-plug-in" target="_blank">Instructions</a> on how to install the plugin
* <a href="https://youtu.be/wSdgmQL-Zbg" target="_blank">End-to-end demo</a> video of the plugin's features
* <a href="https://docs.microsoft.com/en-us/azure/devops/repos/git/create-repo-intellij" target="_blank">Documentation and tutorial</a> on how to use the plugin 
