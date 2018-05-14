#!/bin/bash

PREFIX=/opt

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
