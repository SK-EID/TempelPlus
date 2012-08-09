#!/bin/sh

#Welcoming message
echo "Starting TempelPlus from shell script"

#Variables
JAVA_HOME="/home/anton/bin/jdk1.6.0_32/jre/bin/java"
#JAVA_HOME="java"
ENCODING=UTF-8

#Starts Run.jar, uses native libraries from existing java path and also from "linux" folder (needed for loading libpkcs11wrapper.so, which by default most probably may not belong to java lib path)
#Commandline arguments are passed using syntax ${*+"$*"}) in order to handle embedded spaces properly (if needed)

$JAVA_HOME -Dfile.encoding=$ENCODING -Xmx1024m -Djava.library.path="linux/32:$PATH" -jar Run.jar ${1+"$1"} ${2+"$2"} ${3+"$3"} ${4+"$4"} ${5+"$5"} ${6+"$6"} ${7+"$7"} ${8+"$8"} ${9+"$9"} ${10+"$10"} ${11+"$11"} ${12+"$12"} ${13+"$13"} ${14+"$14"} ${15+"$15"} ${16+"$16"} ${17+"$17"} ${18+"$18"} ${19+"$19"}
