import time

from collections import defaultdict
from shapely.geometry import Polygon, Point

from idms.lib.assertions.abstract import AbstractAssertion
from idms.lib.assertions.exceptions import SatisfactionError

class GeoFense(AbstractAssertion):
    tag = "$geo"
    def initialize(self, poly, ttl):
        self._fense = Polygon(poly)
        self._ttl = ttl
    
    def apply(self, exnode, db):
        valid_depots = set()
        for depot in db.get_depots():
            loc = depot.runningOn.location
            if hasattr(loc, 'latitude') and hasattr(loc, 'longitude'):
                if self._fense.contains(Point(loc.latitude, loc.longitude)) and \
                   depot.status == 'READY' and \
                   depot.ts + (depot.ttl * 1000000) > time.time() * 1000000:
                    valid_depots.add(depot.accessPoint)

        chunks = defualtdict(lambda: 0)
        for e in exnode.extents:
            if e.location in valid_depots:
                chunks[e.offset] = max(chunks[e.offset], e.size)

        offset = 0
        complete = True
        while offset < exnode.size:
            try:
                offset += chunks[offset]
            except KeyError:
                for k,v in chunks.items():
                    if k < offset and k + v > offset:
                        offset += (k + v - offset)
                        continue
                complete = False
                break

        if not complete:
            if not valid_depots:
                raise SatisfactionError("No depots found within the fense")
            db.move_files([exnode], valid_depots.pop(), self._ttl)
        return not complete

