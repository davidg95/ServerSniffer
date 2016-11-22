/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serversniffer;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Server sniffer application which accepts a port number, number of iterations
 * and timeout value as arguments and will try random IP address to see if it
 * gets any replies on the designated port number. it also accepts a -b flag
 * which will limit console output to IP addresses alone
 *
 * @author David
 */
public class ServerSniffer {

    public int PORT;
    public int LOOPS;
    public int TIMEOUT_VALUE;

    private long startTime;
    private long finnishTime;
    private long duration;

    public static int addressesChecked = 0;

    private List<String> servers;
    private List<String> possibleServers;

    private Semaphore sem;
    private Semaphore semPoss;

    public static GUI g;
    public static boolean run;
    public static boolean save;

    public static boolean gui = false;
    public static boolean basic_output = false;

    /**
     * Main method which takes in the arguments and creates a new ServerSniffer
     * object.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                if (!GraphicsEnvironment.isHeadless()) {
                    gui = true;
                    g = new GUI();
                    g.setVisible(true);
                } else {
                    System.out.println("No graphics enviroment detected");
                    System.out.println("Usage- [PORT] [ITTERATIONS] [TIMEOUT] -b");
                    System.out.println("PORT - The port to check. Must be within range 0 to 65525. Integer.");
                    System.out.println("ITTERATIONS - The number of times you want to loop. Integer.");
                    System.out.println("TIMEOUT - The time out value to wait for a reply from each ip address. Integer.");
                    System.out.println("-b - OPTIONAL. Only outputs IP addresses and nothing else.");
                }
            } else {
                if (args[0].equals("?")) {
                    System.out.println("Usage- [PORT] [ITTERATIONS] [TIMEOUT] -b");
                    System.out.println("PORT - The port to check. Must be within range 0 to 65525. Integer.");
                    System.out.println("ITTERATIONS - The number of times you want to loop. Integer.");
                    System.out.println("TIMEOUT - The time out value to wait for a reply from each ip address. Integer.");
                    System.out.println("-b - OPTIONAL. Only outputs IP addresses and nothing else.");
                } else if (args.length < 3 || args.length > 4) { //Check the arguments and generate a usage message if an inbalid  number of arguments were entered.
                    throw new IllegalArgumentException("Usage- [PORT] [ITTERATIONS] [TIMEOUT]");
                } else {
                    int PORT = Integer.parseInt(args[0]);
                    int LOOPS = Integer.parseInt(args[1]);
                    int TIMEOUT_VALUE = Integer.parseInt(args[2]);

                    if (args.length == 4) { //Check if the basic output flag was passsed in.
                        if (args[3].equals("-b")) {
                            basic_output = true;
                        } else {
                            throw new IllegalArgumentException("Usage- [PORT] [ITTERATIONS] [TIMEOUT] -b");
                        }
                    }

                    if (PORT < 0 || LOOPS < 0 || TIMEOUT_VALUE < 0) {
                        throw new IllegalArgumentException("Usage- [PORT] [ITTERATIONS] [TIMEOUT]");
                    }

                    if (PORT > 65535) {
                        throw new IllegalArgumentException("Port must be in range 0-65535");
                    }

                    new ServerSniffer(PORT, LOOPS, TIMEOUT_VALUE).start(); //Start running the scanner.
                }
            }
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Blank constructor to initialise the sniffer.
     *
     * @param PORT the port number getting checked
     * @param LOOPS number of addresses to check
     * @param TIMEOUT_VALUE the timeout values for each connection
     */
    public ServerSniffer(int PORT, int LOOPS, int TIMEOUT_VALUE) {
        this.PORT = PORT;
        this.LOOPS = LOOPS;
        this.TIMEOUT_VALUE = TIMEOUT_VALUE;
    }

    /**
     * Method which handles the main logic.
     */
    public void start() {
        servers = new ArrayList<>();
        possibleServers = new ArrayList<>();
        addressesChecked = 0;
        run = true;

        sem = new Semaphore(1);
        semPoss = new Semaphore(1);

        if (!basic_output) {
            System.out.println("Checking " + LOOPS + " IP addresses on port " + PORT + " with a timeout value of " + TIMEOUT_VALUE + "ms");
        }

        if (gui) {
            g.log("Checking " + LOOPS + " IP addresses on port " + PORT + " with a timeout value of " + TIMEOUT_VALUE + "ms");
        }

        startTime = Calendar.getInstance().getTimeInMillis();

        try {
            for (int i = 0; i < LOOPS; i++) {
                new ConnectionThread(generatePublicIP(), PORT, TIMEOUT_VALUE, LOOPS, servers, possibleServers, sem, semPoss).start(); //Create the threads
                if (!run) {
                    break;
                }
            }

            if (LOOPS < 20) { //If loops is less than 20 then just print 20 dots to screen as the thread wont be able to.
                if (!basic_output) {
                    System.out.println("....................");
                }
            }

            while (addressesChecked != LOOPS) { //Wait here until all threads are completed excecution.
                if (!run) {
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {

                }
            }
        } catch (OutOfMemoryError ex) {
            if (gui) {
                g.log("Out of memory");
                g.showDialog("There is not enough avaliable memory on your system to complete the scan.");
            }
        }

        finnishTime = Calendar.getInstance().getTimeInMillis();

        duration = finnishTime - startTime;

        if (gui) {
            g.complete();
            double dur = duration;
            g.log("Scan complete in " + (dur / 1000) + "s!");
            if (servers.isEmpty() && possibleServers.isEmpty()) {
                g.log("No servers found on port number " + PORT);
            } else {
                g.log(servers.size() + " server(s) and " + possibleServers.size() + " possible server(s) found on port number " + PORT);
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

                servers.forEach((ip) -> {
                    System.out.println(ip);
                });

                if (save) {
                    try {
                        File file = new File("port" + PORT + ".txt");
                        file.delete();
                        try (FileWriter writer = new FileWriter(file, true); PrintWriter out = new PrintWriter(writer)) {
                            servers.forEach((ip) -> {
                                out.println(ip);
                            });
                            possibleServers.forEach((ip) -> {
                                out.println(ip);
                            });
                            out.flush();
                        }
                        g.log("Results saved to " + file.getAbsolutePath());
                        Desktop.getDesktop().open(file);
                    } catch (IOException ex) {

                    }
                }
            }
            if (!possibleServers.isEmpty()) { //Check if any possible servers were found and display them.
                if (!basic_output) {
                    System.out.println("\nPossible servers-");
                }
                possibleServers.forEach((ip) -> {
                    System.out.println(ip);
                });
            }
            if (!basic_output) {
                System.out.println("\n" + (int) (servers.size() + possibleServers.size()) + " servers found");
            }
        }
    }

    /**
     * Method to generate a valid public IP address.
     *
     * @return valid public IP address as a String.
     */
    public String generatePublicIP() {
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
            //From https://en.wikipedia.org/wiki/Reserved_IP_addresses.
            valid = !(octet1 == 0
                    || //0.0.0.0 - 0.255.255.255
                    octet1 == 10
                    || //10.0.0.0 - 10.255.255.255
                    (octet1 == 10 && octet2 > 64 && octet2 < 127)
                    || //10.64.0.0 - 10.127.255.255
                    octet1 == 127
                    || //127.0.0.0 - 127.255.255.255
                    (octet1 == 169 && octet2 == 254)
                    || //169.254.0.0 - 169.254.255.255
                    (octet1 == 172 && octet2 >= 16 && octet2 <= 31)
                    || //172.16.0.0 - 172.32.255.255
                    (octet1 == 192 && octet2 == 0 && octet3 == 0)
                    || //192.0.0.0 - 192.0.0.255
                    (octet1 == 192 && octet2 == 0 && octet3 == 2)
                    || //192.0.2.0 - 192.0.2.255
                    (octet1 == 192 && octet2 == 88 && octet3 == 99)
                    || //192.88.99.0 - 192.88.99.255
                    (octet1 == 192 && octet2 == 168)
                    || //192.168.0.0 - 192.168.255.255
                    (octet1 == 198 && (octet2 == 18 || octet2 == 19))
                    || //198.18.0.0 - 198.19.255.255
                    (octet1 == 198 && octet2 == 51 && octet3 == 100)
                    || //198.51.100.0 - 198.51.100.255
                    (octet1 == 203 && octet2 == 0 && octet3 == 113)
                    || //203.0.113.0 - 203.0.113.255
                    octet1 >= 224); //224.0.0.0 - 255.255.255.255
        }

        return octet1 + "." + octet2 + "." + octet3 + "." + octet4;
    }
}
