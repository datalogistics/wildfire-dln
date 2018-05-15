#!/bin/bash

HOSTNAME=`hostname`

sudo sed -i "s/__HOSTNAME__/${HOSTNAME}/" /usr/local/etc/ibp/ibp.cfg
sudo ibp_server -d /usr/local/etc/ibp/ibp.cfg
get_version $HOSTNAME

sudo /etc/init.d/mongodb start
sudo /etc/init.d/redis-server start

git -C unis pull
git -C dlt-web pull

cd dlt-web
pm2 start --name dlt-web server.js
cd -

echo "Base Station IP : `hostname --ip-address`"
periscoped --port=9000 -d $DEBUG
