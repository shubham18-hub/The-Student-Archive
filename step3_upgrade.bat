@echo off
setlocal enabledelayedexpansion
set JAVA_HOME=C:\Program Files\Java\jdk-21
set M2_HOME=C:\Users\shubh\Downloads\apache-maven-3.9.13
set PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%

cd /d d:\my-pdf-db

echo Step 3: Upgrading Java version to 21
echo ===========================================
echo Java version being used:
"%JAVA_HOME%\bin\java.exe" -version
echo.

echo Compiling with Java 21 (updated pom.xml)...
call mvn clean test-compile

echo.
if %ERRORLEVEL% EQU 0 (
  echo Compilation SUCCESS with Java 21!
) else (
  echo Compilation FAILED!
  exit /b 1
)
