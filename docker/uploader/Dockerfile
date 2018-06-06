FROM python:3.6-stretch

MAINTAINER Ezra Kissel <ezkissel@indiana.edu>

RUN apt-get update
RUN apt-get -y install sudo cmake gcc python-setuptools python-pip

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

RUN git clone -b develop https://github.com/periscope-ps/unisrt
RUN git clone -b master https://github.com/periscope-ps/lace
RUN git clone -b asyncio https://github.com/datalogistics/libdlt

ADD build.sh .
RUN bash ./build.sh

RUN mkdir $HOME/.periscope
ADD depots .depots
ADD --chown=wdln:wdln .rtcache $HOME/.periscope/.rtcache/
ADD --chown=wdln:wdln .cache $HOME/.periscope/.cache/

ADD run.sh .
CMD bash ./run.sh
