@REM Maven Wrapper startup script for Windows
@REM Requires: Java on PATH and .mvn\wrapper\maven-wrapper.jar present.
@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
@REM Strip trailing backslash
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"

if not exist "%WRAPPER_JAR%" (
    echo ERROR: Maven wrapper jar not found at %WRAPPER_JAR%
    echo        Download it and place it at .mvn\wrapper\maven-wrapper.jar
    exit /b 1
)

set "JAVA_EXE=java"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java"

"%JAVA_EXE%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*

exit /b %ERRORLEVEL%
