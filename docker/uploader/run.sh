#!/bin/bash

HOSTNAME=`hostname`

git -C unisrt pull
git -C libdlt pull

echo "Uploading to ${TARGET}..."

sed -i "/${TARGET}/{n;s/\"enabled\": false/\"enabled\": true/}" .depots

dlt_xfer -d ~/.depots -H http://${TARGET}:9000 -V http://wdln-base:42424 -u -r /wdln
