Generate the Rest Client (optional)
===================================
If you skip this step, you will get the latest rest client code published to the nexus repository in azure in the next step.
From the command line, go to your Java.Shared git repo folder and run the following maven command:
    * mvn install

Copy Maven Dependencies
=======================
From the command line, go to your Java.IntelliJ/source folder and execute the following two maven commands:
    * mvn dependency:copy-dependencies -DoutputDirectory=TfIntelliJPlugin/lib/classes
    * mvn dependency:copy-dependencies -DoutputDirectory=TfIntelliJPlugin/lib/src -Dclassifier=sources

Dev Env Setup
=============
1 - Install IntelliJ IDEA Ultimate and IntelliJ IDEA Community Edition
    * https://www.jetbrains.com/idea/download/index.html

2 - Clone the Community Edition sources
    * From the command line, in the root path of the Community Edition folder, clone the sources repo with:
        git clone git://git.jetbrains.org/idea/community.git idea

3 - From IntelliJ Ultimate, open the project Java.IntelliJ\source\TfIntelliJPlugin

4 - Setup a JDK for 1.6 called "IDEA jdk"
    * File -> Project Structure -> Platform Settings -> SDKs -> little green plus
    * It must be called "IDEA jdk"

5 - Setup an IntelliJ Platform Plugin SDK called "IntelliJ IDEA Community Edition"
    * File -> Project Structure -> Platform Settings -> SDKs -> little green plus
    * Point it to your IntelliJ Community Edition location on disk, such as C:\Program Files (x86)\JetBrains\IntelliJ IDEA Community Edition 14.1.4
    * It must be called "IntelliJ IDEA Community Edition"
    * When prompted for the Java SDK to use, select the "IDEA jdk" that was created above
    * On the Sourcepath tab, click the little green plus and add the path to the Community Edition sources, such as C:\Program Files (x86)\JetBrains\IntelliJ IDEA Community Edition 14.1.4\idea

6 - Setup a Global library called "git4idea" 
    * File -> Project Structure -> Platform Settings -> Global Libraries -> little green plus -> Java
    * Point it to <IntelliJ community edition location on disk>\plugins\git4idea\lib\git4idea.jar

7 - Configure the project to build with the above settings
    * File -> Project Structure -> Project Settings -> Modules -> com.microsoft.tf -> Module SDK: -> "IntelliJ IDEA Community Edition"

Build from IntelliJ
===================
To compile:
    * Build -> Make Project
To package the plugin:
    * Build -> Prepare All Plugin Modules For Deployment -> com.microsoft.tf
    
Debug from IntelliJ
===================
Run -> Edit Configurations, Add a default "Plugin" configuration, name it "tfidea"
Run -> Debug tfidea
