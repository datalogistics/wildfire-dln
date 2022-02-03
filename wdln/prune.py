from unis import Runtime
from libdlt.protocol import factor
from urllib.parse import urlparse

import time, argparse, liburl

def main():
    parser = argparse.ArgumentParser(description="Prunes dead dlt allocations")
    parser.add_argument("-H", "--unis", type=str, default="http://localhost:9000")
    parser.add_argument("-p", "--period", type=int, default=60)
    parser.add_argument("-n", "--name", type=str, required=True)
    opts = parser.parse_args()
    while True:
        rt = Runtime(opts.unis, proxy={"subscribe": False})
        extents, seen = [ex for ex in rt.extents]
        for ex in extents:
            if urlparse(ex.location).netloc.startswith(opts.name):
                proxy = factory.makeProxy(ex)
                try: d = proxy.probe(ex, timeout=0.025)['duration']
                except Exception as e:
                    rt.delete(ex)
                if d <= opts.period * 2:
                    rt.delete(ex)
        time.sleep(opts.period)

if __name__ == "__main__":
    main()
