@echo off
REM Gradle Wrapper para Windows

set DIR=%~dp0
set GRADLE_VERSION=7.3.3

if not exist "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  echo Descargando Gradle Wrapper...
  mkdir "%DIR%gradle\wrapper"
  curl -L "https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip" -o "%DIR%gradle\wrapper\gradle-%GRADLE_VERSION%-bin.zip"
  powershell -Command "Expand-Archive -Path '%DIR%gradle\wrapper\gradle-%GRADLE_VERSION%-bin.zip' -DestinationPath '%DIR%gradle\wrapper\'"
)

"%DIR%gradle\wrapper\gradle-%GRADLE_VERSION%\bin\gradle" %*

