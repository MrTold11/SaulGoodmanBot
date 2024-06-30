@echo off
setlocal

rem Build the Java application with Gradle and redirect output to a log file
echo Starting Gradle build...
call .\gradlew build > build.log 2>&1

rem Check if the build was successful
if %ERRORLEVEL% neq 0 (
    echo ERROR: Build failed! Check build.log for details.
    exit /b %ERRORLEVEL%
) else (
    echo Build succeeded!
)

rem Define the path to the JAR file
set JAR_FILE=.\dir\SaulGoodmanBot-1.0-SNAPSHOT.jar

rem Define the server details
set SERVER_USER=root
set SERVER_IP=159.89.110.20
set REMOTE_DIR=/bot

rem Verify if the JAR file exists
if not exist %JAR_FILE% (
    echo ERROR: JAR file not found at %JAR_FILE%!
    exit /b 1
) else (
    echo JAR file found at %JAR_FILE%.
)

rem Send the JAR file to the server
echo Transferring JAR file to the server...
call scp %JAR_FILE% %SERVER_USER%@%SERVER_IP%:%REMOTE_DIR%

rem Check if the scp command was successful
if %ERRORLEVEL% neq 0 (
    echo File transfer failed!
    exit /b %ERRORLEVEL%
) else (
    echo File transfer succeeded!
)

echo Build and transfer to Development environment completed successfully!
pause