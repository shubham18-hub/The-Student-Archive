@echo off
setlocal enabledelayedexpansion
set JAVA_HOME=C:\Program Files\Java\jdk-21
set M2_HOME=C:\Users\shubh\Downloads\apache-maven-3.9.13
set PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%

echo Current JAVA_HOME: %JAVA_HOME%
echo Current M2_HOME: %M2_HOME%
echo.

cd /d d:\my-pdf-db

echo Attempting to compile project...
call mvn clean compile -DskipTests

echo.
echo Compilation step complete. Now running tests...
call mvn test -q

echo All done.
