@echo off
REM WikiChat CLI Wrapper Script for Windows
REM This script provides a convenient way to run the WikiChat CLI without
REM explicitly calling java -jar.

setlocal enabledelayedexpansion

REM Script directory (where this script is located)
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "CLI_DIR=%PROJECT_ROOT%\ao-wiki-chat-cli"
set "JAR_NAME=ao-wiki-chat-cli-0.0.1-SNAPSHOT.jar"
set "JAR_PATH=%CLI_DIR%\target\%JAR_NAME%"

REM Check if Java is installed
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 25 or later and ensure it's in your PATH
    exit /b 1
)

REM Get Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION_STRING=%%g
    goto :version_found
)
:version_found

REM Extract major version number (simplified check)
REM This is a basic check - for production, consider using a more robust method
java -version 2>&1 | findstr /i "25" >nul
if %ERRORLEVEL% NEQ 0 (
    java -version 2>&1 | findstr /i "version \"2[5-9]" >nul
    if %ERRORLEVEL% NEQ 0 (
        echo Error: Java 25 or later is required
        echo Please install Java 25 or later
        exit /b 1
    )
)

echo Info: Java version check passed

REM Check if JAR exists
if not exist "%JAR_PATH%" (
    echo Error: JAR file not found: %JAR_PATH%
    echo.
    echo Please build the CLI first:
    echo   cd %PROJECT_ROOT%
    echo   mvnw.cmd clean package -pl ao-wiki-chat-cli -am
    exit /b 1
)

REM Execute the CLI with all arguments passed to this script
java -jar "%JAR_PATH%" %*

endlocal
