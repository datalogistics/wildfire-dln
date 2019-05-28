import logging
import lace

log = lace.logging.getLogger("ferry")
logging.basicConfig(format='[%(asctime)-15s] [%(levelname)s] %(message)s')
log.setLevel(logging.INFO)
