import sys,os
import traceback
from bottle import route,run,request

UPLOAD_DIR="/depot/upload"
UPLOAD_PORT=8080

def hello():
    return "Hello World!"

@route('/flist')
def flist():
    return repr(os.listdir("UPLOAD_DIR"))

@route('/upload', method='POST')
def UploadFiles():
    #print(request.files.keys())
    for uf in request.files.keys():
        filedata = request.files.get(uf)
        dotname = ".{}".format(filedata.filename)
        filepath = os.path.join(UPLOAD_DIR, dotname)
        filedata.save(filepath, overwrite=True)
        if os.path.getsize(filepath) > 0:
            newpath = os.path.join(UPLOAD_DIR, filedata.filename)
            os.rename(filepath, newpath)
            
run(host='0.0.0.0', port=UPLOAD_PORT, debug=True)
