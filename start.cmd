@echo off
setlocal
cd /d "%~dp0rest"

rem Use the project's installed JDK instead of an older Java from PATH or the IDE.
if exist "%USERPROFILE%\.jdks\openjdk-27\bin\java.exe" set "JAVA_HOME=%USERPROFILE%\.jdks\openjdk-27"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo JDK 27 was not found. Set JAVA_HOME to your JDK 27 installation.
    exit /b 1
)

echo Using JDK: %JAVA_HOME%
echo MySQL must be running on localhost:3306, for example through XAMPP.
call mvnw.cmd --no-transfer-progress spring-boot:run %*
exit /b %errorlevel%
