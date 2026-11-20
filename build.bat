@echo off
rem Compiles the simulation with plain javac (no Maven needed) into the "out" folder.
if exist out rmdir /s /q out
mkdir out
javac --release 21 -Xlint:all -encoding UTF-8 -d out -sourcepath src\main\java src\main\java\mas\Main.java src\main\java\mas\SelfTest.java
if errorlevel 1 (
    echo BUILD FAILED
    exit /b 1
)
echo Build OK. Run with: run.bat   (window)   or   run.bat 42 --console   (terminal)
