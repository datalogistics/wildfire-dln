from idms.lib import assertion

class _AssertionType(type):
    def __init__(cls, name, bases, ns):
        super().__init__(name, bases, ns)
        assertion.register(cls)

class AbstractAssertion(metaclass=_AssertionType):
    def __init__(self, desc):
        self._idms_desc = desc
        self.initialize(**desc)
    def initialize(self):
        pass
    
    @property
    def tag(self):
        raise NotImplementedError()
    
    def apply(self, exnode, runtime):
        """
        Tests a resource change against the validation condition.
        :param exnode: Reference to a resource to validate
        :param runtime: The runtime continaing the resource
        
        :type exnode: Exnode
        :type runtime: Runtime
        :rtype: Boolean
        """
        raise NotImplementedError

    def to_JSON(self):
        return (self.tag, self._idms_desc)
