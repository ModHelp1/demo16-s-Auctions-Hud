@echo off
setlocal
cd /d "%~dp0"
echo === demo16's Auction's Hud - one-click build ===

where java >nul 2>&1
if errorlevel 1 (
  echo.
  echo Java was not found. Install JDK 21 from https://adoptium.net
  echo Then double-click this file again.
  pause
  exit /b 1
)

java -version 2>&1 | findstr /R /C:"version \"2[1-9]" >nul
if errorlevel 1 (
  echo.
  echo Your Java is too old. You need JDK 21 or newer: https://adoptium.net
  pause
  exit /b 1
)

if not exist ".gradle-dist\gradle-9.2.1\bin\gradle.bat" (
  echo Downloading Gradle 9.2.1 ^(one time only^)...
  powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-9.2.1-bin.zip' -OutFile 'gradle.zip'; Expand-Archive 'gradle.zip' -DestinationPath '.gradle-dist' -Force; Remove-Item 'gradle.zip'"
  if errorlevel 1 (
    echo Download failed. Check your internet connection and try again.
    pause
    exit /b 1
  )
)

echo Building... this takes a few minutes the first time.
call ".gradle-dist\gradle-9.2.1\bin\gradle.bat" build --no-daemon
if errorlevel 1 (
  echo.
  echo BUILD FAILED. Copy the error above and send it to Claude.
  pause
  exit /b 1
)

if not exist "MOD-JAR" mkdir "MOD-JAR"
copy /Y "build\libs\demo16s-auctions-hud-1.0.1.jar" "MOD-JAR\" >nul

echo.
echo DONE! Your mod is in the MOD-JAR folder:
echo   MOD-JAR\demo16s-auctions-hud-1.0.1.jar
start "" "MOD-JAR"
pause
