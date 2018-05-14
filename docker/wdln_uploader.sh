#!/bin/bash

UPLOAD_DIR=/tmp/wdln

docker run --net wdln -v ${UPLOAD_DIR}:/wdln:ro -it wdln-uploader
