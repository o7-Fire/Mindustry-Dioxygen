![Logo](core/assets/sprites/logo.png)

[![Build Status](https://travis-ci.org/Anuken/Mindustry.svg?branch=master)](https://travis-ci.org/Anuken/Mindustry) 
[![Discord](https://img.shields.io/discord/391020510269669376.svg)](https://discord.gg/mindustry)  

A sandbox tower defense game written in Java.

_[Trello Board](https://trello.com/b/aE2tcUwF/mindustry-40-plans)_  
_[Wiki](https://mindustrygame.github.io/wiki)_ 

### Building u self

Bleeding-edge live builds are generated automatically for every commit. You can see them [here](https://github.com/Anuken/MindustryBuilds/releases). Old builds might still be on [jenkins](https://jenkins.hellomouse.net/job/mindustry/).

If you'd rather compile on your own, follow these instructions.
First, make sure you have [Java 8](https://www.java.com/en/download/) and [JDK 8](https://adoptopenjdk.net/) installed. Open a terminal in the root directory, `cd` to the Mindustry folder and run the following commands:
and then download the repo and unzip it
after unzip u must run gradlew.bat
#### Important you will need 

#### JDK 8

#### JRE 8


#### Windows

_Building:_ `click me to building desktop.bat `

#### Linux/Mac OS

_Running:_ `./gradlew desktop:run`  
_Building:_ `./gradlew desktop:dist`

#### Server

Server builds are bundled with each released build (in Releases). If you'd rather compile on your own, replace 'desktop' with 'server', e.g. `gradlew server:dist`.

#### Android
_Building:_ `click me to building android.bat `

##### Troubleshooting

If the terminal returns `Permission denied` or `Command not found` on Mac/Linux, run `chmod +x ./gradlew` before running `./gradlew`. *This is a one-time procedure.*

---
Android:Mindustry-o7\android\build\outputs\apk\debug
Gradle may take up to several minutes to download files. Be patient. <br>
After building, the output .JAR file should be in `/desktop/build/libs/Mindustry.jar` for desktop builds, and in `/server/build/libs/server-release.jar` for server builds.


### Downloads

[<img src="https://static.itch.io/images/badge.svg"
     alt="Get it on Itch.io"
     height="60">](https://anuke.itch.io/mindustry)

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=io.anuke.mindustry)

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/io.anuke.mindustry/)
# Mindustry-o7
