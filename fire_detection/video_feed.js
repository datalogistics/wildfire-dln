var fs = require('fs');
var spawn = require('child_process').execSync;

//deletes the test image if already present
try{
	var rm_file = spawn('rm test.jpg > /dev/null 2>&1')
}
catch(error){}

//runs in a loop forever unless stopped
while(true) {
	//pulls image from fosscam
	var ffmpeg = spawn('ffmpeg -rtsp_transport tcp -i rtsp://openlab:open_r0cks\!@10.10.10.115:88/videoMain -r 1 -t 30 -qscale 4 -frames:v 1 test.jpg > /dev/null 2>&1')
	
	//performs image classification to check if the image contains fire/smoke or none of them
	var image_classify = spawn('python -m scripts.label_image --graph tf_files/retrained_graph.pb --image test.jpg').toString()
	console.log(image_classify)

	//once the classification is completed, the image is deleted
	var remove_image = spawn('rm test.jpg')
}













