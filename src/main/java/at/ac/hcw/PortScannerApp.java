package at.ac.hcw;

import at.ac.hcw.model.*;
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PortScannerApp extends Application {
    private volatile boolean running;
    private boolean cancelOutput;
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

    //Primary stage is the splash screen with fake loading
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("splash.fxml"));
        Parent splashRoot = loader.load();

        primaryStage.setTitle("Loading...");
        primaryStage.setScene(new javafx.scene.Scene(splashRoot));
        primaryStage.centerOnScreen();
        primaryStage.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(3)); //Pause for 3 seconds then changes into main portscanner scene

        pause.setOnFinished(event -> {
            try {
                showMainScene(primaryStage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        pause.play();
    }


    private void showMainScene(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();

        stage.setTitle("Port Scanner");
        stage.setScene(new javafx.scene.Scene(root));

        stage.setMinWidth(800);
        stage.setMinHeight(650);

        stage.show();
    }

    //When button is presses
    @FXML
    protected void onStartBtnClick() {
        int portStart, portEnd, numOfThreads, timeout;
        String host = hostInput.getText();
        progressDoneCount = new AtomicInteger(0);

        //Checks if any input is invalid
        //Must be number and 0-65535
        try {
            portStart = Integer.parseInt(portStartInput.getText().trim());
            if (portStart < 0 || portStart > 65535) {
                startBtn.setText("From port must be 0-65535");
                return;
            }
        } catch (NumberFormatException e) {
            startBtn.setText("Invalid From port");
            return;
        }

        //Must be number and 0-65535
        try {
            portEnd = Integer.parseInt(portEndInput.getText().trim());
            if (portEnd < 0 || portEnd > 65535) {
                startBtn.setText("To Port must be 0-65535");
                return;
            }
        } catch (NumberFormatException e) {
            startBtn.setText("Invalid To port");
            return;
        }

        //Start port is greater than end port
        if (portStart > portEnd) {
            startBtn.setText("From port > To port");
            return;
        }

        //Threads greater than 0 and not too many
        try {
            numOfThreads = Integer.parseInt(maxThreadsInput.getText().trim());
            if (numOfThreads < 1) { // Can't have 0 or negative threads
                startBtn.setText("Max threads must be > 0");
                return;
            }

            if (numOfThreads > 1000) {
                startBtn.setText("Max 1000 threads allowed");
                return;
            }
        } catch (NumberFormatException e) {
            startBtn.setText("Invalid max Threads");
            return;
        }

        //Timeout greater than 0
        try {
            timeout = Integer.parseInt(maxTimeoutInput.getText().trim());
            if (timeout < 0) {
                startBtn.setText("Timeout cannot be negative");
                return;
            }
        } catch (NumberFormatException e) {
            startBtn.setText("Invalid Timeout");
            return;
        }

        resultTextArea.clear();

        //Port spread can now be done after validation
        int portsToScan = portEnd - portStart + 1;
        int portsPerThread = (portsToScan / numOfThreads) + 1; //Plus 1 maybe of rounding loss

        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        startBtn.setText("Scanning...");
        running = true;
        cancelOutput = false;

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

            Path folderPath = Paths.get("results");
            //Creates folder if there is none
            try {
                Files.createDirectories(folderPath);
            } catch (IOException e) {
                System.err.println("Failed to create directory: " + e.getMessage());
            }

            String fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss'.csv'").format(new Date());
            Path filePath = folderPath.resolve(fileName);

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
            if (cancelOutput){
                Platform.runLater(() -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    startBtn.setText("Done, do again?");
                });
                return;
            }

            //Sorts and prints the list
            allOpenPorts = allOpenPorts.stream().distinct().collect(Collectors.toList());
            Collections.sort(allOpenPorts);

            long endTime = System.currentTimeMillis();
            double durationSeconds = (endTime - startTime) / 1000.0;

            System.out.println("Open Ports: " + allOpenPorts);
            System.out.println("Scan finished in: " + df.format(durationSeconds) + "s");
            String finalAllOpenPorts = allOpenPorts.toString().substring(1, allOpenPorts.toString().length() - 1); //To remove the brackets
            String logFileOutput = "Host,Port,Status\n"; // CSV Header

            if (allOpenPorts.isEmpty()) {
                logFileOutput += host + ", ,No open ports\n";
            } else {
                for (Integer port : allOpenPorts) {
                    logFileOutput += host + "," + port + ",Open\n";
                }
            }

            try{
                Files.writeString(filePath, logFileOutput, StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Needs this otherwise error if not with runLater because this is in a thread
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

    //Stop button pressed
    @FXML
    protected void onStopBtnClick() {
        //Starts a thread to terminate all the threads
        new Thread(() -> {
            //Disables the running flag so all thread end early
            cancelOutput = true;
            running = false;
            for (Thread thread : threads) {
                try {
                    if (thread != null) {
                        thread.join(); //Waits until the thread stopped
                        System.out.println(thread + "stopped"); //Debug
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Platform.runLater(() -> {
            resultTextArea.appendText("Scanning aborted! \n");
        });
    }
}