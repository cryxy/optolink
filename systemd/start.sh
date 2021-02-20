#!/bin/bash

HOME=/opt/optolink


test -f $HOME/start.sh || exit 0

cd $HOME 

prog_arg=" -Djava.library.path=/usr/lib/jni"
prog_arg="${prog_arg} -Dgnu.io.rxtx.SerialPorts=/dev/ttyUSB0"


java $prog_arg -jar /opt/optolink/optolink-jar-with-dependencies.jar
