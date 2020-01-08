# Selenium project for front-end testing

This project is intended for runing front-end test on different environments. This will be version one which will be from here on out taged for every major release. In the near future the feature files, settings.properties and all the drivers will be moved to different repositories.

## Project structure (when all the repositories will be finished)

* Selenium repository
* Feature file and settings repository
* Drivers repository

# Setting up environment for testing localy 

To start testing on your loacal machine you will need to add the following VM arguments to the Java application runner:

_-Dcucumber.options="path/to/feature/files/in/project --tags @Test"_

Since the project will no longer contain the feature files directly, you have two options. You can either copy the feature files in for example /src/main/resources/features or you can create a symbolic or soft link that point to the location of the feature file and settings repository (this way you will always have the updated feature files).

If you have this set up then you should be able to run the test without any problem, by default if running this localy we use the default settings.properties file either for admin or client.

# Building and using JAR selenium file

We can also create an executable file that can be used in case we need to test stuff from older versions or older releases (since this will be taged in the future). We can create the JAR file by positioning ourself in the selenium project and runing the command bellow:

_mvn clean package_

If you want too run the project then you have to run it with the code bellow from CMD:

_java -Dcucumber.options="features/ --tags @Test" -DisApplicationExeJar="true" -jar .\funtional-testing-1.0-SNAPSHOT.jar_

You need to add the features/ (this is the folder in which the feature files are located, and should be on the same level as the JAR). The isApplicationExeJar should be set to true if executing JAR to use different code for accessing drivers, settings and features.

The most basic structure to use is the folowing:

	|-features/*  
	|  
	|-drivers/*  
	|  
	|-settings/*  
	|  
	|-funtional-testing-1.0-SNAPSHOT.jar  
 
With this structure as long as there are no errors in any of the files provided the JAR should be executed properly.

