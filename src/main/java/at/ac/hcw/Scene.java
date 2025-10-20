package at.ac.hcw;

import at.ac.hcw.model.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;



public class Scene extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Port Scanner");
        Button btn = new Button();
        TextField hostInput = new TextField();
        hostInput.setPromptText("Enter host or IP address");
        btn.setText("Scan Port");
        //When button is presses
        btn.setOnAction(event -> {
            btn.setDisable(true);
            btn.setText("Scanning...");

            //Temp vars because no UI yet
            int numOfThreads = 500;
            int portStart = 0;
            int portEnd = 65535;
            int timeout = 200;

            //Not this
            String host = hostInput.getText();
            int portsPerThread = ((portEnd - portStart) / numOfThreads) + 1; //Plus 1 maybe of rounding loss


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

            //New Thread otherwise UI freeze
            new Thread(() -> {
                System.out.println("Scanning: " + host);

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

                //Sorts and prints the list
                Collections.sort(allOpenPorts);
                System.out.println(allOpenPorts);
                //Needs this otherwise error if not with runLater because this is in a thread
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("Done, do again?");
                });
            }).start();
        }
        );

        VBox root = new VBox(10); // 10px spacing
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(hostInput, btn);
        primaryStage.setScene(new javafx.scene.Scene(root, 300, 250));
        primaryStage.show();
    }
}
