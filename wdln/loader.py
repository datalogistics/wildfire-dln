from lace import logging
from libdlt.util.files import ExnodeInfo
from libdlt.file import DLTFile
from unis import Runtime
from unis.models import Exnode
from wdln.config import MultiConfig
from wdln.settings import LOADER_CONFPATH
import falcon, datetime, time, socket

#TODO Logging

TEMPLATE = """
<html><head><title>Index of /files/</title></head><body><h1>Index of /files/</h1><hr><pre><a href="../">../</a>
{body}</pre><hr></body></html>
"""

log = logging.getLogger('wdln.loader')
class ListFiles(object):
    def __init__(self, uri, stage, bs, **kwargs):
        self._uri = uri
        self._bs = int(bs)
        self._stage = stage

    def _filelist(self):
        rt = Runtime(self._uri, proxy={"subscribe": False})
        files = []
        for ex in rt.exnodes:
            info = ExnodeInfo(ex, remote_validate=False)
            if info.is_complete():
                ts = ex.modified / 1000000
                files.append({
                    "id": ex.id,
                    "name": ex.name,
                    "size": ex.size,
                    "date": datetime.datetime.fromtimestamp(ts).strftime("%d-%b-%Y %H:%M")
                })
        return files

    def on_post(self, req, resp):
        rt = Runtime(self._uri, proxy={"subscribe": False})
        form = req.get_media()
        for part in form:
            if part.filename:
                ex = Exnode({'name': part.secure_filename, 'mode': 'file',
                           'created': int(time.time() * 1000000),
                           'modified': int(time.time() * 1000000),
                           'parent': None,
                           'owner': 'wdln',
                           'group': 'wdln',
                           'permission': '744'})
                try:
                    with DLTFile(ex, "w", bs=self._bs,
                                 dest=self._stage) as dest:
                        part.stream.pipe(dest)
                except AllocationError as exp:
                    resp.status = falcon.HTTP_500
                    resp.text = {"errorcode": 1, "msg": esp.msg}
                rt.insert(ex, commit=True)
                for a in ex.extents:
                    rt.insert(a, commit=True)
                rt.flush()
        resp.status = falcon.HTTP_201
        resp.text = {"fid": ex.id, "status": "GOOD", "size": ex.size}

    def on_get(self, req, resp):
        def line(x):
            dpad = " " * (51 - len(x["name"]))
            spad = " " * (20 - len(str(x["size"])))
            return f'<a href="{x["id"]}">{x["name"]}</a>{dpad}{x["date"]}{spad}{x["size"]}'
        body = "\n".join([line(x) for x in self._filelist()])
        resp.status = falcon.HTTP_200
        resp.content_type = falcon.MEDIA_HTML
        resp.text = TEMPLATE.format(body=body)

class GetFile(object):
    def __init__(self, uri):
        self._uri = uri

    def on_get(self, req, resp, rid):
        e = Runtime(self._uri, proxy={"subscribe": False}).exnodes.first_where({"id":fileid})
        if not e: raise falcon.HTTPBadRequest(description="Unknown exnode id")

        resp.downloadable_as = ex.name
        resp.content_length = ex.size
        resp.content_type = mimetypes.guess_type(ex.name)[0]

        with DLTFile(ex, "r") as stream:
            resp.stream = stream

conf = MultiConfig({"port": "8000", "uri": "http://localhost:9000", "bs": 1024 * 1024 * 20, "stage": socket.gethostname()},
                   "File loader for WDLN remote agents",
                   filepath="/etc/dlt/loader.cfg", filevar=LOADER_CONFPATH)
conf = conf.from_file()
def main():
    app = falcon.App()
    app.add_route("/web/", ListFiles(**conf))
    app.add_route("/web/{fileid}", GetFile(conf['uri']))
    return app

app = main()
def run():
    print(conf)
    from wsgiref.simple_server import make_server
    with make_server('', int(conf['port']), app) as httpd:
        httpd.serve_forever()

if __name__ == "__main__":
    run()
