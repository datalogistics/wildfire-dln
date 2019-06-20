import sys,os
import traceback
from bottle import route,run,request

UPLOAD_DIR="/depot/web"
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
        filepath = os.path.join(UPLOAD_DIR, filedata.filename)
        filedata.save(filepath, overwrite=True)
       
run(host='0.0.0.0', port=UPLOAD_PORT, debug=True)
