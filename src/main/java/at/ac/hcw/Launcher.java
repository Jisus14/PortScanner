package at.ac.hcw;

import javafx.application.Application;

/**
 * Launcher-Klasse:
 * Startpunkt (Entry Point) deines Programms über die main()-Methode.
 *
 * Wozu braucht man das?
 * - Manche Setups/Builds/IDEs mögen es, wenn es eine "normale" main() gibt.
 * - Damit startest du sauber deine JavaFX Application-Klasse (PortScannerApp).
 */
public class Launcher {

    /**
     * main() ist der Einstiegspunkt für Java-Programme.
     * Java startet hier automatisch mit dem String[] args (Kommandozeilen-Argumente).
     */
    public static void main(String[] args) {

        // Startet die JavaFX Runtime und ruft intern PortScannerApp.start(Stage) auf.
        // PortScannerApp MUSS von javafx.application.Application erben.
        Application.launch(PortScannerApp.class, args);
    }
}
