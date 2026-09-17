@echo off
setlocal enableextensions enabledelayedexpansion

set MVN_DIR=%~dp0.mvn\maven-3.9.6
set MVN_BIN=%MVN_DIR%\apache-maven-3.9.6\bin\mvn.cmd

if not exist "%MVN_BIN%" (
    echo Downloading Portable Apache Maven 3.9.6...
    if not exist "%~dp0.mvn" mkdir "%~dp0.mvn"
    powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $zip = '%~dp0.mvn\maven.zip'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile $zip; Expand-Archive -Path $zip -DestinationPath '%MVN_DIR%' -Force; Remove-Item -Path $zip -Force }"
)

call "%MVN_BIN%" %*
exit /b %ERRORLEVEL%
