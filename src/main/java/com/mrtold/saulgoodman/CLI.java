package com.mrtold.saulgoodman;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * @author Mr_Told
 */
public class CLI extends Thread {

    final Main main;
    boolean run = true;

    final Logger log;

    public CLI(Main main) {
        this.main = main;
        log = LoggerFactory.getLogger(CLI.class);
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (run) {
            try {
                String cmdRaw = scanner.nextLine();
                log.info("Input: {}", cmdRaw);

                String[] cmd = cmdRaw.split(" ");
                String command = cmd[0];
                int argsCount = cmd.length - 1;
                switch (command) {
                    case "exit":
                    case "stop":
                        System.out.println("Goodbye!");
                        main.stop();
                        return;
                    case "help":
                        System.out.println(" <------------ Help section ------------> ");
                        System.out.println(" > exit/stop - stop everything and exit");
                        break;
                    default:
                        System.out.printf("Unknown command: %s\n", command);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public void close() {
        run = false;
        interrupt();
    }

}
