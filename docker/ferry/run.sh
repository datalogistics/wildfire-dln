#!/bin/bash

HOSTNAME=`hostname`

sudo sed -i "s/__HOSTNAME__/${HOSTNAME}/" /usr/local/etc/ibp/ibp.cfg
sudo sed -i "s/^minfree_size.*/minfree_size = 100/" /usr/local/etc/ibp/ibp.cfg
sudo ibp_server -d /usr/local/etc/ibp/ibp.cfg
get_version $HOSTNAME

sudo mkdir -p /depot/web
sudo chown wdln:wdln /depot/web

git -C unis pull
git -C unisrt pull
git -C libdlt pull
git -C wildfire-dln pull

# re-apply setup for dev purposes
cd wildfire-dln/ferry
sudo python3 setup.py develop
cd -

sudo /etc/init.d/nginx start
sudo /etc/init.d/mongodb start
sudo /etc/init.d/redis-server start
sudo -E /etc/init.d/supervisor start

echo "Ferry $HOSTNAME IP : `hostname --ip-address`"
sleep 1
tail -f /var/log/ferry.log

