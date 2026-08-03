@echo off
setlocal EnableExtensions

set "GRADLE_VERSION=8.9"
set "GRADLE_SHA256=d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab"
set "BASE_DIR=%USERPROFILE%\.gradle\lunara-bootstrap"
set "GRADLE_HOME=%BASE_DIR%\gradle-%GRADLE_VERSION%"
set "ZIP_FILE=%BASE_DIR%\gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%BASE_DIR%" mkdir "%BASE_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop'; $zip='%ZIP_FILE%'; $part=$zip+'.part'; $expected='%GRADLE_SHA256%';" ^
    "if (Test-Path $zip) { $actual=(Get-FileHash -Algorithm SHA256 $zip).Hash.ToLowerInvariant(); if ($actual -ne $expected) { Remove-Item -Force $zip } };" ^
    "if (-not (Test-Path $zip)) { Remove-Item -Force $part -ErrorAction SilentlyContinue; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $part; $actual=(Get-FileHash -Algorithm SHA256 $part).Hash.ToLowerInvariant(); if ($actual -ne $expected) { Remove-Item -Force $part; throw ('Gradle checksum mismatch. Expected '+$expected+' but received '+$actual) }; Move-Item -Force $part $zip };" ^
    "Expand-Archive -Force $zip '%BASE_DIR%'"
  if errorlevel 1 exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
