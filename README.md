# Monkeyboard - DAB & FM radio for android

<img align="right" src="screenshots/demo.gif">

[Latest Build](/app/build/outputs/apk/debug) (Minimum supported android version 4.2)

Build has only been tested on 6.0 & 5.1. 

If the app crashes on your system submit an issue with a full log file (Google it if you dont know how to do this)

## About

A [Monkeyboard](http://www.monkeyboard.org/products/85-developmentboard/80-dab-fm-digital-radio-development-board) (Keystone radio) is a full DAB & FM radio on a circuitboard.

This app interfaces with a Monkeyboard and provides a control and feedback interface for the user. The Monkeyboard communicates via USB serial, and so to use the board a USB Host cable is required to connect the board to the android device.

The app is designed to act as if it is a normal music player as so will act accordingly, for example audio focus will be follow a mediaplayer notification is provided. 

The monkeyboard does not transmit audio over the USB connection, so the user will need to mix this audio with the tablet audio externally. (Think of the android device as being a screen for the monkeyboard)

I designed this app for use in my car, as so the interface has been designed for large screens.

## Features

- DAB Slideshow! This streams an image from the board and displays it along side the program text information. **This does not require internet**
- DAB Program text
- FM RDS information
- Radio station list. Editable FM saved stations list
- Player notificiation controls
- Automatic android audio handling (Radio will not play over the top of any other android audio)

## Libraries
~
The application uses several third party libraries to acheive full functionallity.

- [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) - this has been slightly modified in this repo for newer android versions.
- [SnappySmoothScroller](https://github.com/nshmura/SnappySmoothScroller)
- [CircularProgressView](https://github.com/rahatarmanahmed/CircularProgressView)

## Contributing

If you wish to make a fix, fork this project, commit, and make a pull request describing the fix in detail.

If you wish to report a bug, create an issue and I will look into it.

## Screenshots
<p align="center">
    <img src="screenshots/main_interface.png" alt="Main interface" width="800"/>
</p>

<p align="center">
    <img src="screenshots/notification.png" alt="Notification with controls, program infromation, and slideshow image" width="800"/>
</p>


<p align="center">
    <img src="screenshots/settings_interface.png" align="center" alt="Settings interface" width="800"/>
</p>


<p align="center">
    <img src="screenshots/dab_search.gif" alt="Searching interface" width="800"/>
</p>

<p align="center">
    <img src="screenshots/portrait.png" alt="Main interface protrait" width="400"/>
</p>


## Licence

    GPL 3.0
   