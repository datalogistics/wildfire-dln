MAJOR_VERSION=1
MINOR_VERSION=0
INC_VERSION=  2

LOCAL_UNIS_HOST="localhost"
LOCAL_UNIS_PORT="9000"

GPS_DEFAULT = [39.16533, -86.52639] # Bloomington, IN
GPS_BOX = [32.702719, -117.170799,
           32.712358, -117.156406]  # San Diego CC

# number of retries before re-registering to UNIS
RETRY_COUNT = 2

# WDLN schema
GEOLOC="http://unis.crest.iu.edu/schema/ext/dln/1/geoloc#"
FERRY_SERVICE="http://unis.crest.iu.edu/schema/ext/dln/1/ferry#"

# Where is the IBP configuration file
IBP_CONFIG="/usr/local/etc/ibp/ibp.cfg"

# File handling
DOWNLOAD_DIR="/depot/web"
UPLOAD_DIR="/depot/web"

# File HTTP endpoint
UPLOAD_PORT=8080

DEFAULT_BASE_CONFIG={
    "localonly": True,
    "remote": {
        "host": "localhost",
        "port": LOCAL_UNIS_PORT
    },
    "name": None
}

DEFAULT_FERRY_CONFIG={
    "name": None,
    "localonly": False,
    "ibp": False,
    "remote": {
        "host": "localhost",
        "port": "8888"
    },
    "local": {
        "host": None,
        "port": LOCAL_UNIS_PORT
    },
    "file": {
        "download": DOWNLOAD_DIR,
        "upload": UPLOAD_DIR,
        "port": UPLOAD_PORT
    },
    "engine": {
        "interval": 5,
        "maxfail": 2
    }
}
