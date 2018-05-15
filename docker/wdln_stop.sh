#!/bin/bash

docker stop wdln-ferry-00 wdln-ferry-01 wdln-base wdln-idms
docker rm wdln-base wdln-idms wdln-ferry-00 wdln-ferry-01
