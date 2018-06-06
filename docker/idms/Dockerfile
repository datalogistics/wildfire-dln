FROM python:3.6-stretch

MAINTAINER Ezra Kissel <ezkissel@indiana.edu>

EXPOSE 9001/tcp

RUN apt-get update
RUN apt-get -y install sudo cmake gcc libaprutil1-dev libapr1-dev python-setuptools python-pip

RUN export uid=1000 gid=1000 && \
    mkdir -p /home/wdln && \
    echo "wdln:x:${uid}:${gid}:WILDFIRE-DLN,,,:/home/wdln:/bin/bash" >> /etc/passwd && \
    echo "wdln:x:${uid}:" >> /etc/group && \
    echo "wdln ALL=(ALL) NOPASSWD: ALL" > /etc/sudoers.d/wdln && \
    chmod 0440 /etc/sudoers.d/wdln && \
    chown ${uid}:${gid} -R /home/wdln && \
    chown ${uid}:${gid} -R /opt

USER wdln
ENV HOME /home/wdln
WORKDIR $HOME

RUN git clone -b master https://github.com/datalogistics/wildfire-dln
RUN git clone -b asyncio https://github.com/datalogistics/libdlt

ADD .rtcache $HOME/.periscope/

ADD build.sh .
RUN bash ./build.sh

ADD .dlt .

ENV DEBUG INFO
ADD run.sh .
CMD bash ./run.sh
