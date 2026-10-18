@echo off
echo ============================================================
echo   LibraryOS — Library Management System
echo   Quick Start Script for Windows
echo ============================================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed or not in PATH.
    echo.
    echo Please install Java 21 from:
    echo   https://adoptium.net/  ^(free, recommended^)
    echo.
    echo After installing, re-run this script.
    pause
    exit /b 1
)

echo [OK] Java found:
java -version 2>&1 | findstr /i "version"
echo.

REM Check if Maven is installed
mvn -version >nul 2>&1
IF %ERRORLEVEL% EQU 0 (
    echo [OK] Maven found. Starting backend...
    echo.
    mvn spring-boot:run
) ELSE (
    echo [INFO] Maven not found in PATH. Using Maven Wrapper...
    echo.
    IF EXIST "mvnw.cmd" (
        call mvnw.cmd spring-boot:run
    ) ELSE (
        echo [ERROR] mvnw.cmd not found either.
        echo.
        echo Please install Maven from https://maven.apache.org/download.cgi
        echo Extract it and add the \bin folder to your System PATH.
        pause
        exit /b 1
    )
)
