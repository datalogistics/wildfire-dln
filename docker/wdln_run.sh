#!/bin/bash

docker run --name wdln-base --network wdln --hostname wdln-base -it -d wdln-base
docker run --name wdln-ferry-00 --network wdln --hostname wdln-ferry-00 -it -d wdln-ferry
docker run --name wdln-ferry-01 --network wdln --hostname wdln-ferry-01 -it -d wdln-ferry
#docker run --name wdln-idms --network wdln --hostname wdln-idms -it -d wdln-idms

echo
echo

docker network inspect --format='{{range .Containers}}{{.IPv4Address}} {{println .Name}}{{end}}' wdln | sed 's/\/16//g'
