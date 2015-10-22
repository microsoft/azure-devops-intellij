# Visual Studio Team Foundation Plugin for IntelliJ (Preview)

An IntelliJ plugin for working with Git repositories on Visual Studio Online (VSO) and Team Foundation Server (TFS) 2015 inside IntelliJ.
Supported on Linux, Mac OS X, and Windows.
Compatible with IntelliJ IDEA Community and Ultimate editions (version 14) and Android Studio (version 1.2 - 1.4).


## Pre-Reqs
1. Install JDK 6
1. Install IntelliJ IDEA Community Edition version 14.x


## Build with Gradle

We use Gradle as the build tool for this project.
1. cd into the source root directory

1. Create a `gradle.properties` file that points to IntelliJ libraries

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

1. Run `./gradlew zip`. The plugin zip will be created in `plugin.idea/build/distributions/` folder


## Build and Run with IntelliJ
1. Run `./gradlew copyDependencies` from the root directory to download dependencies to local

1. Open the existing IntelliJ project file `com.microsoft.alm.plugin.idea.iml` inside `plugin.idea` directory

1. Configure a "IntelliJ Platform Plugin SDK" basd on JDK 6 and IntelliJ 14.x

1. Configure the project and each module to build with this "IntelliJ Platform Plugin SDK"

1. Add git4idea.jar lib to `com.microsoft.alm.plugin.idea` module if it doesn't already exist
  * File -> Project Structure -> Project Settings -> Libraries -> Add new Java library
  * Select <IntelliJ community edition location on disk>/plugins/git4idea/lib/git4idea.jar, name it git4idea
  * Add this git4idea lib to `com.microsoft.alm.plugin.idea` module
  * IMPORTANT: please make sure the scope for git4idea is changed from `Compile` to `Provided`

1. Make sure 'GUI designer' generates Java source code
  * File -> Settings (on Windows) / Preferences (on Mac) -> Editor -> GUI Designer -> Generate GUI into -> Select `Java Souce Code`

1. Create a configuration to run/debug the code
  * Run -> Add a "Plugin" configuration


## Contributing

We welcome Pull Requests.

A few styles we follow:
1. All java source files must have the following two lines at the top:
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

