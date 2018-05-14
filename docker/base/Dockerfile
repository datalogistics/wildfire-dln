FROM python:3.6-stretch

MAINTAINER Ezra Kissel <ezkissel@indiana.edu>

EXPOSE 6714/tcp
EXPOSE 9000/tcp
EXPOSE 42424/tcp

RUN curl -sL https://deb.nodesource.com/setup_8.x | bash -
RUN apt-get -y install sudo cmake gcc libaprutil1-dev libapr1-dev mongodb redis-server python-setuptools python-pip python-m2crypto nodejs

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

RUN git clone https://github.com/datalogistics/ibp_server
RUN git clone -b wdln https://github.com/datalogistics/dlt-web
RUN git clone -b develop https://github.com/periscope-ps/unis
#RUN git clone https://github.com/datalogistics/wildfire-dln

ADD build.sh .
RUN bash ./build.sh

ADD properties.js $HOME/dlt-web/

ADD run.sh .
CMD bash ./run.sh
