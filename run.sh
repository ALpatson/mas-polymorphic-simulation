#!/bin/sh
# Runs the simulation. Example: ./run.sh 42 300 150
[ -f out/mas/Main.class ] || sh build.sh
java -cp out mas.Main "$@"
