from unis import Runtime
from libdlt.util.files import ExnodeInfo
from libdlt.file import DLTFile
from lace import logging

import falcon, datetime, bottle, os
import libdlt

DB_HOST = "http://locahost:9000"

TEMPLATE = """
<html>
  <head>
    <title>Index of /files/</title>
  </head>
  <body>
    <h1>Index of /files/</h1>
    <hr>
    <pre>
      <a href="../">../</a>
      {body}
    </pre>
    <hr>
  </body>
</html>
"""

#TODO
# Convert this entire block to a falcon server
log = logging.getLogger("wdln.uploader")
def configure_upload_server(agent):
    @bottle.route('/flist')
    def _list():
        return repr(os.listdir(agent.cfg['file']['upload']))
    @bottle.route('/upload', method='POST')
    def _files():
        with libdlt.Session(agent.rt, bs="5m", depots={agent.service.accessPoint: { "enabled": True}}, threads=1) as sess:
            for f in bottle.request.files.keys():
                try:
                    dat = bottle.request.files.get(f)
                    path = os.path.join(agent.cfg['file']['upload'], dat.filename)
                    dat.save(path, overwrite=True)
                    res = sess.upload(path)
                    if not hasattr(agent.service, 'uploaded_exnodes'):
                        agent.service.extendSchema('uploaded_exnodes', [])
                    agent.service.uploaded_exnodes.append(res.exnode)
                except ValueError as e:
                    log.warning(e)

class ListFiles(object):
    def _filelist(self):
        rt = Runtime(DB_HOST, proxy={"subscribe": False})
        files = []
        for ex in rt.exnodes:
            info = ExnodeInfo(ex, remote_validate=True)
            if info.is_complete():
                files.append({
                    "id": ex.id,
                    "name": ex.name,
                    "size": ex.size,
                    "date": datetime.datetime.fromtimestamp(ex.modified).strftime("%d-%b-%Y %H:%M")
                })
        return files

    def on_get(self, req, resp):
        def line(x):
            dlen = " " * (52 - len(x["date"]))
            spad = " " * (20 - len(x["size"]))
            return f'<a href="{x["id"]}">{x["name"]}</a>{dpad}{x["date"]}{spad}{x["size"]}'
        body = "\n".join([line(x) for x in self._filelist()])
        resp.status = falcon.HTTP_200
        resp.content_type = falcon.MEDIA_HTML
        resp.text = TEMPLATE.format(body=body)

class GetFile(object):
    def on_get(self, req, resp, fileid):
        rt = Runtime(DB_HOST, proxy={"subscribe": False})
        ex = rt.exnodes.first_where({"id": fileid})
        if ex is None:
            resp.status = falcon.HTP_404
            resp.content_type = 'application/octet-stream'
            resp.data = "Bad filename"
        else:
            resp.status = falcon.HTTP_200
            resp.content_type = 'application/octet-stream'
            resp.content_length = ex.size
            resp.downloadable_as = str(ex.name)
            resp.stream = DLTFile(ex)

def main():
    app = falcon.App()
    app.add_route("/files/", ListFiles())
    app.add_route("/files/{fileid}", GetFile())
    return app

app = main()
def run():
    from wsgiref.simple_sesrver import make_server
    with make_server('', 8000, app) as httpd:
        httpd.serve_forever()

if __name__ == "__main__":
    run()
