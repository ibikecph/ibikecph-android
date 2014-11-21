##README

The I Bike CPH project makes use of one Android library *ibikecph-lib-android* that includes other sub-libraries.

Android library **ibikecph-lib-android** includes:
- facebook sdk android
- osmdroid

##Build

This Android project is meant to be used inside Eclipse IDE.
For latest code, branch *develop* should be used.

**Clone the iBikecph repository**
- git clone https://github.com/ibikecph/ibikecph-android.git

**Switch to *develop* branch**
 - git checkout develop

**Clone the iBikecph-lib-android**
 - git submodule init => initializes your local configuration file
 - git submodule update => clones the ibike-cph-lib

**Use Eclipse IDE to import the projects from the cloned folder**