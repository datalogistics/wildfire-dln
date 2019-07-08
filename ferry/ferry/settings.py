UNIS_URL="http://localhost:8888"

LOCAL_UNIS_HOST="localhost"
LOCAL_UNIS_PORT=9000

GPS_DEFAULT = [39.16533, -86.52639] # Bloomington, IN
GPS_BOX = [32.702719, -117.170799,
           32.712358, -117.156406]  # San Diego CC

# seconds between service/node updates
UPDATE_INTERVAL = 5

# number of retries before re-registering to UNIS
RETRY_COUNT = 2

# WDLN schema
GEOLOC="http://unis.crest.iu.edu/schema/ext/dln/1/geoloc#"
FERRY_SERVICE="http://unis.crest.iu.edu/schema/ext/dln/1/ferry#"

# Where is the IBP configuration file
IBP_CONFIG="/usr/local/etc/ibp/ibp.cfg"

# File handling
DOWNLOAD_DIR="/depot/web"
UPLOAD_DIR="/depot/upload"

# File HTTP endpoint
UPLOAD_PORT=8080
