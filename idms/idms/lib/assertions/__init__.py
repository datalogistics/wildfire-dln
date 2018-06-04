from idms.lib.assertions.exceptions import AssertionError

assertions = {}
def register(assertion):
    assertions[assertion.tag] = assertion

def factory(self, desc):
    if desc[0] not in assertions:
        raise AssertionError("Bad assertion type - {}".format(desc[0]))
    return assertions[desc[0]](desc[1])
