from collections import namedtuple

Policy = namedtuple('Policy', ['desc', 'sendto', 'meta'])
class Policy(object):
    def __init__(self, desc, sendto, meta=None):
        self.desc = desc
        self.sendto = sendto
        self.meta = meta or {}
    
    def match(self, exnode):
        # Logical ops
        def _and(n,v):
            return all(_comp(k, n) for k in v)
        def _or(n,v):
            return any(_comp(k, n) for k in v)
        def _not(n,v):
            return not _comp(k, n)
        
        # Comparitors
        def _in(n,v):
            test = getattr(exnode, n, None)
            return test is not None and test in v
        def _gt(n,v):
            test = getattr(exnode, n, None)
            return test is not None and test > v
        def _gte(n,v):
            test = getattr(exnode, n, None)
            return test is not None and test >= v
        def _lt(n,v):
            test = getattr(exnode, n, None)
            return test is not None and test < v
        def _lte(n,v):
            test = getattr(exnode, n, None)
            return test is not None and test <= v
        def _comp(v, ctx):
            lmap = {"$or": _or, "$and": _and, "$not": _not}
            cmap = {"$in": _in, "$gt": _gt, "$lt": _lt, "$gte": _gte, "$lte": _lte}
            result = True
            for n,v in v.items():
                fn = lmap.get(n, cmap.get(n, None))
                if fn:
                    result &= fn(ctx,v)
                else:
                    if isinstance(v, dict):
                        return _comp(v, n)
                    test = getattr(exnode, n, None)
                    result &= test is not None and test == v
            return result
        
        return _comp(self.desc, None)
    def to_JSON(self):
        return {"description": self.desc,
                "destination": self.sendto,
                "metadata": self.meta}

