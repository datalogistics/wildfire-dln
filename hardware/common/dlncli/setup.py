#!/usr/bin/env python
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

import sys
from setuptools import setup

version = "1.0"

sys.path.append(".")
if sys.version_info[0] < 3: 
    print("------------------------------")
    print("Must use python 3.0 or greater", file=sys.stderr)
    print("Found python version ", sys.version_info, file=sys.stderr)
    print("Installation aborted", file=sys.stderr)
    print("------------------------------")
    sys.exit()

setup(
    name = "dlncli",
    version = version,
    author = "Jeremy Musser",
    author_email="jemusser@indiana.edu",
    license="http://www.apache.org/licenses/LICENSE-2.0",
    packages = ["dln"],
    install_requires=[
        "netifaces",
        "npyscreen"
    ],
    entry_points = {
        'console_scripts': [
            'dln = dln.cli:main'
        ]
    },
)
