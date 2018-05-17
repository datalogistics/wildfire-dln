#!/bin/bash

if [ $# -eq 0 ]
then
    echo "Usage: $0 [wdln-base|wdln-ferry-00|...]"
    exit 1
fi

TARGET=$1
UPLOAD_DIR=/tmp/wdln

docker run --net wdln -v ${UPLOAD_DIR}:/wdln:ro -it -e TARGET=${TARGET} wdln-uploader
