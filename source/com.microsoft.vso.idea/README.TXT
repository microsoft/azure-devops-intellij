Generate the Rest Client (optional)
===================================
If you skip this step, you will get the latest rest client code published to tfsxp-boundary in the next step.
From the command line, go to your Java.Shared git repo folder and run the following maven command:
    * mvn install

Copy Maven Dependencies
=======================
From the command line, got to your Java.IntelliJ/source folder and execute the following two maven commands:
    * mvn dependency:copy-dependencies -DoutputDirectory=com.microsoft.vso.idea/lib/classes
    * mvn dependency:copy-dependencies -DoutputDirectory=com.microsoft.vso.idea/lib/src -Dclassifier=sources

Dev Env Setup
=============
From IntelliJ, open the project Java.IntelliJ/source/com.microsoft.vso.idea/com.microsofot.vso.ide.iml

1 - Setup a JDK for 1.6 and call it "1.6"
    * File -> Project Structure -> Platform Settings -> SDKs -> little green plus

2 - Setup an IntelliJ Plugin SDK and call it "IntelliJ IDEA Community Edition"
    * File -> Project Structure -> Platform Settings -> SDKs -> little green plus

Build from IntelliJ
===================
From IntelliJ:
    * Build -> Prepare Plugin Module 'com.microsoft.vso.idea' For Deployment

Generate Ant Files (these are by command line builds)
=====================================================
If you skip this step, the existing ant build files will be used below, which will generally be OK as long as there are no structural changes
From IntelliJ:
    * Build -> Generate Ant Build

Build from Command Line
=======================
From the command line, got to your Java.IntelliJ/source/com.microsoft.vso.idea folder and execute the following ant command:
    * ant -buildfile com.microsoft.vso.idea.xml