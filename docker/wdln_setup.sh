#!/bin/bash

docker build -t "wdln-base" base/
docker build -t "wdln-ferry" ferry/

docker network create wdln

