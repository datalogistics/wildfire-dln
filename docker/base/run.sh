#!/bin/bash

HOSTNAME=`hostname`

sudo sed -i "s/__HOSTNAME__/${HOSTNAME}/" /usr/local/etc/ibp/ibp.cfg
sudo sed -i "s/^minfree_size.*/minfree_size = 100/" /usr/local/etc/ibp/ibp.cfg
sudo ibp_server -d /usr/local/etc/ibp/ibp.cfg
get_version $HOSTNAME

git -C unis pull
git -C unisrt pull
git -C wildfire-dln pull

git -C dlt-web stash
git -C dlt-web pull
git -C dlt-web stash pop

sudo /etc/init.d/mongodb start
sudo /etc/init.d/redis-server start
sudo /etc/init.d/supervisor start

cd dlt-web
pm2 start --name dlt-web server.js
cd -

echo "Base Station IP : `hostname --ip-address`"

while [ ! -f /var/log/base.log ]
do
    sleep 1
done
tail -f /var/log/base.log
