#!/bin/bash

/etc/init.d/ibp-server stop

rm -rf /depot/db/*
rm -rf /depot/ibp_resources/*
rm -rf /tmp/ibp_dbenv

mkfs.resource 1 dir /depot/ibp_resources /depot/db
chown -R ibp:ibp /depot/db
chown -R ibp:ibp /depot/ibp_resources

/etc/init.d/ibp-server start

sleep 2;

get_version `hostname`
