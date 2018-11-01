## Fire/ Smoke classification in image using tensorflow on raspberry pi

The model using pre-trained [Mobile net](https://arxiv.org/abs/1704.04861) , a deep convolutional neural network architecture which is suitable for resource constrained devices. The model is retrained on images containing smoke, fire or neither of them.

The retrained model graph is frozen and stored in `retrained_graph.pb` file.

The application is run as a node app which pulls live video feed using RTSP protocol and converts into images using `ffmpeg`. The received image is stored as `test.jpg` on raspberry pi. The node app invokes the python script which performs real time inference on the input `test.jpg` to emit the probability scores on which class the particular image belongs.

Once it is invoked, The application runs forever until it is stopped by an user.

Invocation of the image classification pipeline.

```sh
    $ node video_feed.js
```

Sample output of the program

```
Evaluation time (1-image): 0.951s

{"fire": 0.002, "smoke": 0.0, "nofire": 0.998}
```


