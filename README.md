# I BIKE CPH
This repository contains the code for the I BIKE CPH and Cykelplanen apps. It was made by taking the `develop` branches of three previously separate projects.

This project has been maintained by Spoiled Milk until late 2014 when it was taken over by BIT BLUEPRINT.  

## Code structure
As stated, this project brings together three separate projects, `IBikeCPHLib` (the *library*), `IBikeCPH` and `Supercykelstier`. The latter was previously called `Cykelsuperstier` and indeed some references might still use the old name.

The library implements all of the functionality needed for the I BIKE CPH app, so actually the `IBikeCPH` project is just a shell that runs the main activity of the library and publishes it under the `dk.kk` namespace, which is what the city of Copenhagen uses.

Supercykelstier also relies on the library for most of its functionality, but also implements some additional things, among other things the "Knæk ruten" functionality.

The Java code is published under the `com.spoiledmilk` namespace, and split into packages that reflect what part of the app they pertain to. Model class names are suffixed with  `Data`.
