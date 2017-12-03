from __future__ import print_function
from setuptools import setup, Command
import os

import sys

version = "0.1.dev0"

sys.path.append(".")
if sys.version_info[0] < 3:
    print("------------------------------")
    print("Must use python 3.0 or greater", file=sys.stderr)
    print("Found python verion ", sys.version_info, file=sys.stderr)
    print("Installation aborted", file=sys.stderr)
    print("------------------------------")
    sys.exit()

setup(
    name="idms",
    version=version,
    packages=["idms"],
    author="Jeremy Musser, Ezra Kissel",
    license="http://www.apache.org/licenses/LICENSE-2.0",
    
    dependency_links=[
        "git+https://github.com/periscope-ps/lace.git/@master#egg=lace",
        "git+https://github.com/periscope-ps/unisrt.git/@multisource#egg=unisrt",
    ],
    install_requires=[
        "falcon>=1.3.0",
        "bson",
        "unisrt",
        "lace"
    ],
    entry_points = {
        'console_scripts': [
            'idms = idms.app:main'
        ]
    }
)
