@echo off
setlocal

set "WRAPPER_DIR=%~dp0"
set "M2_WRAPPER_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven"
set "MVN_CMD=%M2_WRAPPER_DIR%\apache-maven-3.9.9\bin\mvn.cmd"

if not exist "%MVN_CMD%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; $url = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip'; $m2WrapperDir = Join-Path $env:USERPROFILE '.m2\wrapper\dists\apache-maven'; New-Item -ItemType Directory -Force -Path $m2WrapperDir | Out-Null; $zipPath = Join-Path $m2WrapperDir 'apache-maven-3.9.9-bin.zip'; Write-Host 'Downloading Apache Maven...'; Invoke-WebRequest -Uri $url -OutFile $zipPath; Expand-Archive -Path $zipPath -DestinationPath $m2WrapperDir -Force; Remove-Item $zipPath -Force"
)

call "%MVN_CMD%" %*
exit /b %ERRORLEVEL%
