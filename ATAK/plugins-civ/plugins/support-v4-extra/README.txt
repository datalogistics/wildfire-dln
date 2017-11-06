The ATAK project makes use of a subset of the android-support-v4 library and as 
such, the stripped library is labeled android-support-v4-slim.   The was derived 
from the Android Support Library, revision 19.0 (October 2013) downloaded from 
https://dl-ssl.google.com/android/repository/support_r19.zip

The missing classes are provided in a jar file in this directory called 
android-support-v4-noncore.jar and can be used by plugin developers looking to 
utilize features not found in the slim version.   

Some developers are using other android support libraries (recycler view for 
example) that will not work with the slim version and would require the plugin 
to put the noncore jar file in the lib directory.

Please do not use the android-support-v4-19 library in this directory.

android-support-v4-slim is provided directly by ATAK.
android-support-v4-noncore can be added to individual plugins as needed.


The summation of the two library is equal to android-support-v4.jar revision 19.0.

