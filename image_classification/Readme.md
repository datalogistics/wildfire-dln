## Image classification using tensorflow in raspberry pi

The dataset folder contains a .txt file which contains the class names used in the imagenet classification task.
The python code includes utility method to pull the frozen weights from the inception model from tensorflow repository

This requires tensoflow to be installed on the raspberry pi. Please follow instructions from https://github.com/samjabrahams/tensorflow-on-raspberry-pi. The python script connects to fosscam over rtsp and stores the image captured by the camera in a .png format.

Inovacation of the script.

```sh
    $ python image_classification.py
```

