import importlib

from idms.lib.assertions.exceptions import AssertionError

_builtin = [
    "idms.lib.assertions.and.Conjunction",
    "idms.lib.assertions.or.Disjunction",
    "idms.lib.assertions.exact.Exact",
    "idms.lib.assertions.geofense.GeoFense",
    "idms.lib.assertions.replicate.Replicate"
]

assertions = {}
def register(assertion):
    if isinstance(assertion, str):
        path = assertion.split(".")
        module = importlib.import_module(".".join(path[:-1]))
        assertion = getattr(module, path[-1])
    assertions[assertion.tag] = assertion

def factory(desc):
    if desc[0] not in assertions:
        raise AssertionError("Bad assertion type - {}".format(desc[0]))
    return assertions[desc[0]](desc[1])

for path in _builtin:
    register(path)
