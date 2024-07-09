@echo off
setlocal

echo Starting Gradle build...
call .\gradlew build > build.log 2>&1

if %ERRORLEVEL% neq 0 (
    echo ERROR: Build failed! Check build.log for details.
    exit /b %ERRORLEVEL%
) else (
    echo Build succeeded!
)

set JAR_FILE=.\dir\SaulGoodmanBot-1.0-SNAPSHOT.jar

set SERVER_USER=root
set SERVER_IP=development.mbalg.com
set REMOTE_DIR=/root/bot/

if not exist %JAR_FILE% (
    echo ERROR: JAR file not found at %JAR_FILE%!
    exit /b 1
) else (
    echo JAR file found at %JAR_FILE%.
)

echo Transferring JAR file to the server...
call scp %JAR_FILE% %SERVER_USER%@%SERVER_IP%:%REMOTE_DIR%

if %ERRORLEVEL% neq 0 (
    echo File transfer failed!
    exit /b %ERRORLEVEL%
) else (
    echo File transfer succeeded!
)

echo DEVELOPMENT Pipeline completed successfully!
