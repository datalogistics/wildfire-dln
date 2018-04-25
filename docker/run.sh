#!/bin/bash

sudo bash -c 'cat <<EOF >> /etc/hosts
`hostname --ip-address`   wdln-base-00
EOF'

sudo ibp_server -d /usr/local/etc/ibp/ibp.cfg
get_version wdln-base-00

sudo /etc/init.d/mongodb start
sudo /etc/init.d/redis-server start

cd dlt-web
pm2 start --name dlt-web server.js
cd -

echo "wdln-base-00 IP : `hostname --ip-address`"
periscoped --port=9000 -d DEBUG
