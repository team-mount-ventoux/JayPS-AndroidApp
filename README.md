PebbleBike-AndroidApp
=====================

[![Build Status](https://travis-ci.org/pebble-bike/PebbleBike-AndroidApp.svg?branch=v2)](https://travis-ci.org/pebble-bike/PebbleBike-AndroidApp)

##Prerequisites  

Gradle 1.10  
Download ``http://www.gradle.org/downloads``  
Set GRADLE_HOME environment var ``export GRADLE_HOME=/Applications/gradle``  
Set Gradle bin to your exe path ``export PATH=$GRADLE_HOME/bin:$PATH``


##Build Instructions  

Update the submodules 
```
git submodules init
git submodules update
```
Run the tests  

Build the application
```
gradle connectedCheck
```
