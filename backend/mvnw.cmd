@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script
@REM This downloads Maven automatically if not installed
@REM ----------------------------------------------------------------------------
@echo off

SET MAVEN_VERSION=3.9.6
SET MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip
SET MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%
SET MAVEN_EXE=%MAVEN_HOME%\bin\mvn.cmd

IF EXIST "%MAVEN_EXE%" GOTO run

echo [INFO] Maven not found. Downloading Apache Maven %MAVEN_VERSION%...
echo [INFO] This is a one-time download and will be cached for future use.
echo.

IF NOT EXIST "%USERPROFILE%\.m2\wrapper\dists" mkdir "%USERPROFILE%\.m2\wrapper\dists"

powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%USERPROFILE%\.m2\wrapper\apache-maven-%MAVEN_VERSION%-bin.zip' }"

echo [INFO] Extracting Maven...
powershell -Command "Expand-Archive -Path '%USERPROFILE%\.m2\wrapper\apache-maven-%MAVEN_VERSION%-bin.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists\' -Force"

del "%USERPROFILE%\.m2\wrapper\apache-maven-%MAVEN_VERSION%-bin.zip"
echo [INFO] Maven downloaded successfully.

:run
"%MAVEN_EXE%" %*
