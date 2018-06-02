#!/bin/bash

git -C libdlt pull
git -C wildfire-dln pull

echo "idms IP : `hostname --ip-address`"
idms -u http://wdln-base:9000 -H wdln-idms -p 9001 -d $DEBUG \
     -D .dlt -v http://wdln-base -q 42424
