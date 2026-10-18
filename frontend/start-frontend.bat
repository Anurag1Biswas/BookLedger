@echo off
echo ============================================================
echo   LibraryOS — Frontend Start Script
echo ============================================================
echo.

REM Check Node.js
node -v >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is not installed.
    echo Please install it from: https://nodejs.org  ^(LTS version^)
    pause
    exit /b 1
)
echo [OK] Node.js found: 
node -v
echo.

REM Install dependencies if node_modules missing
IF NOT EXIST "node_modules" (
    echo [INFO] Installing npm dependencies ^(first time only^)...
    npm install
    echo.
)

REM Check if Angular CLI is installed globally
ng version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [INFO] Angular CLI not found globally. Installing...
    npm install -g @angular/cli
    echo.
)

echo [INFO] Starting Angular dev server on http://localhost:4200 ...
echo.
ng serve --open
