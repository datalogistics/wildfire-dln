#!/bin/bash

docker build --no-cache -t "wdln-base" base/
docker build --no-cache -t "wdln-ferry" ferry/
docker build --no-cache -t "wdln-idms" idms/
docker build --no-cache -t "wdln-uploader" uploader/

docker network create wdln
