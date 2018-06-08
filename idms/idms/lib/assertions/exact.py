import time

from collections import namedtuple

from idms.lib.assertions.abstract import AbstractAssertion
from idms.lib.assertions.exceptions import SatisfactionError

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
            if isinstance(self._dest, str):
                raise SatisfactionError("Currently unknown destination")
        
        depot = self._dest.accessPoint
        offsets = []
        done = 0
        for e in exnode.extents:
            if e.location == depot:
                offsets.append(_chunk(e.offset, e.size))

        offsets = sorted(offsets, key=lambda x: x.offset)
        complete = True
        for offset, size in offsets:
            if offset > done:
                complete = False
            done += size
        
        complete &= done == exnode.size
        if self._dest.status != 'READY' and self._dest.status != 'UPDATE':
            raise SatisfactionError("Destination is not ready [{}]".format(self._dest.status))
        elif self._dest.ts + (self._dest.ttl * 1000000) < time.time() * 1000000:
            raise SatisfactionError("Destination has not checked in in", self._dest.ttl)
        if not complete:
            db.move_files([exnode], self._dest, self._ttl)

        return not complete

