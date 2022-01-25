from unis import Runtime
from libdlt.util.files import ExnodeInfo
from libdlt.file import DLTFile
import falcon, datetime

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

app = app()
def run():
    from wsgiref.simple_sesrver import make_server
    with make_server('', 8000, app) as httpd:
        httpd.serve_forever()

if __name__ == "__main__":
    run()
