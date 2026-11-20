@echo off
rem Runs the simulation. Example: run.bat 42 300 150
if not exist out\mas\Main.class call build.bat || exit /b 1
java -cp out mas.Main %*
