WxPlugin
_________________________________________________________________
PURPOSE AND CAPABILITIES

The WxReport plugin provides ATAK users with the capabilities to query for the weather forecast at a given location and add weather overlays to the map.

Note:  An Internet connection is required to do forecast query and to download weather overlay data.  When no internet connection is availible weather overlays may be displayed but they will not be updated until a connection with the internet is re-established.
_________________________________________________________________
STATUS

Released to SOMPE September 1st 2016

_________________________________________________________________
ATAK VERSIONS

ATAK 3.4

_________________________________________________________________
POINT OF CONTACTS

Primary Developer Contact:     Pat Stevenson.  pat_stevenson@partech.com / 919-925-8037 / PAR Government - SOMPE
PAR Contact:           Eric Donovan.  Eric_Donovan@partech.com / 919-285-5546 / PAR Government - SOMPE
SOMPE Program Office Contact:  Christopher Abbot.  christopher.d.abbott8.civ@mail.mil / 757-878-6659 / SOMPE 

_________________________________________________________________
USER GROUPS

SOF

_________________________________________________________________
EQUIPMENT REQUIRED

_________________________________________________________________
EQUIPMENT SUPPORTED

_________________________________________________________________
COMPILATION

helloword is a complete skeleton project that can be used as a starting point 
for developing ATAK private plugins.  


Private Plugins offer the most capability for utilizing the ATAK subsystem, but 
this interface will likely change from version to version.


build.xml and Makefile both reflect the same project name (in this case helloworld).

The assets file describes both a Lifecycle and a ToolDescriptor.   For convention,
these are in the same location used in the AndroidManifest.xml file.    For 
readability I have broken out the plugin to be in a directory off of the main 
package structure.

When constructing the plugin, it is important to recognize that there are two 
different android.content.Context in play.   

  The plugin context is used to resolve resources from the plugin APK
  The mapView context is used for graphic access (AlertDialogs, Toasts, etc).

Note:
   The plugin context will cause a runtime error to occur if used to construct an
   AlertDialog.

_________________________________________________________________
DEVELOPER NOTES

Note, it's possible to use the Wx plugin to view imagery, and use the transparency slider along with it

This can be done by downloading data from the Layers menu (saved as a .sqlite), then copying that map data to the Wx directory.

This could be helpful when comparing two map types, or overlaying a second map type over base map data.  