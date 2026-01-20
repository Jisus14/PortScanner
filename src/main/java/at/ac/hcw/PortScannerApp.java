package at.ac.hcw;

import at.ac.hcw.model.*; // eigene Klassen (z.B. ScannerApplication)

import javafx.animation.PauseTransition;       // Timer für Splash Screen
import javafx.application.Application;         // Basisklasse für JavaFX Apps
import javafx.application.Platform;            // UI Updates aus Threads (runLater)
import javafx.fxml.FXML;                       // Verbindung FXML -> Controller
import javafx.fxml.FXMLLoader;                 // Lädt FXML Dateien
import javafx.scene.Parent;                    // Root Node einer Scene
import javafx.scene.control.Button;            // Button UI Element
import javafx.scene.control.ProgressBar;       // Fortschrittsbalken
import javafx.scene.control.TextArea;          // Textausgabe (mehrzeilig)
import javafx.scene.control.TextField;         // Eingabefelder
import javafx.scene.text.Text;                 // UI Textanzeige
import javafx.stage.Stage;                     // Fenster
import javafx.util.Duration;                   // Zeitangabe (z.B. 3 Sekunden)

import java.io.IOException;                    // Exception bei IO
import java.nio.file.Files;                    // Ordner/Datei Aktionen
import java.nio.file.Path;                     // Dateipfad Objekt
import java.nio.file.Paths;                    // Erzeugt Path aus String
import java.nio.file.StandardOpenOption;       // Datei-Schreiboptionen

import java.text.DecimalFormat;                // Format für Prozent / Zeit
import java.text.SimpleDateFormat;             // Dateiname mit Datum
import java.util.ArrayList;                    // Liste für Ports
import java.util.Collections;                  // Sortieren
import java.util.Date;                         // aktuelles Datum/Zeit
import java.util.List;                         // Interface für Listen

import java.util.concurrent.atomic.AtomicInteger; // thread-sicherer Counter
import java.util.stream.Collectors;            // Stream/Collect für distinct()

/**
 * Haupt-JavaFX Anwendung.
 * - zeigt Splash Screen
 * - zeigt Main GUI
 * - startet Multi-Thread Port Scan
 * - schreibt Ergebnis als CSV
 */
public class PortScannerApp extends Application {

    // volatile => Änderungen (true/false) werden sofort in Threads sichtbar
    private volatile boolean running; //Sorgt dafür, dass alle Threads immer den aktuellsten Wert sehen.

    // Wenn true => Scan wurde abgebrochen, Ergebnis wird nicht mehr geschrieben/ausgegeben
    private boolean cancelOutput;

    // Thread-sicherer Fortschrittszähler (alle Worker erhöhen ihn)
    private AtomicInteger progressDoneCount;

    // Formatierung für Prozentanzeige
    private static final DecimalFormat df = new DecimalFormat("0.00");//erstellt ein Formatierungs-Objekt, mit dem du Zahlen immer mit 2 Nachkommastellen als String ausgeben kannst

    // ===== GUI Elemente aus main.fxml =====
    @FXML private TextField hostInput;
    @FXML private TextField portStartInput;
    @FXML private TextField portEndInput;
    @FXML private TextField maxThreadsInput;
    @FXML private TextField maxTimeoutInput;

    @FXML private Button startBtn;
    @FXML private Button stopBtn;

    @FXML private Text progress;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea resultTextArea;

    // Threads und Scanner in Arrays, damit man Ergebnisse später sammeln kann
    private Thread[] threads;
    private ScannerApplication[] scanners;

    /**
     * JavaFX Start-Methode.
     * Zeigt zuerst splash.fxml und wechselt nach 3 Sekunden auf main.fxml.
     */
    @Override
    public void start(Stage primaryStage) throws IOException {

        // Splash FXML laden
        FXMLLoader loader = new FXMLLoader(getClass().getResource("splash.fxml"));
        Parent splashRoot = loader.load();

        primaryStage.setTitle("Loading...");
        primaryStage.setScene(new javafx.scene.Scene(splashRoot));
        primaryStage.centerOnScreen();
        primaryStage.show();

        // PauseTransition = Timer (3 Sekunden warten)
        PauseTransition pause = new PauseTransition(Duration.seconds(3));

        // Nach Ablauf: Hauptfenster zeigen
        pause.setOnFinished(event -> {
            try {
                showMainScene(primaryStage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        pause.play();
    }

    /**
     * Lädt main.fxml und zeigt das Hauptfenster.
     */
    private void showMainScene(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();

        stage.setTitle("Port Scanner");
        stage.setScene(new javafx.scene.Scene(root));

        stage.setMinWidth(800);
        stage.setMinHeight(650);

        stage.show();
    }

    /**
     * Start Button: validiert Eingaben, startet Threads und Scan-Logik.
     */
    @FXML
    protected void onStartBtnClick() {

        int portStart, portEnd, numOfThreads, timeout;

        // Host lesen
        String host = hostInput.getText();

        // Fortschritt neu starten
        progressDoneCount = new AtomicInteger(0);

        // ===== Validierung: Start Port =====
        try {
            portStart = Integer.parseInt(portStartInput.getText().trim()); //liest eine Zahl aus einem Textfeld und speichert sie als int in der Variable portStart
            if (portStart < 0 || portStart > 65535) {
                startBtn.setText("From port must be 0-65535");
                return;
            }
        } catch (NumberFormatException e) {
            startBtn.setText("Invalid From port");
            return;
        }

        // ===== Validierung: End Port =====
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

        // Start darf nicht größer als End sein
        if (portStart > portEnd) {
            startBtn.setText("From port > To port");
            return;
        }

        // ===== Validierung: Threads =====
        try {
            numOfThreads = Integer.parseInt(maxThreadsInput.getText().trim());

            if (numOfThreads < 1) {
                startBtn.setText("Max threads must be > 0");
                return;
            }

            // Sicherheitslimit, damit das Programm nicht “explodiert”
            if (numOfThreads > 1000) {
                startBtn.setText("Max 1000 threads allowed");
                return;
            }
        } catch (NumberFormatException e) {
            startBtn.setText("Invalid max Threads");
            return;
        }

        // ===== Validierung: Timeout =====
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

        // ===== Port Bereich auf Threads aufteilen =====
        int portsToScan = portEnd - portStart + 1;

        // +1, um Rundungsfehler bei Integer-Division abzufangen
        int portsPerThread = (portsToScan / numOfThreads) + 1;

        // Buttons sperren/aktivieren
        startBtn.setDisable(true);//port darf nicht mehr geklickt werden wenn der button schon einmal geklickt wurde
        stopBtn.setDisable(false); //stop button ist klickbar - kannst stoppen
        startBtn.setText("Scanning...");

        running = true;
        cancelOutput = false;

        long startTime = System.currentTimeMillis();

        resultTextArea.appendText("Scanning: " + host + "\n");

        // Arrays für Threads und Scanner erstellen
        threads = new Thread[numOfThreads];
        scanners = new ScannerApplication[numOfThreads]; // füllt das array mit so viel objekte der klasse scannerapplication as number of threads

        // ===== Threads starten =====
        for (int i = 0; i < threads.length; i++) {

            // Bereich berechnen
            int start = portStart + (i * portsPerThread);
            int end = start + portsPerThread;

            // Grenzen korrigieren
            if (end > portEnd) end = portEnd;
            if (start < portStart) start = portStart;

            // ScannerWorker erstellen: bekommt runningSupplier, damit Stop funktioniert
            scanners[i] = new ScannerApplication(host, start, end, timeout, progressDoneCount, () -> running); //verweist auf dem konstruktor von scanner application

            // Thread starten
            threads[i] = new Thread(scanners[i]);
            threads[i].start(); // startet ein thread aus dem array
        }

        // ===== Progress Thread ===== Das ist ein streaming und dann wird eine interne funktion angerufen
        new Thread(() -> { // Mit Lambda-Ausdruck new Thread(...) erstellt einen neuen Thread (Nebenläufigkeit).
            //() -> { ... } ist eine Lambda (Runnable-Code).
            while (running) {


                // Fortschritt als 0..1 Wert
                double progressDecimal = portsToScan == 0
                        ? 1.0
                        : (double) progressDoneCount.get() / portsToScan; //wie viele Ports schon “fertig” sind (thread-sicher, AtomicInteger)

                // UI Updates müssen im JavaFX Thread laufen; UI darf nur im JavaFX Application Thread geändert werden.
                Platform.runLater(() -> {
                    progressBar.setProgress(progressDecimal);
                    progress.setText(df.format(progressDecimal * 100) + "%");
                });
                //Der Thread wartet 50 ms → das sind ca. 20 Updates pro Sekunde (1000ms / 50ms = 20).
                //
                //Warum das wichtig ist:
                //
                //Ohne sleep würde die Schleife extrem schnell laufen und unnötig CPU verbrauchen.
                //
                //20 fps reicht für eine flüssige ProgressBar.
                try {
                    Thread.sleep(50); // ~20 Updates pro Sekunde
                } catch (InterruptedException e) {
                    break;
                }

            }
        }).start(); //.start() startet den Thread wirklich.
        //(Wichtig: start() ≠ run(). start() führt den Code parallel aus.)

        // ===== Ergebnis Sammeln / CSV schreiben =====
        new Thread(() -> {

            // results Ordner
            Path folderPath = Paths.get("results");

            // Ordner erstellen (falls nicht vorhanden)
            try {
                Files.createDirectories(folderPath);
            } catch (IOException e) {
                System.err.println("Failed to create directory: " + e.getMessage());
            }

            // Dateiname mit Timestamp
            String fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss'.csv'").format(new Date());
            Path filePath = folderPath.resolve(fileName);

            // Hier sammeln wir alle offenen Ports
            List<Integer> allOpenPorts = new ArrayList<>();

            // Auf alle Threads warten und Ports sammeln
            for (int i = 0; i < threads.length; i++) {
                try {
                    if (threads[i] != null) {
                        threads[i].join(); // wartet bis fertig
                        allOpenPorts.addAll(scanners[i].getOpenPorts());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            running = false;

            // Wenn abgebrochen: kein Output, nur UI reset
            if (cancelOutput) {
                Platform.runLater(() -> {
                    startBtn.setDisable(false);
                    stopBtn.setDisable(true);
                    startBtn.setText("Done, do again?");
                });
                return;
            }

            // Duplikate entfernen + sortieren
            allOpenPorts = allOpenPorts.stream().distinct().collect(Collectors.toList());
            Collections.sort(allOpenPorts);

            // Dauer berechnen
            long endTime = System.currentTimeMillis();
            double durationSeconds = (endTime - startTime) / 1000.0;

            // Ports als String (ohne [ ])
            String finalAllOpenPorts = allOpenPorts.toString()
                    .substring(1, allOpenPorts.toString().length() - 1);

            // CSV Inhalt erstellen
            String logFileOutput = "Host,Port,Status\n";

            if (allOpenPorts.isEmpty()) {
                logFileOutput += host + ", ,No open ports\n";
            } else {
                for (Integer port : allOpenPorts) {
                    logFileOutput += host + "," + port + ",Open\n";
                }
            }

            // CSV Datei schreiben
            try {
                Files.writeString(filePath, logFileOutput, StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // GUI Update nach Ende (JavaFX Thread!)
            Platform.runLater(() -> {
                resultTextArea.appendText(finalAllOpenPorts + "\n");
                progressBar.setProgress(1.0);
                progress.setText(df.format(100.0) + "%");
                startBtn.setDisable(false);
                stopBtn.setDisable(true);
                startBtn.setText("Done, do again?");
            });

        }).start();
    }

    /**
     * Stop Button: setzt running=false, damit Worker abbrechen.
     * cancelOutput=true verhindert, dass am Ende noch Ergebnisse ausgegeben werden.
     */
    @FXML
    protected void onStopBtnClick() {

        new Thread(() -> {
            cancelOutput = true;
            running = false;

            // auf alle Threads warten, damit alles sauber endet
            for (Thread thread : threads) {
                try {
                    if (thread != null) {
                        thread.join();
                        System.out.println(thread + " stopped");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // UI Meldung
        Platform.runLater(() -> resultTextArea.appendText("Scanning aborted! \n"));
    }
}
