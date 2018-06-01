import io
import random
from ferry.settings import GPS_DEFAULT, GPS_BOX
from gps3 import gps3
from ferry.log import log
from shapely.geometry import box, Point
from contextlib import redirect_stderr

GPS_DEV_READ_LEN=50 # number of lines of output to read from gps3

# for retrieving GPS coordinates via gps3
class GPS:
    sock = None
    stream = None
    gps_box = None
    read_count = 0
    
    def __init__(self):
        self.sock = gps3.GPSDSocket()
        self.stream = gps3.DataStream()
        
        # GPS3 throws no exceptions and returns no status,
        # so we have to capture stdout and do string matching, sigh
        f = io.StringIO()
        with redirect_stderr(f):
            self.sock.connect()
        if "Connection refused" in f.getvalue():
            log.warn("Could not connect to GPSD socket, continuing with GPS_BOX")
            self.sock = None
        else:
            log.info("Connected to GPSD socket")
            self.sock.watch()
            
        # we will use the GPS_BOX setting to determine if
        # queries will return locations within a specified range
        if len(GPS_BOX) == 4:
            self.gps_box = box(GPS_BOX[0], GPS_BOX[1],
                               GPS_BOX[2], GPS_BOX[3])
            log.info("Created GPS box")
        else:
            log.warn("Invalid GPS_BOX size, using GPS_DEFAULT")
            
    def query(self):
        if not self.sock:
            if self.gps_box:
                min_x, min_y, max_x, max_y = self.gps_box.bounds
                pt = Point([random.uniform(min_x, max_x), random.uniform(min_y, max_y)])
                log.info('Location identified as %f,%f' % (pt.x, pt.y))
                return (pt.x, pt.y)
            return (GPS_DEFAULT[0], GPS_DEFAULT[1])
        
        lack_lat = True
        lack_long = True
        lack_alt = True
        
        for new_data in self.sock:
            latitude = 'n/a'
            longitude = 'n/a'
            altitude = 'n/a'
            
            if new_data:
                self.stream.unpack(new_data)
                self.read_count = self.read_count + 1
                
                latitude = self.stream.TPV['lat']
                longitude = self.stream.TPV['lon']
                altitude = self.stream.TPV['alt']
                
            if lack_lat and latitude != 'n/a' and type(latitude) == float:
                latitude = float(latitude)
                lack_lat = False
                
            if lack_long and longitude != 'n/a' and type(longitude) == float:
                longitude = float(longitude)
                lack_long = False
                                
            if lack_alt and altitude != 'n/a' and type(altitude) == float:
                altitude = float(altitude)
                lack_alt = False
                        
            if not lack_lat and not lack_long: # optional: and not lack_alt
                log.info('Location identified as %f,%f' % (latitude,longitude))
                return (latitude,longitude)
            
            if self.read_count > GPS_DEV_READ_LEN:
                break

            # only return default if socket is dead
            # otherwise location will jump if GPS signal is lost
            return (None,None)
