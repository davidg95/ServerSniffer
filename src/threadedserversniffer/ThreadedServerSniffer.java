/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package threadedserversniffer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Server sniffer application which accepts a port number, number of iterations
 * and timeout value as arguments and will try random IP address to see if it
 * gets any replies on the designated port number.
 *
 * @author David
 */
public class ThreadedServerSniffer {

    public static int PORT;
    public static int LOOPS;
    public static int TIMEOUT_VALUE;

    public static int addressesChecked = 0;

    private static List<String> servers;
    private static List<String> possibleServers;

    private static Semaphore sem;
    private static Semaphore semPoss;

    /**
     * Main method which handles the main program logic.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        servers = new ArrayList<>();
        possibleServers = new ArrayList<>();

        sem = new Semaphore(1);
        semPoss = new Semaphore(1);

        if (args.length != 3) { //Check the arguments and generate a usage message if an inbalid  number of arguments were entered.
            System.out.println("Usage- [PORT] [ITTERATIONS] [TIMEOUT]");
        } else {
            PORT = Integer.parseInt(args[0]);
            LOOPS = Integer.parseInt(args[1]);
            TIMEOUT_VALUE = Integer.parseInt(args[2]);

            System.out.println("Checking " + LOOPS + " IP addresses on port " + PORT + " with a timeout value of " + TIMEOUT_VALUE + "ms");

            for (int i = 0; i < LOOPS; i++) {
                new ConnectionThread(generatePublicIP(), PORT, servers, possibleServers, sem, semPoss).start(); //Create the threads
            }

            while (addressesChecked != LOOPS) { //Wait here until all threads are completed excecution.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {

                }
            }

            System.out.println("");

            if (servers.isEmpty() && possibleServers.isEmpty()) { //Check if any servers were found.
                System.out.println("No servers found on port number " + PORT);
            } else {
                System.out.println(servers.size() + " server(s) and " + possibleServers.size() + " possible server(s) found on port number " + PORT + " -");
                if (!servers.isEmpty()) { //Check if any servers were found and display them.
                    System.out.println("Servers-");
                    for (String ip : servers) {
                        System.out.println(ip);
                    }
                }
                if (!possibleServers.isEmpty()) { //Check if any possible servers were found and display them.
                    System.out.println("\nPossible servers-");
                    for (String ip : possibleServers) {
                        System.out.println(ip);
                    }
                }
                System.out.println("\n" + (int) (servers.size() + possibleServers.size()) + " servers found");
            }
        }
    }

    /**
     * Method to generate a valid public IP address.
     *
     * @return valid public IP address as a String.
     */
    public static String generatePublicIP() {
        boolean valid = false;
        int octet1 = 0;
        int octet2 = 0;
        int octet3 = 0;
        int octet4 = 0;

        while (valid == false) {
            octet1 = (int) (Math.random() * 255);
            octet2 = (int) (Math.random() * 255);
            octet3 = (int) (Math.random() * 255);
            octet4 = (int) (Math.random() * 255);

            //Check that the IP is a valid usable public IP address.
            if (octet1 == 0 || (octet1 == 10 && octet2 >= 64 && octet2 <= 127) || octet1 == 127 || (octet1 == 169 && octet2 == 254) || (octet1 == 172 && octet2 >= 16 && octet2 <= 31) || (octet1 == 192 && octet2 == 0 && (octet3 == 0 || octet3 == 2)) || (octet1 == 192 && octet2 == 88 && octet3 == 99) || (octet1 == 192 && octet2 == 168) || (octet1 == 198 && (octet2 == 18 || (octet2 == 52 && octet2 == 100))) || (octet1 == 203 && octet2 == 0 && octet3 == 113) || octet1 >= 224) {
                valid = false;
            } else {
                valid = true;
            }
        }

        return octet1 + "." + octet2 + "." + octet3 + "." + octet4;
    }
}
