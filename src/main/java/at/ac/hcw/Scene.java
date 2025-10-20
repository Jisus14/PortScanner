package at.ac.hcw;

import at.ac.hcw.model.*;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


public class Scene extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Port Scanner");
        Button btn = new Button();
        btn.setText("Scan Port");
        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                //For async scanning
                int numOfThreads = 20;
                String host = "localhost";
                int portStart = 0;
                int portEnd = 10000;
                int timeout = 200;
                int portsPerThread = ((portEnd - portStart) / numOfThreads) + 1; //Plus 1 maybe of rounding loss
                Thread[] threads = new Thread[numOfThreads];

                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new Thread(new ScannerApplication(host,portStart + ((i*portsPerThread)), portStart + ((i+1)*portsPerThread), timeout));
                    threads[i].start();
                }
            }
        });

        StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new javafx.scene.Scene(root, 300, 250));
        primaryStage.show();
    }
}
