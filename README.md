# Visual Studio Team Foundation Plugin for IntelliJ (Preview)

An IntelliJ plugin for working with Git repositories on Visual Studio Online (VSO) and Team Foundation Server (TFS) 2015 inside IntelliJ.
Supported on Linux, Mac OS X, and Windows.
Compatible with IntelliJ IDEA Community and Ultimate editions (version 14) and Android Studio (version 1.2 - 1.4).


## Pre-Reqs
1. Install JDK 6
1. Install IntelliJ IDEA Community Edition version 14.x

## Create (or Update) the Gradle Properties file
We use Gradle as the build tool for this project.  Before you get started, you will need to either create a `gradle.properties` 
file in your user profile directory or update the file found in the IntelliJ repository root folder.

From a terminal/console window,
1. To create a `gradle.properties` file in your user profile directory, cd into your user profile directory.

1. Use your favorite text editor to create the file.

1. If you are updating the file in the IntelliJ repository, simply open that file in your favorite text editor.

1. Add or update the values of ideaSdk and git4idea as shown below.

  * Sample property file on Linux
```
ideaSdk=~/idea-IC-141.2735.5/lib
git4idea=~/idea-IC-141.2735.5/plugins/git4idea/lib
```
  * Sample property file on Mac
```
ideaSdk=/Applications/IntelliJ IDEA 14 CE.app/Contents/lib
git4idea=/Applications/IntelliJ\ IDEA\ 14\ CE.app/Contents/plugins/git4idea/lib
```
  * Sample property file on Windows
```
ideaSdk=C:\\Program Files (x86)\\JetBrains\\IntelliJ IDEA Community Edition 14.1.4\\lib
git4idea=C:\\Program Files (x86)\\JetBrains\\IntelliJ IDEA Community Edition 14.1.4\\plugins\\git4idea\\lib
```

## Build with Gradle
Once your `gradle.properties` file has been updated, run the build.

1. From a terminal/console window, change to the root folder of the IntelliJ repository and then run `./gradlew zip`.
1. The plugin zip file will be created in the `plugin.idea/build/distributions/` folder.


## Build and Run with IntelliJ
1. Before you can build and run with IntelliJ, you must run `gradlew` to download the project dependencies.  If you have already built with Gradle
using the steps in the previous section, you can skip the next step and just open the IntelliJ project file detailed below.

1. If you have not built with Gradle yet, open a terminal/console window and run `./gradlew copyDependencies` from the root directory of the IntelliJ repository.
This will download all of the necessary build dependencies to your local disk.

1. Open the existing IntelliJ project file `com.microsoft.alm.plugin.idea.iml` from the `plugin.idea` directory.

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
  * Select <IntelliJ community edition location on disk>/plugins/git4idea/lib/git4idea.jar, name it `git4idea`
  * Add this git4idea lib to the `com.microsoft.alm.plugin.idea` module
  * IMPORTANT: you ***must*** make sure the scope for `git4idea` is changed from `Compile` to `Provided`
    * Project Settings -> Modules -> com.microsoft.alm.plugin.idea -> Dependencies.  Change `git4idea` to `Provided`

1. Make sure the 'GUI designer' generates Java source code.
  * File -> Settings (on Windows) / Preferences (on Mac) -> Editor -> GUI Designer -> Generate GUI into -> Select `Java Souce Code`

1. Create a "Plugin" configuration to run/debug the code.
  * Run -> Edit Configurations... -> Add a "Plugin" configuration 
  * Provide a name for the configuration (e.g., IntelliJ for TFS)
  * Set "Use classpath of module" to `com.microsoft.alm.plugin.idea`


## Contributing

We welcome Pull Requests.

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

