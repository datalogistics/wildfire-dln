import time

from collections import namedtuple, defaultdict

from idms.lib.assertions.abstract import AbstractAssertion
from idms.lib.assertions.exceptions import SatisfactionError

_chunk = namedtuple('chunk', ['loc', 'size'])
class Replicate(AbstractAssertion):
    tag = "$replicate"
    def initialize(self, copies, ttl):
        self._ttl = ttl
        self._copies = copies
    
    def apply(self, exnode, db):
        active_list = set()
        for depot in db.get_depots():
            if depot.status in ['READY', 'UPDATE'] and \
               depot.ts + (depot.ttl * 1000000) > time.time() * 1000000:
                active_list.add(depot.accessPoint)

        e_list = defaultdict(dict)
        used_list = set()
        for e in exnode.extents:
            used_list.add(e.location)
            if e.location in active_list:
                e_list[e.offset][e.location] = e.size

        offsets = [-1] * self._copies
        deltas = [0] * self._copies
        while any([offsets[i] != deltas[i] for i in range(self._copies)]):
            offsets = deltas.copy()
            for i in range(self._copies):
                offset = offsets[i]
                if not e_list[offset]:
                    continue
                loc = list(e_list[offset].keys())[0]
                deltas[i] += e_list[offset][loc]
                del e_list[offset][loc]

        new_copies = sum([1 for x in offsets if x < exnode.size])
        avail = active_list - used_list
        if len(avail) < new_copies:
            raise SatisfactionError("Too few depots available to satisfy replication")

        for _ in range(new_copies):
            db.move_files([exnode], avail.pop(), self._ttl)

        return not any([offset < exnode.size for offset in offsets])
