PebbleBike-AndroidApp
=====================

[![Build Status](https://travis-ci.org/pebble-bike/PebbleBike-AndroidApp.svg?branch=v2)](https://travis-ci.org/pebble-bike/PebbleBike-AndroidApp)
[![Coverage Status](https://coveralls.io/repos/pebble-bike/PebbleBike-AndroidApp/badge.png?branch=v2)](https://coveralls.io/r/pebble-bike/PebbleBike-AndroidApp?branch=v2)

##Info
Pebble bike v2 is pretty much a complete re-write to implement best practice in terms of coding.  In an attempt to improve stability and reduce crashes when we develop the application futher we decided that we needed to re-architect the appliction.  

To allow testability we have had to de-couple many parts of the application so to facilitate this we have used the excellent frameworks dagger (for dependency injection) and otto (event bus) written by square.  Initially we intended to use Roboelectric to run all our tests super fast without the need to install the application on a device or emulator but this has proven difficult as Android studio and Gradle do not support this method out of the box.  Due to a desire to have a simple build and setup we decided to fall back to the AndroidInstrumentation tests as recommended by Google.

##Prerequisites  

Gradle 1.10  
Download ``http://www.gradle.org/downloads``  
Set GRADLE_HOME environment var ``export GRADLE_HOME=/Applications/gradle``  
Set Gradle bin to your exe path ``export PATH=$GRADLE_HOME/bin:$PATH``


##Build Instructions  

Update the submodules 
```
git submodule init
git submodule update
```
Build the application and run the tests  
```
gradle connectedCheck
```
