Generate the Rest Client (optional)
===================================
If you skip this step, you will get the latest rest client code published to the nexus repository in azure in the next step.
From the command line, go to your Java.Shared git repo folder and run the following maven command:
    * mvn install

Copy Maven Dependencies
=======================
From the command line, got to your Java.IntelliJ/source folder and execute the following two maven commands:
    * mvn dependency:copy-dependencies -DoutputDirectory=TfIntelliJPlugin/lib/classes
    * mvn dependency:copy-dependencies -DoutputDirectory=TfIntelliJPlugin/lib/src -Dclassifier=sources

Dev Env Setup
=============
From IntelliJ, open the project Java.IntelliJ/source/com.microsoft.vso.idea

1 - Setup a JDK for 1.6 called "IDEA jdk"
    * File -> Project Structure -> Platform Settings -> SDKs -> little green plus

2 - Setup an IntelliJ Plugin SDK called "IntelliJ IDEA Community Edition", point it to IntelliJ community edition location on disk 
    * File -> Project Structure -> Platform Settings -> SDKs -> little green plus

3 - Setup a Global library called "git4idea", point it to <IntelliJ community edition location on disk>\plugins\git4idea\lib\git4idea.jar 
    * File -> Project Structure -> Platform Settings -> Libraries -> little green plus

Build from IntelliJ
===================
From IntelliJ:
    * Build -> Make Project to compile
    * Build -> Prepare Plugin Module 'com.microsoft.vso.idea' For Deployment
    
Debug from IntelliJ
===================
Run -> Edit Configurations, Add a default "Plugin" configuration, name it "vsoidea"
Run -> Debug vsoidea

