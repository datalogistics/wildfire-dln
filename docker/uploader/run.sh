#!/bin/bash

HOSTNAME=`hostname`

git -C unisrt pull
git -C libdlt pull

dlt_xfer -d ~/.depots -H http://wdln-base:9000 -u -r /wdln
