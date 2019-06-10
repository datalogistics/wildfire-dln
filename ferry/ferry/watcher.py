import time
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
from ferry.log import log
from ferry.settings import UPLOAD_DIR

class UploadWatcher:
    def __init__(self, rt, wdir=UPLOAD_DIR ):
        self.rt = rt
        self.wdir = wdir

        try:
            self.observer = Observer()
            self.event_handler = Handler()
            self.observer.schedule(self.event_handler, self.wdir, recursive=True)
            self.observer.start()
        except Exception as e:
            log.error("Could not start upload watcher: {} [{}]".format(e, self.wdir))

    def stop():
        self.observer.stop()
        self.observer.join()

class Handler(FileSystemEventHandler):
    @staticmethod
    def on_any_event(event):
        if event.is_directory:
            return None

        elif event.event_type == 'created':
            log.info("Received created event: %s" % event.src_path)

        elif event.event_type == 'modified':
            log.info("Received modified event: %s" % event.src_path)
