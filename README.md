This is a minimal connection to OMERO using the Java gateway
based on https://github.com/ome/minimal-omero-client to measure upload speed
Client-Server.

Using gradle to build project.
Usage:
Please set the right gradle path in build.sh.
Build: >>sh build.sh

OR start client separately with script:
OmeroMonitoring/src/build/install/OmeroMonitoring/bin/OmeroMonitoring.bat
OR execute java:
java -cp .;<pathTo>/OmeroMonitoring/src/build/install/OmeroMonitoring/lib/*; com.example.SimpleConnection
