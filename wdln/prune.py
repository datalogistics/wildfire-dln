from unis import Runtime
from libdlt.protocol import factory
from urllib.parse import urlparse

import time, argparse, datetime

def main():
    parser = argparse.ArgumentParser(description="Prunes dead dlt allocations")
    parser.add_argument("-H", "--unis", type=str, default="http://localhost:9000")
    parser.add_argument("-p", "--period", type=int, default=60)
    parser.add_argument("-n", "--name", type=str, required=True)
    opts = parser.parse_args()
    while True:
        print(f"[{datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Interrogaing allocations...")
        rt = Runtime(opts.unis, proxy={"subscribe": False})
        extents = [ex for ex in rt.extents]
        for ex in extents:
            if urlparse(getattr(ex, 'location', "")).netloc.startswith(opts.name):
                proxy = factory.makeProxy(ex)
                try: d = proxy.probe(ex, timeout=0.025)['duration']
                except Exception as e:
                    rt.delete(ex)
                if d <= opts.period * 2:
                    print("    Prunning ex.id")
                    rt.delete(ex)
        time.sleep(opts.period)

if __name__ == "__main__":
    main()
