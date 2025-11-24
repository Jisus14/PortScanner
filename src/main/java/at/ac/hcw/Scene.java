package at.ac.hcw;

import at.ac.hcw.model.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Scene extends Application {
    public static boolean running;

    @FXML
    private TextField hostInput;
    @FXML
    private TextField portStartInput;
    @FXML
    private TextField portEndInput;
    @FXML
    private TextField maxThreadsInput;
    @FXML
    private TextField maxTimeoutInput;
    @FXML
    private Button start_btn;
    @FXML
    private Button stop_btn;

    private Thread[] threads;
    private ScannerApplication[] scanners;

    private boolean checkValidity(char[] toCheck){
        for(int i = 0; i < toCheck.length; i++){
            if(toCheck[i] < '0' || toCheck[i] > '9'){
                return false;
            }
        }
        return true;
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Port Scanner");
        primaryStage.setScene(new javafx.scene.Scene(root, 500, 400));
        primaryStage.show();
    }

    @FXML
    protected void onStopBtnClick() {
        //Starts a thread to terminate all the threads
        new Thread(() ->{
            running = false; //Disables the running flag so all thread end early
            for (int i = 0; i < threads.length; i++) {
                try{
                    if (threads[i] != null) {
                        threads[i].interrupt();
                        threads[i].join(); //Waits until the thread stopped
                        System.out.println(threads[i] + "stopped"); //Debug
                    }
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //When button is presses
    @FXML
    protected void onStartBtnClick() {

        //Cheks if any of the integer inputs is invalid
        char[] portStartCheck = portStartInput.getText().toCharArray();
        char[] portEndCheck = portEndInput.getText().toCharArray();
        char[] maxThreadsCheck = maxThreadsInput.getText().toCharArray();
        char[] maxTimeoutCheck = maxTimeoutInput.getText().toCharArray();
        if(!checkValidity(portStartCheck)){
            start_btn.setText("Invalid port");
            return;
        }
        if(!checkValidity(portEndCheck)){
            start_btn.setText("Invalid port");
            return;
        }
        if(!checkValidity(maxThreadsCheck)){
            start_btn.setText("Invalid threads");
            return;
        }
        if(!checkValidity(maxTimeoutCheck)){
            start_btn.setText("Invalid timeout");
            return;
        }

        //Parses all the values to start scan
        String host = hostInput.getText();
        int portStart = Integer.parseInt(portStartInput.getText());
        int portEnd = Integer.parseInt(portEndInput.getText());
        int numOfThreads = Integer.parseInt(maxThreadsInput.getText());
        int portsPerThread = ((portEnd - portStart) / numOfThreads) + 1; //Plus 1 maybe of rounding loss
        int timeout = Integer.parseInt(maxTimeoutInput.getText());


        start_btn.setDisable(true);
        stop_btn.setDisable(false);
        start_btn.setText("Scanning...");
        running = true;


        System.out.println("Scanning: " + host);

        //Splits work into multiple workers.
        //2 Arrays so they are targetable for summerization of open ports
        threads = new Thread[numOfThreads];
        scanners = new ScannerApplication[numOfThreads];

        //Starts loop and gives every worker a set of ports depending on how many threads etc.
        for (int i = 0; i < threads.length; i++) {
            int start = portStart + ((i * portsPerThread));
            int end = start + portsPerThread;

            //Makes sure no false ports are scanned
            if (end > portEnd) {
                end = portEnd;
            }
            if (start < portStart) {
                start = portStart;
            }
            scanners[i] = new ScannerApplication(host, start, end, timeout);
            threads[i] = new Thread(scanners[i]);
            threads[i].start();
        }

        //New Thread otherwise UI freeze
        new Thread(() -> {

            List<Integer> allOpenPorts = new ArrayList<>();

            //Collects all open ports from each scanner to allOpenPorts
            for (int i = 0; i < threads.length; i++) {
                try {
                    if (threads[i] != null) {
                        threads[i].join();
                        allOpenPorts.addAll(scanners[i].getOpenPorts());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            running = false;
            //Sorts and prints the list
            Collections.sort(allOpenPorts);
            System.out.println(allOpenPorts);
            //Needs this otherwise error if not with runLater because this is in a thread
            Platform.runLater(() -> {
                running = false;
                start_btn.setDisable(false);
                stop_btn.setDisable(true);
                start_btn.setText("Done, do again?");
            });
        }).start();
    }
}