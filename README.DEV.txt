Overview
====

ATAK is a Moving Map capability for the Android OS. 

Requirements for Development
====

The following tools are required (at a minimum) to compile and deploy ATAK:

- Java Development Kit 1.8 or greater
- Android SDK with API level 19 installed
- Subversion client 1.7 or greater

If you are using the command line to build and deploy ATAK:

- make / gradle

If using an IDE:

- Android Studio 
- Eclipse (deprecated)
- IntelliJ (deprecated)


All development should make use of the android-formatting.prefs rules, 
use LF (Unix) breaks, and should contain sufficient documentation to 
be used by development team. 

Prior to code review or periodically, your code will be stripped of CR's and 
hard tabs.   It is also a good idea to make use of the code formatting files 
in the root of the directory.   Automatic code formatting is run on occasion.
              
Consideration of thirdparty libraries need to occur early in a release cycle.  
Without proper attribution and appropriate review, third party libraries will 
be removed.


Development Note
====

ATAK makes use a subset of the android-support-v4 library release on 
Android Support Library, revision 19.0.   This library was 
stripped of many unused constructs.    For plugin developers this means 
that you may encounter a situation where the support library is missing 
functionality that you would like to make use of.   In order provide a 
a complete version of the android support library you will need to follow 
the instructions in plugins/support-v4-extra README.txt.


Why does Studio keep warning me about GRADLE
====

As of 4/26/2016 most of the gradle build system has been postured to replace the 
mixed build system we currently support.  Please note, this is mixed only from the 
standpoint of Android Studio.   We use gradle for our continuous integration.

It will need to remain mixed until we can sort out the following issues:

1) gradle plugins depend on ATAK being built first.  Using Android Studio iml files,
Android Studio will automatically compile ATAK prior to launching the plugin.

2) When using gradle plugins, you cannot skip to the source code for the plugin call.
Using Android Studio, you can jump to the declaration.   

Both of these issues have to do with the fact that in Gradle building AAR files 
cannot be "provided" or weakly linked.   Only JAR files can be prodivded or weakly 
linked.   In Gradle, if the plugin project is made to depend on ATAK core 
functionality - it would be no longer weakly included and BREAK our plugins.

This disconnect is not an issue in the SDK repository.

If we did actually enable Gradle builds in Android Studio for Core development
   1) So to produce a plugin you need the JAR files that describe ATAK - you cannot use AAR files with the keyword provided.
   2) If you are editing a plugin in a gradle based Android Studio.  It cannot correctly resolve the code when you attempt to jump to a declaration


[4:17] 
it tries to decompile the JAR file.



Why do you use SVN
====

Is there a clear metric that another version control system will be better?   




Steps for building from the Command Line Interface
====

1. Core team source code, the appropriate command is
     svn co https://svn.takmaps.com/repos/TakMaps/trunk

   SDK plugin access, the appropriate command is:
     svn co https://svn.takmaps.com/repos/plugins
      
    For SDK access, the appropriate compiled version of ATAK is already supplied.  
    Please skip steps 2.

2. Run 'make release' for an obfuscated apk suitable for installation or 
   'make devrelease' for a non-obfuscated version.     
    (Most of the time just issuing a make clean install will do)

     if you want to see more of the make functionality feel free to type make help 
     (running lint, stripping lines, etc).
   

   The build process should result in a BUILD SUCCESSFUL message.

   Note:   Issuing 'make clean install' triggers a rebuild of the ATAK apk, builds a 
           non-obfuscated version, and loads that version via adb install.   
 
	   In some cases a full clean needs to be performed in order to correctly 
           reference changes to layout files within the project or subprojects. 
           A full clean of ATAK can be performed by 'make cleanall'   

           You can also execute 'make help' to get a list of available make targets.

3. Building a plugin
    Once ATAK is built and installed, the plugins can be compiled by going into the 
    plugins directory.   
    For a specific plugin, go ahead and change into that directory and type in 
    'make install'


Using an IDE
====

Project metadata for Eclipse and IntelliJ are included in the SVN repository.  
Support for these tools will be deprecated in the future.   Please make use of 
Android Studio.

The production build is not compiled using an IDE and is compiled using this 
command line Android tools and the corresponding proguard configuration.
Code will be reverted if it does not compile/function when built in the 
production environment.

For ANDROID Studio
====

https://atakmap.com/wiki/index.php?title=AndroidStudioInstructions_here

Preconditions (Required)
-------------

    Java Development Kit (JDK) 8 or later is installed. 
    Available here: http://www.oracle.com/technetwork/java/javase/downloads/index.html 


    *IMPORTANT*
    On a Windows system, install svn:
        https://sliksvn.com/pub/Slik-Subversion-1.9.5-x64.zip

    On Linux / Mac, install svn using whatever package management system is in use.


    *IMPORTANT*
    As of 6/9/2017 - Android Build Tools when used by Android Studio produces a dx.jar error 
    during dexing.   Please revert the Android Build Tools to 25.0.3.
    Tools->Android->SDK Manager
        Click on SDK Tools
        Check the Show Package Details Box in the Lower Right
        Unselect Android SDK Build Tools 26 
        Select Android SDK Build Tools 25.0.3


Build Steps
-----------

    1. Install Android Studio from http://developer.android.com/sdk/index.html 
       and launch it. The version at the time of this entry is 2.3.
    2. At the Welcome to Android Studio Screen, Select 
       "Check out project from Version Control" and select Subversion.
    3. Press the + button and enter:

        For core source code development
        --------------------------------
           https://takmaps.com:8443/scm/svn/TakMaps/trunk

        For plugin SDK development
        --------------------------
           https://takmaps.com:8443/scm/svn/plugins

         as the repository. Highlight this and select Checkout. Do not select one of the sub-directories.
    4. Select a directory to put the ATAK project, click OK.
    5. Click OK accepting the information.
    6. Enter in your SVN credentials.
    7. Select the highest subversion working copy format available.
    8. Wait - there are more than 10K files.
    9. In Android Studio, open Tools > Android > SDK Manager (It may take some time for the SDK Manager to open.)
        In the SDK Manager, select Android 4.2 (API19) and click install packages, then follow the instructions to install it. 

    9b. Disable Instant Run - https://stackoverflow.com/questions/35168753/instant-run-in-android-studio-2-0-how-to-turn-off
    10. Exit and relaunch Android Studio. If it hasn't already reopened the project, go ahead and open it yourself.
    11. Installing ATAK on the system
        For development of plugins, you will need to deploy the developer copy of ATAK on the device.

        (Core Only) Use the ATAK run directive at the top of the screen (selecting ATAK and pressing the play button). Afterwards, you will need to select just the plugin you are going to be compiling and hit the play button. 

        (Plugin SDK) Install the ATAKActivity-*.apk on the phone.


If this is the first time you are deploying a plugin (3.5.3 and earlier, skip for 3.6):

This can be done by going to ATAK Settings->Tools Preferences->Plugin & App Management.   Once there,

1) Select Manual Plugin Scan and select your plugin from the list.   Hit Load Selected Plugins

2) Turn on Plugin Auto Scanning  - enables plugin scanning at startup.


Note:   For deploying in an emulated environment, ATAK might crash due to incompatibilities with the computers graphics card.   Please either modify 

1) construct a file in the /sdcard/atak directory called "opengl.broken"
   - this will set USE_GENERIC_EGL_CONFIG true 

2) set the system property "USE_GENERIC_EGL_CONFIG" to  "true"

3) If all else fails modify code
./ATAKMapEngine/src/com/atakmap/map/opengl/GLMapSurface.java:121

    if (System.getProperty("USE_GENERIC_EGL_CONFIG", "false").equals("true"))
to be:
    if (true)


For ECLIPSE
====

https://atakmap.com/wiki/index.php?title=EclipseInstructions_here


Signing Development Builds
====

ATAK has been using android_keystore provided in this directory.

password: tnttnt



ATAK
====

ATAK is a moving map capability that is comprised of several key concepts:

     Tools - Provide a capabilty for interacting with the map on the right hand side of the 
     screen.   They may or may not have a associated Drop Down component.    Tools are considered
     a legacy concept.

     DropDowns - All of the right hand functionality within ATAK is achieved by the concept of a 
     drop down.  A drop down receiver is what is implemented to inject functionality into the 
     ATAK system. 

     AbstractMapComponent -  An implementation of an AbstractMapComponent is usually used to 
     create the and manage one or more drop down receivers.   Any implementation of the 
     AbstractMapComponent is loaded by a XML file in trunk/ATAK/assets/components.xml

     MapView - The map view is the moving map capability within ATAK.    There are many examples 
     on how Tools and DropDowns make use of the MapView.

     ATAKActivity - This is responsible for the startup, creation, cleanup, and shutdown of ATAK
     proper.

     AtakBroadcast - Used instead of global usage of sendBroadcast, registerReceiver, unregisterReceiver.
     If you need to send a system wide intent, use sendSystemBroadcast if required.    


A MapComponent is the basic unit of functionality for major functionality within 
ATAK.   A MapComponent can have one or more DropDownReceivers, Tools, Widgets. 
Each MapComponent should live in com.atakmap.android at this time and be registered
in the ATAK/assets/components.xml.   Additionally, if there is a DropDownReceiver, 
it needs to be registered in the DefaultActionBars.xml file.   Dynamic registration
of preferences associated with the components will allow for easier maintenance 
as these certain components are created as "plugins".



Code Structure
====

ATAK is comprised of 3 Android projects.

      ATAKMapEngine    
          - Changes will require CCB and coordination with Chris L.
     
      MapCoreInterfaces   
          - Changes will require CCB and coordination with Chris L/Shawn B. 

      ATAK 
          - Changes are driven by requirements and may require CCB


ATAK has additional directories 

      plugins - store source code for plugins that can be installed optionally.

      tools - various tools and android projects used in conjunction with ATAK,
              ATAK development, or ATAK testing.

      doc - documenation relevant to developers.

Plugins
===

Plugins for ATAK are additional Android Applications that are built but cannot be run stand alone.   These plugins are built so that at runtime they rely on the internal classes within the ATAK application.  This is done through a special classloader that knows how to join up the missing class files from the plugin application at run time.    During compile time, the plugin only needs to reference the classes using the provided keyword.

A good example of a plugin that does several different tasks is the Helloworld plugin.   This plugin is structured to be very minimalistic and contains a single user experience.   The plugin describes the capabilitities within the assets/plugin.xml file under the plugins/helloworld directory.    The java src code contains comment to guide the developer through the mechanics of how a plugin works.
 


Formatting
====

Source code within the ATAK repository is formatted using a set of Eclipse rules that exist in the
root of the repository.   In order to use these rules with Android Studio, we require the installation
of the EclipseCodeFormatter plugin.   Once installed, the required configuration files are all set up.
Within Android Studio

   Settings->
          Plugins->
              Type in Eclipse in the Search Box
              Tap Browse Repository Button
              Select the Eclipse Code Formatter Plugin 
       Once Android Studio restarts, then you should be able to perform normal code formatting per the 
       ATAK formatting rules.


Plugins
====


The plugin architecture is no different than developing code internally to ATAK.   
In fact, all ATAK components use the same constructs and capabilities.    There is 
not a single document that could cover the entire capabilities of the plugin
architecture, but there are several examples that show how to do basic capabilities.


A plugin can be produced that will work with a release version provided you have
the exact source code used to produce the release.   To do this you will need to
use the Makefile's provided in the ATAK root directory and in the specific plugin
directory.
     1) Check out the exact svn version for the source code of the release -
        this usually lives on a branch.
     2) run make release
          -- this only produces a mapping file used in step 4 and will not
          -- produce a proper build.
     3) update the specific plugin directory
     4) run: make release for that plugin.
          -- produces a plugin that makes use of the proper mapping.


An example plugin can be found under plugins/helloWorldPlugin which demonstrates
some of the characteristics of the plugin architecture.  

Plugin Structure
===

The assets/plugin.xml contains the information required to load a plugin.
Plugins may implement Lifecycle and/or Tools.   In the helloworld example, the 
plugin.xml file contains both.

By convention the lifecycle and tool are mostly boilerplate code and can be 
mostly duplicated from one project to another.   In the ATAK architecture, a 
Lifecycle closely relates to a MapComponent.  A Tool is used to populate the 
action bar and describe the intent that is fired by invoking the icon on the 
actionbar.

When developing in the plugin architecture, it is important to remember that 
there are two context's in use.   The primary context is the ATAK context and
should be used when visual components such as AlertDialog is being constructed.   
The secondary context is the plugin's context and should be used when looking up
resources and graphics specific to the plugin.  Keep this in mind during 
development.   Using the wrong context can lead to runtime crashes or wrong 
visual behavior.

A few notes:

1) Notifications cannot reference a resource from the plugin.  The implementation 
of the Notification class is not capable of realizing where to look up a resource.
In the helloworld plugin, there is an example of how to have your plugin display an
appropriate small icon.

2) Spinners will crash on devices depending how the theme is set.   For this purpose,
ATAK provides a com.atakmap.android.gui.PluginSpinner class which is identical 
to a real android Spinner.

3) Signing a Plugin -  If you choose to develop a plugin, the key alogrithm must be RSA and the Signing Algorithm must be SHA1withRSA.



Useful Information
====

1) Filing a bug with a screen capture.    
     There are many ways to capture the screen for bug submission.  By default
     a large png file is produced.   If you have access to adb and would like 
     to capture directly to a jpg, you can execute:

      unix - 
        adb shell screencap -j -p /sdcard/screen.jpg && adb pull /sdcard/screen.jpg
      windows - 
        adb shell screencap -j -p /sdcard/screen.jpg 
        adb pull /sdcard/screen.jpg

2) To record a video from the command line of the screen.
      adb shell screenrecord /sdcard/demo.mp4
      adb pull /sdcard/demo.mp4

     Stop the screen recording by pressing Ctrl-C, 
     otherwise the recording stops automatically at 
     three minutes or the time limit set by --time-limit.

Contact Information
====

Questions about the releasability of ATAK or technical issues relating to ATAK
development should be directed to the individuals listed below.

Government PM
===

Joshua Sterling
joshua.d.sterling.civ@mail.mil

Bryan Harris
bryan.s.harris.civ@mail.mil

Technical Contacts
====

Todd Krokowski (PAR Government)
todd_krokowski@partech.com

Chris Lawrence (PAR Government)
chris_lawrence@partech.com

Shawn Bisgrove (PAR Government)
shawn_bisgrove@partech.com

Jeff Downs (PAR Government)
jeff_downs@partech.com

Brian Young (ARA)
byoung@otis.onmicrosoft.com

Matt Gillen (BBN)
mgillen@bbn.com


Additional Resources
====

ATAK Main Page: 

ATAK Jira: https://jira.pargovernment.net/

ATAK Resource Page: 

ATAK Wiki: 

