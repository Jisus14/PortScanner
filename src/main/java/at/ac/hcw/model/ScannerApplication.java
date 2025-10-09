package at.ac.hcw.model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ScannerApplication {

    public static boolean pingHost(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            System.err.println("Could not connect to :" + port + e.getMessage());
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }

    public static void scanPort(String host, int portStart, int portEnd, int maxThreads, int[] ignorePort, int timeout) {
        int[] results = new int[portEnd - portStart + 1];
        for (int i = portStart; i < portEnd; i++){
            boolean result = pingHost(host, i, timeout);
            if(result){
                results[i] = i;
            }
            System.out.println(i + ":" + result);
        }

        for (int i = 0; i < results.length; i++){
            if(results[i] != 0){
                System.out.println(results[i]);
            }

        }
    }

    public static void scanPort(String host, int portStart, int portEnd,int maxThreads, int[] ignorePort) {
        scanPort(host, portStart, portEnd, maxThreads, ignorePort, 200);
    }
    public static void scanPort(String host, int portStart, int portEnd, int maxThreads) {
        scanPort(host, portStart, portEnd, maxThreads, new int[]{});
    }
    public static void scanPort(String host, int portStart, int portEnd) {
        scanPort(host, portStart, portEnd, 0);
    }
    public static void scanPort(String host, int portStart) {
        scanPort(host, portStart, 65535);
    }
    public static void scanPort(String host) {
        scanPort(host, 1);
    }

    public static void main(String[] args) {

    }
}
