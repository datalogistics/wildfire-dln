from collections import defaultdict
from lace import logging
from libdlt.util.files import ExnodeInfo
from libdlt.file import DLTFile
from libdlt.depot import Depot
from libdlt.protocol import factory
from unis import Runtime
from unis.models import Exnode
from wdln.config import MultiConfig
from wdln.settings import LOADER_CONFPATH

import falcon, datetime, os, time, socket
import libdlt

TEMPLATE = """
<html><head><title>Index of /files/</title></head><body><h1>Index of /files/</h1><hr><pre><a href="../">../</a>
{body}</pre><hr></body></html>
"""

CR, LF = ord('\r'), ord('\n')
PREAMBLE, HEADERS, BODY, COMPLETE = list(range(4))
class ListFiles(object):
    def _getproxy(self):
        rt = Runtime(self._uri, proxy={"subscribe": False})
        self._stage = rt.services.first_where({"name": self._stage})
        self._d = Depot(self._stage.accessPoint)
        self._proxy = factory.makeProxy(getattr(self._d, "$schema", None))

    def __init__(self, uri, stage, bs, **kwargs):
        self._uri = uri
        self._bs = int(bs)
        self._stage = stage

    def _filelist(self):
        rt = Runtime(self._uri, proxy={"subscribe": False})
        files = []
        for ex in rt.exnodes:
            info = ExnodeInfo(ex, remote_validate=True)
            if info.is_complete():
                ts = ex.modified / 1000000
                files.append({
                    "id": ex.id,
                    "name": ex.name,
                    "size": ex.size,
                    "date": datetime.datetime.fromtimestamp(ts).strftime("%d-%b-%Y %H:%M")
                })
        return files

    def _dispatch_block(self, block, offset, exnode):
        if not exnode: return
        try: alloc = self._proxy.allocate(self._d, offset, len(block))
        except: alloc - None
        if not alloc: raise Exception("Failed to connect to staging block store")

        alloc.parent, alloc.offset = exnode, offset
        try: del alloc.getObject().__dict__['function']
        except KeyError: pass
        exnode.extents.append(alloc)
        self._proxy.store(alloc, block, len(block))

    def on_post(self, req, resp):
        self._getproxy()
        has_readinto = hasattr(req.stream, 'readinto')
        def _createfile(name):
            return Exnode({'name': name.decode('utf-8').strip('\"').strip('\''),
                           'mode': 'file',
                           'created': int(time.time() * 1000000),
                           'modified': int(time.time() * 1000000),
                           'parent': None,
                           'owner': 'wdln',
                           'group': 'wdln',
                           'permission': '744'})
        def _readblock(block, size, edge, length, read):
            newblock = bytearray(size)
            newblock[:edge] = block[-edge:]
            if has_readinto:
                return req.stream.readinto(memoryview(newblock)[edge:min(size, edge + (length -read))]), newblock
            else:
                newsize = min(size, edge + (length - read)) - edge
                newblock[edge:] = req.stream.read(newsize)
                return len(newblock), newblock

        state, workers, exnodes, is_file = PREAMBLE, [], [], False
        payload, headers, params, name = defaultdict(list), None, None, None
        if req.content_length and req.content_type.startswith("multipart/form-data"):
            try:
                bound = bytes(req.content_type[req.content_type.find("boundary=")+9:], 'utf-8')
                boundlen = len(bound)
                overwrite, writesize = boundlen + 4, self._bs
                blocksize, filesize = writesize + overwrite, 0
                read, length = 0, req.content_length
                do_read, block = True, bytearray(blocksize)
                s = time.time()
                while True:
                    if do_read:
                        r, block = _readblock(block, blocksize, overwrite, length, read)
                        read += r
                
                    if state == BODY:
                        i = block.find(bound)
                        if i == -1 or i > writesize:
                            data = memoryview(block)[:len(block) - overwrite]
                            if is_file:
                                self._dispatch_block(data, filesize, exnodes[-1])
                            else:
                                payload[name][-1]['content'] += data
                            filesize += len(data)
                            do_read = True
                            continue
                        blob = memoryview(block)[:i+boundlen+2]
                        data = blob[:i-4]
                        if blob[-2:] == b'--': state = COMPLETE
                        else: state, blob = HEADERS, blob[:-2]
                        if is_file:
                            self._dispatch_block(data, filesize, exnodes[-1])
                        else:
                            payload[name][-1]['content'] += data
                        filesize += len(data)
                        r, block = _readblock(block, blocksize, len(block), length, read)
                        read += r
                        do_read = False
                    elif state == HEADERS:
                        state, j = BODY, block.find(b'\r\n\r\n')
                        if j == -1:
                            raise Exception("Header block does not close properly")
                        bstart = j + 4
                        raw, headers, params = block[:j], defaultdict(list), {}
                        for line in raw.split(b'\r\n'):
                            line = line.split(b':')
                            if len(line) == 2:
                                headers[line[0].decode('utf-8').strip()] = line[1]
                                if line[0] == b'Content-Disposition':
                                    for v in line[1].split(b';'):
                                        v = v.split(b'=')
                                        params[v[0].decode('utf-8').strip()] = v[1] if len(v) == 2 else b''
                        if 'filename' in params:
                            filesize, is_file = 0, True
                            exnodes.append(_createfile(params['filename']))
                        else: is_file = False
                        name = params['name'].decode('utf-8')
                        payload[name].append({'headers': headers, 'params': params, 'content': bytearray()})
                        do_read, block = False, block[bstart:]
                    elif state == PREAMBLE:
                        i = block.find(bound)
                        if i == -1:
                            raise Exception("Preamble block does not close properly")
                        blob = memoryview(block)[:i+boundlen+2]
                        if blob[:-2] == b'--': state = COMPLETE
                        else:
                            j = block.find(b'\r\n\r\n')
                            if j == -1:
                                raise Exception("Header block does not close properly")
                            bstart = j + 4
                            raw, headers, params = block[i+boundlen+2:j], defaultdict(list), {}
                            for line in raw.split(b'\r\n'):
                                line = line.split(b':')
                                if len(line) == 2:
                                    headers[line[0].decode('utf-8').strip().strip('\"')] = line[1]
                                    if line[0] == b'Content-Disposition':
                                        for v in line[1].split(b';'):
                                            v = v.split(b'=')
                                            params[v[0].decode('utf-8').strip().strip('\"')] = v[1] if len(v) == 2 else b''
                            if 'filename' in params:
                                is_file = True
                                exnodes.append(_createfile(params['filename']))
                            else: is_file = False
                            name = params['name'].decode('utf-8').strip('\"').strip("\'")
                            payload[name].append({'headers': headers, 'params': params, 'content': bytearray()})
                            do_read, block = False, block[bstart:]
                            state = BODY
                    else:
                        break
                    if do_read and read >= length: break
            except Exception as e:
                import traceback
                traceback.print_exc()
                resp.status = falcon.HTTP_400
                resp.content_type = falcon.MEDIA_JSON
                resp.media = {"errorcode": 1, "msg": "Malformed multipart content"}
                return

            rt = Runtime(self._uri, proxy={"subscribe": False})
            parent = payload['parent'][0]['content'].decode('utf-8') if 'parent' in payload else None
            parent = rt.exnodes.first_where(lambda x: x.id == parent)
            for ex in exnodes:
                ex.parent = parent
                rt.insert(ex, commit=True)
                for alloc in ex.extents:
                    rt.insert(alloc, commit=True)
            ex.size = sum([e.size for e in ex.extents])
            rt.flush()
            resp.content_type = falcon.MEDIA_TEXT
            resp.media = ""
            resp.status = falcon.HTTP_201

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
    
    def on_get(self, req, resp, fileid):
        rt = Runtime(self._uri, proxy={"subscribe": False})
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

class Uploader(object):
    def __init__(self, uri, stage, bs):
        rt = Runtime(uri, proxy={"subscribe": False})
        self._uri = uri
        self._bs = int(bs)
        self._stage = rt.services.first_where({"name": stage})
        self._d = Depot(self._stage.accessPoint)
        self._proxy = factory.makeProxy(getattr(self._d, "$schema", None))


conf = MultiConfig({"port": "8000", "uri": "http://localhost:9000", "bs": 1024 * 1024 * 20, "stage": socket.gethostname()},
                   "File loader for WDLN remote agents",
                   filepath="/etc/dlt/loader.cfg", filevar=LOADER_CONFPATH)
conf = conf.from_file()
def main():
    app = falcon.App()
    app.add_route("/files/", ListFiles(**conf))
    app.add_route("/files/{fileid}", GetFile(conf['uri']))
    return app

app = main()
def run():
    print(conf)
    from wsgiref.simple_server import make_server
    with make_server('', int(conf['port']), app) as httpd:
        httpd.serve_forever()

if __name__ == "__main__":
    run()
