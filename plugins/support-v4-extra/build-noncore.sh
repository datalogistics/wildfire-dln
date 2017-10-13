# This script is used to produce the noncore jar file based on the delta between
# the full android-support-v4-19.0.jar file and whatever slim is at the time this 
# is run.   The delta is then used to construct the android-support-v4-noncore 
# component which would be used by plugin developers.

#!/bin/sh
ANDROID_SUPPORT=android-support-v4-19.0.jar
SLIM_ANDROID_SUPPORT=../../ATAK/libs/android-support-v4-slim.jar
ANDROID_SUPPORT_NON_CORE=android-support-v4-noncore.jar


mkdir TMP
cd TMP
jar xvf ../$ANDROID_SUPPORT
for FILE in `jar tf ../$SLIM_ANDROID_SUPPORT`;do rm "$FILE";done
jar cvf ../$ANDROID_SUPPORT_NON_CORE .
cd ..
rm -fr TMP


