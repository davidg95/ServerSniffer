/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serversniffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * The thread class to handle the individual connections, a new thread is
 * spawned for each IP address that is checked. A semaphore is used to protect
 * access to the list of IP addresses that are found.
 *
 * @author David
 */
public class ConnectionThread extends Thread {

    private final String HOST;
    private final int PORT;
    private final int TIMEOUT_VALUE;
    private final int LOOPS;
    private final List<String> servers;
    private final List<String> possibleServers;
    private final Semaphore sem;
    private final Semaphore semPoss;
    private final Socket s;

    /**
     * Constructor method for the thread.
     *
     * @param HOST the host which is being checked
     * @param PORT the port it is being checked on
     * @param TIMEOUT_VALUE the timeout value for the connection
     * @param LOOPS the number of IP addresses getting checked altogether, this
     * is required for the loading dots
     * @param servers the list for storing IP addresses which have servers
     * running on the specified port.
     * @param possibleServers the list for storing IP addresses which have
     * possible servers, an address is added to the list of no reply was
     * received but the connection gets blocked
     * @param sem the semaphore to protect the servers list
     * @param semPoss the semaphore to protect the possible servers list
     */
    public ConnectionThread(String HOST, int PORT, int TIMEOUT_VALUE, int LOOPS, List<String> servers, List<String> possibleServers, Semaphore sem, Semaphore semPoss) {
        this.HOST = HOST;
        this.PORT = PORT;
        this.TIMEOUT_VALUE = TIMEOUT_VALUE;
        this.LOOPS = LOOPS;
        this.servers = servers;
        this.possibleServers = possibleServers;
        this.sem = sem;
        this.semPoss = semPoss;
        s = new Socket();
    }

    /**
     * Main thread logic.
     */
    @Override
    public void run() {
        try {
            s.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_VALUE); //Try make a connection

            //Flow of excecution only reaches this point if a connection was successful.
            sem.acquire();
            ServerSniffer.addressesChecked++;
            if (ServerSniffer.gui) {
                if (ServerSniffer.addressesChecked % (double) (LOOPS / 100) == 0) { //Check how many addresses have been scanend and add anohter segment to the progress bar.
                    ServerSniffer.g.bar(1);
                }
                ServerSniffer.g.addAddress(HOST);
            }
            if (ServerSniffer.addressesChecked % (double) (LOOPS / 20) == 0) { //Check how many addresses have been scanend and add anohter dot to the progress bar.
                System.out.print(".");
            }
            servers.add(HOST);
            sem.release();

            s.close();
        } catch (IOException e) { //If full connection was not established.
            try {
                sem.acquire();
            } catch (InterruptedException ex) {
            }
            try {
                ServerSniffer.addressesChecked++;
                sem.release();
                if (ServerSniffer.gui) {
                    if (ServerSniffer.addressesChecked % (double) (LOOPS / 100) == 0) { //Check how many addresses have been scanend and add anohter segment to the progress bar.
                        ServerSniffer.g.bar(1);
                    }
                }
                if (ServerSniffer.addressesChecked % (double) (LOOPS / 20) == 0) {
                    if (!ServerSniffer.basic_output) {
                        System.out.print(".");
                    }
                }
                if (e.getMessage().equals("Connection refused: connect")) { //Check if the connection was forcibly blocked.
                    try {
                        semPoss.acquire();
                    } catch (InterruptedException ex) {
                    }
                    if (ServerSniffer.gui) {
                        ServerSniffer.g.addPossibleAddresses(HOST);
                    }
                    possibleServers.add(HOST);
                    semPoss.release();
                }
            } catch (NullPointerException en) {
            }
        } catch (InterruptedException ex) {
        }
    }
}
