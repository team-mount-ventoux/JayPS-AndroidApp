PebbleBike-AndroidApp
=====================

##Prerequisites  

Gradle 1.11  
Download ``http://www.gradle.org/downloads``  
Set GRADLE_HOME environment var ``export GRADLE_HOME=/Applications/gradle-1.9``  
Set Gradle bin to your exe path ``export PATH=$GRADLE_HOME/bin:$PATH``


##Build Instructions  

Update the submodules 
```
git submodules init
git submodules update
```

Install the gradle test plugin
```
cd submodules/gradle-android-test-plugin
./gradlew install
```

Run the tests  

Build the application
```
gradle
```
