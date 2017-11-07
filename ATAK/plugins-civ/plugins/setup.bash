#!/bin/bash

#
# Set ANDROID_HOME to build plugins
# call this using:
#
# $ . setup.bash
#
# adjust sdk variable below as necessary
#

sdk="$HOME/Android/Sdk"
if [ -d $sdk ]; then
	export ANDROID_HOME="$sdk";
	echo "ANDROID_HOME set to $sdk"
else
	echo "Set ANDROID_HOME before building."
fi
