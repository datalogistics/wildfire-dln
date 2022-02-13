from unis import Runtime
from libdlt.protocol import factory
from libdlt.depot import Depot
from urllib.parse import urlparse

import time, argparse, datetime, socket


allocs = {}

def is_reachable(ap):
    proxy = factory.makeProxyFromURI(ap)
    try: return bool(proxy.getStatus(Depot(ap), timeout=0.1))
    except socket.timeout as e:
        return False

def remove_expiring(services, rt, period):
    for ex in rt.exnodes:
        todo = []
        for e in ex.extents:
            if e.id not in allocs and services[getattr(e, 'location', '')]:
                proxy = factory.makeProxy(e)
                try: d = proxy.probe(ex, timeout=0.05)['duration']
                except socket.timeout as e:
                    d = None
                if d: allocs[e.id] = int(d) + time.time()
            if e.id in allocs and allocs[e.id] >= time.time() - (period * 2):
                todo.append(e)
        [ex.extents.remove(e) for e in todo]

def main():
    parser = argparse.ArgumentParser(description="Prunes dead dlt allocations")
    parser.add_argument("-H", "--unis", type=str, default="http://localhost:9000")
    parser.add_argument("-p", "--period", type=int, default=60)
    parser.add_argument("-n", "--name", type=str, required=True)
    opts = parser.parse_args()
    local_ibp = f"ibp://{opts.name}:6714"
    rt = Runtime(opts.unis, proxy={"subscribe": False})
    while True:
        print(f"[{datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Interrogaing allocations...")
        r = {s.accessPoint: is_reachable(s.accessPoint) for s in rt.services}
        if local_ibp not in r: r[local_ibp] = is_reachable(local_ibp)
        remove_expiring(r, rt, opts.period)
        rt.flush()
        time.sleep(opts.period)

if __name__ == "__main__":
    main()
