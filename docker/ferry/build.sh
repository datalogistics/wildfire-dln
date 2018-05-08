#!/bin/bash

PREFIX=/opt

echo "Setting up IBP Server..."
cd ibp_server
cmake .
make -j 4
sudo make install
sudo mkdir -p /depot/ibp_resources
sudo bash -c 'cat <<EOF > /usr/local/etc/ibp/ibp.cfg
[server]
user=root
group=root
pidfile=/var/run/ibp_server.pid
interfaces=__HOSTNAME__:6714;
port=6714
lazy_allocate=1
threads=16
log_file=/var/log/ibp_server.log
activity_file=/var/log/ibp_activity.log

EOF'
sudo bash -c 'mkfs.resource 1 dir /depot/ibp_resources /depot/db >> /usr/local/etc/ibp/ibp.cfg'
cd -

echo "Setting up Periscope..."
cd unis
sudo python2 setup.py develop
cd -

echo "Setting up UNIS-RT..."
cd lace
sudo python3 setup.py develop
cd -
cd unisrt
sudo python3 setup.py develop
cd -

echo "Setting up libdlt..."
cd libdlt
sudo python3 setup.py develop
cd -

echo "Setting up WDLN-Ferry..."
cd wildfire-dln/ferry
sudo python3 setup.py develop
cd -
