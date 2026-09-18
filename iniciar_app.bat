@echo off
title DB Studio - Iniciando...
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"
echo Iniciando DB Studio con Compose Desktop...
call gradlew.bat run
pause
