var fs = require('fs');
var spawn = require('child_process').execSync;

var rtspURI = 'rtsp://openlab:open_r0cks\!@10.10.10.115:88/videoMain';
try{
var rm_file = spawn('rm test.jpg > /dev/null 2>&1')}
catch(error){}
while(true) {
	var ffmpeg = spawn('ffmpeg -rtsp_transport tcp -i rtsp://openlab:open_r0cks\!@10.10.10.115:88/videoMain -r 1 -t 30 -qscale 4 -frames:v 1 test.jpg > /dev/null 2>&1')
	var image_classify = spawn('python -m scripts.label_image --graph tf_files/retrained_graph.pb --image test.jpg').toString()
	console.log(image_classify)
	var remove_image = spawn('rm test.jpg')
}













