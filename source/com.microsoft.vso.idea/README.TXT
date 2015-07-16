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
======================
From IntelliJ, open the project Java.IntelliJ/source/com.microsoft.vso.idea/com.microsofot.vso.ide.iml

1 - Setup a JDK for 1.6 and call it "1.6"
    * File -> Project Structure -> Platform Settings -> SDKs -> little green plus

2 - Setup an IntelliJ Plugin SDK and call it "IntelliJ IDEA Community Edition"
    * File -> Project Structure -> Platform Settings -> SDKs -> little green plus

