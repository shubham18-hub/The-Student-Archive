@echo off
cd /d d:\my-pdf-db

REM Set Java environment
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%

REM Run Maven compile
echo Building with Java 21...
mvn clean test-compile -q
echo Build complete!
pause
