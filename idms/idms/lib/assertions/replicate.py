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
            if depot.status == 'READY' and depot.ts + (depot.ttl * 1000) > time.time() * 1000000:
                active_list.add(depot.accessPoint)

        e_list = defaultdict(dict)
        used_list = set()
        for e in exnode.extents:
            used_list.add(e.location)
            if e.location in active_list:
                e_list[e.offset][e.location] = e.size

        offsets = [-1] * self._copies
        deltas = [0] * self._copies
        while any([offets[i] != deltas[i] for i in range(self._copies)]):
            offsets = deltas
            for i in self._copies:
                if not e_list[i]:
                    continue
                loc = list(e_list[i].keys())[0]
                delta[i] += e_list[i][loc]
                del e_list[i][loc]

        new_copies = sum([1 for x in offsets if x < exnode.size])
        avail = active_list - used_list
        if len(avail) < new_copies:
            raise SatisfactionError("Too few depots available to satisfy replication")

        for new_copies:
            db.move_files([exnode], avail.pop(). self._ttl)

        return not any([offset < exnode.size for offset in offsets])
