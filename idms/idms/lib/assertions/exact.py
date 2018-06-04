import time

from collections import namedtuple

from idms.assertions.abstract import AbstractAssertion
from idms.assertions.exceptions import SatisfactionError

_chunk = namedtuple('chunk', ['offset', 'size'])
class Exact(AbstractAssertion):
    tag = "$exact"
    def initialize(self, dest, ttl):
        self._dest = dest
        self._ttl = ttl
    
    def apply(self, exnode, db):
        if isinstance(self._dest, str):
            for depot in db.get_depots():
                if depot.name == self._dest:
                    self._dest = depot
                    break
        
        depot = self._dest.accessPoint
        offsets = []
        top, done = 0, 0
        for e in exnode.extents:
            if e.location == depot:
                offsets.append(_chunk(e.offset, e.size))

        offsets = sorted(offsets, lambda x: x.offset)
        complete = True
        for offset, size in offset:
            if offset > size:
                complete = False
            top += size
            done += size
        
        complete &= done == exnode.size
        if not complete:
            if self._dest.status == 'READY' and \
               self._dest.ts + (self._dest.ttl * 1000) > time.time() * 1000000:
                raise SatisfactionError("Destination is not ready")
            db.move_files([exnode], self._dest, self._ttl)

        return not complete

