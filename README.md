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

1. Run `./gradlew zip`. The plugin zip will be created in `build/distributions/` folder


## Build and Run with IntelliJ
1. Create a new IntelliJ project from existing source, or open the existing IntelliJ project file `com.microsoft.tf.iml` in the source root directory

1. Configure a "IntelliJ Platform Plugin SDK" basd on JDK 6 and IntelliJ 14.x

1. Configure the project to build with this "IntelliJ Platform Plugin SDK" 

1. Add git4idea.jar lib to the project
  * File -> Project Structure -> Project Settings -> Libraries -> Add new Java library
  * Select <IntelliJ community edition location on disk>/plugins/git4idea/lib/git4idea.jar, name it git4idea
  * Add this git4idea lib to the module and change the scope from `Compile` to `Provided`
  
1. Make sure 'GUI designer' generates Java source code
  * File -> Settings (on Windows) / Preferences (on Mac) -> Editor -> GUI Designer -> Generate GUI into -> Select `Java Souce Code`  
  
1. Create a configuration to run/debug the code
  * Run -> Add a "Plugin" configuration 
  

## Contributing

We welcome Pull Requests. 
