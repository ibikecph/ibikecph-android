# I BIKE CPH
This repository contains the code for the I BIKE CPH and Cykelplanen
apps. It was made by taking the `develop` branches of three previously
separate projects.

This project has been maintained by Spoiled Milk until late 2014 when
it was taken over by Socialsquare (formerly BIT BLUEPRINT).

## Getting started

### Check out the repository

As always, first step is checking out the repository from GitHub. It is
highly recommended that you check out and build the project using the
Android Studio 2.0 or newer.

### Initializing (or updating) the sub-modules

The Android project shares translated human readable text-strings with
the iOS project. When you've checked out the repository, you need to
initialize it's submodules as well:

    git submodule update --init

### Creating a `secret.properties` file

To be able to build and sign the apps using the development-keystore in
the root of this directory, either add a secret.properties or as
environment variables.

    DEV_KEYSTORE_PASSWORD=...
    DEV_KEY_ALIAS=...
    DEV_KEY_PASSWORD=...
    HOCKEY_APP_TOKEN=... # Used when pushing to HockeyApp

### Building a production app (if you are releasing)

It is recommended that you use Android Studio when building the app in
release mode, this involves a few steps:

- Checkout the tag related to the release we want to make.
- From the "Build" menu in the top menu and choose "Generate Signed APK"
  and follow along with the wizard.
- For older version of Android, you must enable V1 (in addition to V2)
  signing when building the sigend app.
  Otherwise the app will fail to install with a simple
  message the the app failed to install, without further details.
 
## Code structure

As stated, this project brings together three separate projects,
`IBikeCPHLib` (the *library*), `IBikeCPH` and `Supercykelstier`.
The latter was previously called `Cykelsuperstier` and indeed some
references might still use the old name.

The library implements all of the functionality needed for the
I BIKE CPH app, so actually the `IBikeCPH` project is just a shell that
runs the main activity of the library and publishes it under the `dk.kk`
namespace, which is what the city of Copenhagen uses.

Supercykelstier also relies on the library for most of its
functionality, but also implements some additional things, among other
things the "Knæk ruten" functionality.

The Java code is published under the `dk.kk` namespace, and
split into packages that reflect what part of the app they pertain to.
Model class names are suffixed with  `Data`.
