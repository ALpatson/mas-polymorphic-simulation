#!/bin/sh
# Compiles the simulation with plain javac (no Maven needed) into the "out" folder.
set -e
rm -rf out && mkdir out
javac --release 21 -Xlint:all -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/mas/Main.java src/main/java/mas/SelfTest.java
echo "Build OK. Run with: sh run.sh (window) or sh run.sh 42 --console (terminal)"
