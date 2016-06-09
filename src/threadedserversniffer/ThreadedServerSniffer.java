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
 * gets any replies on the designated port number. it also accepts a
 * -basicoutput flag which will limit console output to IP addresses alone
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

    public static boolean basic_output = false;

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

        try {
            if (args[0].equals("?")) {
                System.out.println("Usage- [PORT] [ITTERATIONS] [TIMEOUT] -basicoutput");
                System.out.println("PORT - The port to check. Must be within range 0 to 65525. Integer.");
                System.out.println("ITTERATIONS - The number of times you want to loop. Integer.");
                System.out.println("TIMEOUT - The time out value to wait for a reply from each ip address. Integer.");
                System.out.println("-basicoutput - OPTIONAL. Only outputs IP addresses and nothing else.");
            } else if (args.length < 3 || args.length > 4) { //Check the arguments and generate a usage message if an inbalid  number of arguments were entered.
                throw new IllegalArgumentException("Usage- [PORT] [ITTERATIONS] [TIMEOUT]");
            } else {
                PORT = Integer.parseInt(args[0]);
                LOOPS = Integer.parseInt(args[1]);
                TIMEOUT_VALUE = Integer.parseInt(args[2]);

                if (args.length == 4) { //Check if the basic output flag was passsed in.
                    if (args[3].equals("-basicoutput")) {
                        basic_output = true;
                    } else {
                        throw new IllegalArgumentException("Usage- [PORT] [ITTERATIONS] [TIMEOUT] -basicoutput");
                    }
                }

                if (PORT < 0 || LOOPS < 0 || TIMEOUT_VALUE < 0) {
                    throw new IllegalArgumentException("Usage- [PORT] [ITTERATIONS] [TIMEOUT]");
                }

                if (!basic_output) {
                    System.out.println("Checking " + LOOPS + " IP addresses on port " + PORT + " with a timeout value of " + TIMEOUT_VALUE + "ms");
                }

                for (int i = 0; i < LOOPS; i++) {
                    new ConnectionThread(generatePublicIP(), PORT, servers, possibleServers, sem, semPoss).start(); //Create the threads
                }

                if (LOOPS < 20) {
                    if (!basic_output) {
                        System.out.println("....................");
                    }
                }

                while (addressesChecked != LOOPS) { //Wait here until all threads are completed excecution.
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {

                    }
                }

                if (!basic_output) {
                    System.out.println("");
                }

                if (servers.isEmpty() && possibleServers.isEmpty()) { //Check if any servers were found.
                    if (!basic_output) {
                        System.out.println("No servers found on port number " + PORT);
                    }
                } else {
                    if (!basic_output) {
                        System.out.println(servers.size() + " server(s) and " + possibleServers.size() + " possible server(s) found on port number " + PORT + " -");
                    }
                    if (!servers.isEmpty()) { //Check if any servers were found and display them.
                        if (!basic_output) {
                            System.out.println("Servers-");
                        }
                        for (String ip : servers) {
                            System.out.println(ip);
                        }
                    }
                    if (!possibleServers.isEmpty()) { //Check if any possible servers were found and display them.
                        if (!basic_output) {
                            System.out.println("\nPossible servers-");
                        }
                        for (String ip : possibleServers) {
                            System.out.println(ip);
                        }
                    }
                    if (!basic_output) {
                        System.out.println("\n" + (int) (servers.size() + possibleServers.size()) + " servers found");
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
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
