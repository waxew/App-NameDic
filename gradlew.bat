@echo off
rem Bootstrap سبک Gradle برای Windows.
setlocal
set GRADLE_VERSION=9.5.0
set ROOT_DIR=%~dp0
set LOCAL_DIR=%ROOT_DIR%.gradle-local
set GRADLE_HOME=%LOCAL_DIR%\gradle-%GRADLE_VERSION%
set ZIP_FILE=%LOCAL_DIR%\gradle-%GRADLE_VERSION%-bin.zip
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (gradle %* & exit /b %ERRORLEVEL%)
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%LOCAL_DIR%" mkdir "%LOCAL_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP_FILE%'; Expand-Archive -Force '%ZIP_FILE%' '%LOCAL_DIR%'"
  if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
