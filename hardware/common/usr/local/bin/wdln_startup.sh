#!/bin/bash

if [ -f /var/lib/mongodb/mongod.lock ]; then
	rm -f /var/lib/mongodb/mongod.lock
fi
