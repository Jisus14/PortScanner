package at.ac.hcw;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;


public class Scanner extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    public static boolean pingHost(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            System.err.println("Could not connect to :" + port + e.getMessage());
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hello World!");
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                int[] results = new int[500];
                for (int i = 0; i < 500; i++){
                    boolean result = pingHost("localhost", i, 20000);
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
        });

        StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
    }
}
