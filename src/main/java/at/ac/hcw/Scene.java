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
                Thread newThread = new Thread(() -> {
                    ScannerApplication.scanPort("1.1.1.1", 1, 500);
                });
                newThread.start();
            }
        });

        StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new javafx.scene.Scene(root, 300, 250));
        primaryStage.show();
    }
}
