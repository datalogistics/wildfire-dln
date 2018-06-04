from lace import logging
from threading import Thread, Lock, Event

from idms.lib.assertions.exceptions import AssertionError

dirty = Event()
def run(db):
    log = logging.get_logger()
    def _loop():
        while True:
            dirty.wait()
            dirty.clear()
            log.info("Detected topology change, validating")

            try:
                for p in db.get_active_policies() if p.dirty:
                    try:
                        p.apply()
                    except AssertionError:
            except Exception as exp:
                log.error("Failure during policy application - {}".format(exp))

            status = [p.dirty for p in db.get_active_policies()]
            _print_status(status)

    runner = Thread(target=_loop, name="idms_engine", daemon=True)
    runner.start()

def _print_status(status):
    # print top
    print("\u250c", end='')
    for _ in range(len(status) - 1):
        print("\u2500\u2500\u2500", end='')
        print("\u252c", end='')
    print("\u2500\u2500\u2500", end='')
    print("\u2510")
    
    # print ids
    print("\u2502", end='')
    for i in range(len(status)):
        print("", i, "\u2502", end='')
    print()
    
    # print headerbottom
    print("\u251c", end='')
    for _ in range(len(status) - 1):
        print("\u2500\u2500\u2500", end='')
        print("\u253c", end='')
    print("\u2500\u2500\u2500", end='')
    print("\u2524")
    
    # print statuses
    print("\u2502", end='')
    for stat in status:
        result = "{color}{mark}{clear}".format(**{
            "color": "\033[0;32m" if stat else "\033[0;31m",
            "mark":"\u2b24" if stat else "\u2718",
            "clear": "\033[0m"
        })
        print("", result, "\u2502", end='')
    print()
    
    # print bottom
    print("\u2514", end='')
    for _ in range(len(status) - 1):
        print("\u2500\u2500\u2500", end='')
        print("\u2534", end='')
    print("\u2500\u2500\u2500", end='')
    print("\u2518")
