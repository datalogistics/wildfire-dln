import ferry.settings as settings
from gps3 import gps3
from ferry.log import log

GPS_DEV_READ_LEN=50 # number of lines of output to read from gps3

# for retrieving GPS coordinates via gps3
class GPS:
    sock = None
    stream = None
    read_count = 0
    
    def __init__(self):
        try:
            sock = gps3.GPSDSocket()
            stream = gps3.DataStream()
            sock.connect()
            sock.watch()
        except Exception as e:
            log.info("Could not initialize GPS: {}".format(e))

    def query(self):
        if not self.sock:
            return (settings.GPS_DEFAULT[0], settings.GPS_DEFAULT[1])
        
        lack_lat = True
        lack_long = True
        lack_alt = True
        
        for new_data in self.sock:
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
                log.info('Ferry location identified as %f,%f' % (latitude,longitude))
                return (latitude,longitude)
            
            if self.read_count > GPS_DEV_READ_LEN:
                break

            # only return default if socket is dead
            # otherwise location will jump if GPS signal is lost
            return (None,None)
