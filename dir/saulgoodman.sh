#!/bin/bash

JAR=SaulGoodmanBot-1.0-SNAPSHOT.jar
MAXRAM=512M
MINRAM=128M
TIME=8


while true; do
    java -Xmx$MAXRAM -Xms$MINRAM -jar $JAR
    if [[ ! -d "exit_codes" ]]; then
        mkdir "exit_codes";
    fi
    if [[ ! -f "exit_codes/server_exit_codes.log" ]]; then
        touch "exit_codes/server_exit_codes.log";
    fi
    echo "[$(date +"%d.%m.%Y %T")] ExitCode: $?" >> exit_codes/server_exit_codes.log
    echo "----- Press enter to prevent the program from restarting in $TIME seconds -----";
    input=$(read -r -t $TIME);
    if [[ $input == 0 ]]; then
        break;
    else
        echo "------------------- SERVER RESTARTS -------------------";
    fi
done