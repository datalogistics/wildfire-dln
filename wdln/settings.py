LOADER_CONFPATH="$WDLN_LOADER_CONFPATH"
AGENT_CONFPATH="$WDLN_FERRY_CONFIGPATH"

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

DEFAULT_FERRY_CONFIG={
    "name": None,
    "localonly": False,
    "ibp": False,
    "servicetype": "base",
    "remote": {
        "host": "localhost",
        "port": "8888"
    },
    "local": {
        "host": None,
        "port": "9000"
    },
    "engine": {
        "interval": 5,
        "maxfail": 2
    }
}
