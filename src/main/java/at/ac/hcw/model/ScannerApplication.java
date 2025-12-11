package at.ac.hcw.model;

import at.ac.hcw.Scene;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.ArrayList;

public class ScannerApplication implements Runnable {

    //vars
    private final String host;
    private final int portStart;
    private final int portEnd;
    private final int timeout;
    private final List<Integer> openPorts = new ArrayList<>();

    //const
    public ScannerApplication(String host, int portStart, int portEnd, int timeout) {
        this.host = host;
        this.portStart = portStart;
        this.portEnd = portEnd;
        this.timeout = timeout;
    }

    //Main function of Runnable
    public void run() {
        for (int port = portStart; port <= portEnd; port++) {
            Scene.progressDoneCount.incrementAndGet();
            if (pingHost(host, port, timeout)) {
                openPorts.add(port);
            }
            if(!Scene.running){
                break;
            }
        }
    }

    //So Scene can get the open ports of every scanner
    public List<Integer> getOpenPorts() {
        return openPorts;
    }

    //Pings IP with port to get response -> port open
    public static boolean pingHost(String host, int port, int timeout) {
        try(Socket socket = new Socket()) {
            InetSocketAddress address = new InetSocketAddress(host, port);
            socket.connect(address, timeout); //Tries to connect to the given address on a port, will error out when closed or exceed timeout
            return true;
        }catch(SocketTimeoutException e){
            //System.err.println("Port " + port + ": TIMEOUT " + e.getMessage());
            return false;
        } catch (IOException e) {
            //Debug
            //System.err.println("Port " + port + ": REFUSE " + e.getMessage());
            return false;
        }
    }
}
