@echo off
setlocal enabledelayedexpansion
set JAVA_HOME=C:\Program Files\Java\jdk-21
set M2_HOME=C:\Users\shubh\Downloads\apache-maven-3.9.13
set PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%

cd /d d:\my-pdf-db

echo Step 4: Final Validation
echo ===========================================
echo Running full test suite with Java 21...
echo.

call mvn clean test

echo.
echo Final validation complete!
