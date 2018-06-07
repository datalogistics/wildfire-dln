from idms.lib import assertions
from idms.lib.assertions.abstract import AbstractAssertion
from idms.lib.assertions.exceptions import SatisfactionError

class Conjunction(AbstractAssertion):
    tag = "$and"
    def initialize(self, policies):
        self._ls = [assertions.factory(p) for p in policies]

    def apply(self, exnode, runtime):
        change = False
        for policy in self._ls:
            change |= policy.apply(exnode, runtime)
        return change
