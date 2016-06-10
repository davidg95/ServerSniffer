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

    String HOST;
    int PORT;
    List<String> servers;
    List<String> possibleServers;
    Semaphore sem;
    Semaphore semPoss;
    Socket s;

    /**
     * Constructor method for the thread.
     *
     * @param HOST the host which is being checked
     * @param PORT the port it is being checked on.
     * @param servers the list for storing IP addresses which have servers
     * running on the specified port.
     * @param possibleServers the list for storing IP addresses which have
     * possible servers, an address is added to the list of no reply was
     * received but the connection gets blocked.
     * @param sem the semaphore to protect the servers list.
     * @param semPoss the semaphore to protect the possible servers list.
     */
    public ConnectionThread(String HOST, int PORT, List<String> servers, List<String> possibleServers, Semaphore sem, Semaphore semPoss) {
        this.HOST = HOST;
        this.PORT = PORT;
        this.servers = servers;
        this.possibleServers = possibleServers;
        this.sem = sem;
        this.semPoss = semPoss;
    }

    /**
     * Main thread logic.
     */
    @Override
    public void run() {
        try {
            s = new Socket();
            s.connect(new InetSocketAddress(HOST, PORT), ServerSniffer.TIMEOUT_VALUE); //Try make a connection

            //Flow of excecution only reaches this point of a connection was successful.
            sem.acquire();
            ServerSniffer.addressesChecked++;
            if (ServerSniffer.addressesChecked % (ServerSniffer.LOOPS / 20) == 0) { //Check how many addresses have been scanend and add anohter dot to the progress bar.
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
                if (ServerSniffer.addressesChecked % (double) (ServerSniffer.LOOPS / 20) == 0) {
                    if (!ServerSniffer.basic_output) {
                        System.out.print(".");
                    }
                }
                if (e.getMessage().equals("Connection refused: connect")) { //Check if the connection was forcibly blocked.
                    try {
                        semPoss.acquire();
                    } catch (InterruptedException ex) {
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
