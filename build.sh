#!/bin/bash

#build
opt/gradle-3.5.1/bin/gradle build install
cp -rlf build src/
rm -r build

# execute
#cd src/build/install/OmeroMonitoring/
#./bin/OmeroMonitoring
