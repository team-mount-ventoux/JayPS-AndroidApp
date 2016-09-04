PebbleBike-AndroidApp
=====================

[![Build Status](https://api.travis-ci.org/team-mount-ventoux/PebbleVentoo-AndroidApp.svg?branch=master)](https://travis-ci.org/team-mount-ventoux/PebbleVentoo-AndroidApp)
[![Coverage Status](https://coveralls.io/repos/team-mount-ventoux/PebbleVentoo-AndroidApp/badge.png?branch=master)](https://coveralls.io/r/team-mount-ventoux/PebbleVentoo-AndroidApp?branch=v2)

[Pebble Bike](http://www.pebblebike.com) is a GPS cycle computer for your Pebble smart watch. It uses your phone's GPS to send speed, distance and altitude data to your Pebble. You can also see your track directly on your Pebble.

Pebble Bike also has an innovative auto start feature which will auto start the bike computer on your watch when it detects you are riding a bike, using low battery technology pebble bike checks your activity every 30 seconds, so low power in fact you can leave this switched on all day.

Pebble Bike has a Live Tracking function that sends your position every 30 seconds to the internet. If you're using it with your friends, it can receive theirs positions and display them with your own track, directly on your Pebble.

Licensed under [MIT License](http://opensource.org/licenses/MIT)

## Contributors
* JayPS (https://github.com/jay3)  
* Nic Jackson (https://github.com/nicholasjackson)

## Translations
* English
* French
* German (Hanspeter Jochmann)
* Spanish (ZoretMan)
* Italian (Riccardo Fantoni)
* Japanese (Moyashi)
* Dutch (Erwin Dirkx)

## Watchface
See also the [Pebble Bike Watch Face](https://github.com/pebble-bike/PebbleBike-PebbleWatchFace).

##Info
Pebble bike v2 is pretty much a complete re-write to implement best practice in terms of coding.  In an attempt to improve stability and reduce crashes when we develop the application futher we decided that we needed to re-architect the appliction.  

To allow testability we have had to de-couple many parts of the application so to facilitate this we have used the excellent frameworks dagger (for dependency injection) and otto (event bus) written by square.  Initially we intended to use Roboelectric to run all our tests super fast without the need to install the application on a device or emulator but this has proven difficult as Android studio and Gradle do not support this method out of the box.  Due to a desire to have a simple build and setup we decided to fall back to the AndroidInstrumentation tests as recommended by Google.

##Prerequisites  

Gradle 2.10  
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
./gradlew connectedCheck
```
