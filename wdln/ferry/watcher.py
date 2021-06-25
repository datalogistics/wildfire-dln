import time, libdlt, socket, os
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
from wdln.ferry.log import log
from wdln.settings import UPLOAD_DIR, DOWNLOAD_DIR

class UploadWatcher:
    def __init__(self, service, local_unis, wdir=UPLOAD_DIR):
        self.wdir = wdir

        try:
            self.observer = Observer()
            self.event_handler = Handler(service, local_unis)
            self.observer.schedule(self.event_handler, self.wdir, recursive=True)
            self.observer.start()
        except Exception as e:
            log.error("Could not start upload watcher: {} [{}]".format(e, self.wdir))

    def stop():
        self.observer.stop()
        self.observer.join()

class Handler(FileSystemEventHandler):
    def __init__(self, service, local_unis):
        self._s, self._local = service, local_unis
    
    def on_any_event(self, event):
        if event.is_directory:
            return None

        if event.event_type == 'moved':
            if not os.path.basename(event.src_path).startswith("."):
                return
            
            fname = os.path.basename(event.src_path)
            dname = os.path.dirname(event.src_path)
            npath = os.path.join(dname, fname[1:])
            dpath = os.path.join(DOWNLOAD_DIR, fname[1:])
            if os.path.getsize(npath) > 0:
                log.info("Handling new file upload: %s" % npath)
                self._do_upload(npath)
                os.rename(npath, dpath)
            
    def _do_upload(self, src):
        LOCAL_DEPOT={"ibp://{}:6714".format(socket.getfqdn()): { "enabled": True}}
        try:
            with libdlt.Session(self._local, bs="5m", depots=LOCAL_DEPOT, threads=1) as sess:
                res = sess.upload(src)
        except ValueError as e:
            log.warn(e)
        if not hasattr(self._s, 'uploaded_exnodes'):
            self._s.extendSchema('uploaded_exnodes', [res.exnode])
        else:
            self._s.uploaded_exnodes.append(res.exnode)
        
