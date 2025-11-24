package at.ac.hcw;

import at.ac.hcw.model.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;



public class Scene extends Application {
    public static boolean running;

    private boolean checkValidity(char[] toCheck){
        for(int i = 0; i < toCheck.length; i++){
            if(toCheck[i] < '0' || toCheck[i] > '9'){
                return false;
            }
        }
        return true;
    }


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Port Scanner");
        //FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));

        Button start_btn = new Button();
        Button stop_btn = new Button();

        TextField hostInput = new TextField("localhost");
        Label targetLabel = new Label("Target address");
        targetLabel.setLabelFor(hostInput);

        TextField portStartInput = new TextField("0");
        Label portStartLabel = new Label("Starting port");
        portStartLabel.setLabelFor(portStartInput);

        TextField portEndInput = new TextField("100");
        Label portEndLabel = new Label("End port");
        portEndLabel.setLabelFor(portEndInput);

        TextField maxThreadsInput = new TextField("20");
        Label maxThreadsLabel = new Label("Maximum of Threads");
        maxThreadsLabel.setLabelFor(maxThreadsInput);

        TextField maxTimeoutInput = new TextField("200");
        Label maxTimeoutLabel = new Label("Maximum timeout");
        maxTimeoutLabel.setLabelFor(maxTimeoutInput);

        start_btn.setText("Scan port");
        stop_btn.setText("Stop");
        stop_btn.setDisable(true);


        //When button is presses
        start_btn.setOnAction(event -> {

            //Cheks if any of the integer inputs is invalid
            Boolean canRun = true;
            char[] portStartCheck = portStartInput.getText().toCharArray();
            char[] portEndCheck = portEndInput.getText().toCharArray();
            char[] maxThreadsCheck = maxThreadsInput.getText().toCharArray();
            char[] maxTimeoutCheck = maxTimeoutInput.getText().toCharArray();
            if(!checkValidity(portStartCheck)){
                canRun = false;
                start_btn.setText("Invalid port");
            }
            if(!checkValidity(portEndCheck)){
                canRun = false;
                start_btn.setText("Invalid port");
            }
            if(!checkValidity(maxThreadsCheck)){
                canRun = false;
                start_btn.setText("Invalid threads");
            }
            if(!checkValidity(maxTimeoutCheck)){
                canRun = false;
                start_btn.setText("Invalid timeout");
            }

            if(canRun){
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
                Thread[] threads = new Thread[numOfThreads];
                ScannerApplication[] scanners = new ScannerApplication[numOfThreads];

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

                //Starts a thread to terminate all the threads
                new Thread(() ->{
                    while(threads[0].isAlive()){
                        stop_btn.setOnAction(stopEvent ->{
                            running = false; //Disables the running flag so all thread end early
                            for (int i = 0; i < threads.length; i++) {
                                try{
                                    threads[i].interrupt();
                                    threads[i].join(); //Waits until the thread stopped
                                    System.out.println(threads[i] + "stopped"); //Debug
                                }catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }).start();

                //New Thread otherwise UI freeze
                new Thread(() -> {

                    List<Integer> allOpenPorts = new ArrayList<>();

                    //Collects all open ports from each scanner to allOpenPorts
                    for (int i = 0; i < threads.length; i++) {
                        try {
                            threads[i].join();
                            allOpenPorts.addAll(scanners[i].getOpenPorts());
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
            }}
            );

            VBox root = new VBox(10);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));
            root.getChildren().addAll(targetLabel, hostInput,portStartLabel,portStartInput,portEndLabel,portEndInput,maxThreadsLabel, maxThreadsInput,maxTimeoutLabel, maxTimeoutInput, start_btn, stop_btn);
            primaryStage.setScene(new javafx.scene.Scene(root, 500, 400));
            primaryStage.show();
        }
}
