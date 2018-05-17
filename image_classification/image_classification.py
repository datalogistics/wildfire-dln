import time
import re
import tensorflow as tf
import sys
import tarfile
from six.moves import urllib
import numpy as np
import numpy.random as rnd
import os
import matplotlib.image as mpimg
from tensorflow.contrib.slim.nets import inception
import tensorflow.contrib.slim as slim
import datetime
import shutil
'''
    requires tensorflow to be installed on raspberry pi 
    follow instructions from https://github.com/samjabrahams/tensorflow-on-raspberry-pi for installing
    tensorflow on raspberry pi
'''
#URL for downloading the inception v3 model
TF_MODELS_URL = "http://download.tensorflow.org/models"
INCEPTION_V3_URL = TF_MODELS_URL + "/inception_v3_2016_08_28.tar.gz"
INCEPTION_PATH = os.path.join("datasets", "inception")
INCEPTION_V3_CHECKPOINT_PATH = os.path.join(INCEPTION_PATH, "inception_v3.ckpt")

#dimensions for cropping the image
width = 299
height = 299
channels = 3


#method to download the model if it isn't available
def download_progress(count, block_size, total_size):
    percent = count * block_size * 100 // total_size
    sys.stdout.write("\rDownloading: {}%".format(percent))
    sys.stdout.flush()

#method to fetch the model
def fetch_pretrained_inception_v3(url=INCEPTION_V3_URL, path=INCEPTION_PATH):
    if os.path.exists(INCEPTION_V3_CHECKPOINT_PATH):
        return
    os.makedirs(path)
    tgz_path = os.path.join(path, "inception_v3.tgz")
    urllib.request.urlretrieve(url, tgz_path, reporthook=download_progress)
    inception_tgz = tarfile.open(tgz_path)
    inception_tgz.extractall(path=path)
    inception_tgz.close()
    os.remove(tgz_path)

CLASS_NAME_REGEX = re.compile(r"^n\d+\s+(.*)\s*$", re.M | re.U)

#Loads the classes from the imagenet corpus from a text file
def load_class_names():
    with open(os.path.join("datasets","inception","imagenet_class_names.txt"), "rb") as f:
        content = f.read().decode("utf-8")
        return CLASS_NAME_REGEX.findall(content)

fetch_pretrained_inception_v3()
class_names = load_class_names()


tf.reset_default_graph()

#place holder for the input tensor
X = tf.placeholder(tf.float32, shape=[None, height, width, channels], name="X")

#retreiving weights from the frozen inception model
with slim.arg_scope(inception.inception_v3_arg_scope()):
    logits, end_points = inception.inception_v3(X, num_classes=1001, is_training=False)
predictions = end_points["Predictions"]
saver = tf.train.Saver()


while True:
    if os.path.exists('output.png'):
        if os.path.exists('archive'):
            tm = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d %H:%M:%S')
            shutil.move('output.png', 'archive/'+tm)
        else:
            os.mkdir('archive')
            datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d %H:%M:%S')
            shutil.move('output.png', 'archive/'+tm)
    #pulls the image from the fosscamm over rtsp and stores into output.png file in the current working directory
    os.system('ffmpeg -rtsp_transport tcp -i rtsp://openlab:open_r0cks\!@10.10.10.115:88/videoMain -ss 00:00:00.00  -t 30 -qscale:v 4 -frames:v 1  output.png >/dev/null 2>&1')
    os.system('convert output.png -resize 299x299! test_image.png')     #reshapes the image to be suitable for input tensor
    test_image = mpimg.imread(os.path.join("test_image.png"))[:, :, :channels]
    X_test = test_image.reshape(-1, height, width, channels)

    with tf.Session() as sess:
        saver.restore(sess, INCEPTION_V3_CHECKPOINT_PATH)
        predictions_val = predictions.eval(feed_dict={X: X_test})       #prediction for the output.png image

    top_5 = np.argpartition(predictions_val[0], -5)[-5:]
    top_5 = top_5[np.argsort(predictions_val[0][top_5])]                #top 5 predictions for the image
    for i in top_5:
            print("{0}: {1:.2f}%".format(class_names[i], 100*predictions_val[0][i]))
    print('-----------------------------------')
