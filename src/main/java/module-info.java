module com.example.scanner {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;

    requires org.controlsfx.controls;

    // FXML needs reflection access to your controller package:
    opens at.ac.hcw to javafx.fxml;

    // Export your main package:
    exports at.ac.hcw;
}
