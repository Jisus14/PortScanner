package at.ac.hcw;

import at.ac.hcw.model.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PortScannerApp extends Application {
    private volatile boolean running;
    private AtomicInteger progressDoneCount;
    private static final DecimalFormat df = new DecimalFormat("0.00");

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
    private Button startBtn;
    @FXML
    private Button stopBtn;
    @FXML
    private Text progress;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextArea resultTextArea;

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
        primaryStage.setScene(new javafx.scene.Scene(root));
        primaryStage.show();
    }

    @FXML
    protected void onStopBtnClick() {
        //Starts a thread to terminate all the threads
        new Thread(() -> {
            running = false; //Disables the running flag so all thread end early
            for (Thread thread : threads) {
                try {
                    if (thread != null) {
                        thread.interrupt();
                        thread.join(); //Waits until the thread stopped
                        System.out.println(thread + "stopped"); //Debug
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Platform.runLater(() -> {
            resultTextArea.appendText("Scanning stopped! Following open ports have been found:\n");
        });
    }

    //When button is presses
    @FXML
    protected void onStartBtnClick() {

        //Checks if any of the integer inputs is invalid
        char[] portStartCheck = portStartInput.getText().toCharArray();
        char[] portEndCheck = portEndInput.getText().toCharArray();
        char[] maxThreadsCheck = maxThreadsInput.getText().toCharArray();
        char[] maxTimeoutCheck = maxTimeoutInput.getText().toCharArray();
        if(!checkValidity(portStartCheck)){
            startBtn.setText("Invalid from port");
            return;
        }
        if(!checkValidity(portEndCheck)){
            startBtn.setText("Invalid to port");
            return;
        }
        if(!checkValidity(maxThreadsCheck)){
            startBtn.setText("Invalid threads");
            return;
        }
        if(!checkValidity(maxTimeoutCheck)){
            startBtn.setText("Invalid timeout");
            return;
        }
        resultTextArea.clear();

        //Parses all the values to start scan
        String host = hostInput.getText();
        int portStart = Integer.parseInt(portStartInput.getText());
        int portEnd = Integer.parseInt(portEndInput.getText());
        int portsToScan = portEnd - portStart + 1;
        int numOfThreads = Integer.parseInt(maxThreadsInput.getText());
        progressDoneCount = new AtomicInteger(0);
        int portsPerThread = (portsToScan / numOfThreads) + 1; //Plus 1 maybe of rounding loss
        int timeout = Integer.parseInt(maxTimeoutInput.getText());

        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        startBtn.setText("Scanning...");
        running = true;

        long startTime = System.currentTimeMillis();

        resultTextArea.appendText("Scanning: " + host + "\n");

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
            scanners[i] = new ScannerApplication(host, start, end, timeout, progressDoneCount, ()-> running);
            threads[i] = new Thread(scanners[i]);
            threads[i].start();
        }

        //Progressbar
        new Thread(() -> {
            while(running) {
                double progressDecimal = portsToScan == 0
                        ? 1.0
                        : (double) progressDoneCount.get() / portsToScan;
                //Needs this otherwise error if not with runLater because this is in a thread
                Platform.runLater(() -> {
                    progressBar.setProgress(progressDecimal);
                    progress.setText(df.format(progressDecimal * 100) + "%");
                });

                try {
                    Thread.sleep(50); // Updates 20 times per second
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();


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
            allOpenPorts = allOpenPorts.stream().distinct().collect(Collectors.toList());
            Collections.sort(allOpenPorts);

            long endTime = System.currentTimeMillis();
            double durationSeconds = (endTime - startTime) / 1000.0;

            System.out.println("Open Ports: " + allOpenPorts);
            System.out.println("Scan finished in: " + df.format(durationSeconds) + "s");

            //Needs this otherwise error if not with runLater because this is in a thread
            String finalAllOpenPorts = allOpenPorts.toString().substring(1, allOpenPorts.toString().length() - 1); //To remove the brackets
            Platform.runLater(() -> {
                resultTextArea.appendText(finalAllOpenPorts + "\n");
                progressBar.setProgress(1.0);
                progress.setText(df.format(100.0) + "%");
                running = false;
                startBtn.setDisable(false);
                stopBtn.setDisable(true);
                startBtn.setText("Done, do again?");
            });
        }).start();
    }
}