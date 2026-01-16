package at.ac.hcw.model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * ScannerApplication ist ein "Worker", der einen Portbereich scannt.
 *
 * - Implementiert Runnable, damit man ihn in einem Thread ausführen kann.
 * - Scannt Ports von portStart bis portEnd und sammelt offene Ports.
 * - Aktualisiert einen Fortschrittszähler (AtomicInteger).
 * - Kann über runningSupplier jederzeit "abgebrochen" werden.
 */
public class ScannerApplication implements Runnable {

    // ======= Felder / Instanzvariablen =======

    // Zielhost (IP oder Domain), z.B. "192.168.0.1" oder "google.com"
    private final String host;

    // Startport (inklusive)
    private final int portStart;

    // Endport (inklusive)
    private final int portEnd;

    // Timeout in Millisekunden: wie lange socket.connect(...) maximal warten soll
    private final int timeout;

    // Liste der offenen Ports, die gefunden wurden
    // (wird während des Scans befüllt)
    private final List<Integer> openPorts = new ArrayList<>();

    // Gemeinsamer Fortschrittszähler (thread-sicher), z.B. für ProgressBar
    // AtomicInteger wird verwendet, weil mehrere Threads gleichzeitig erhöhen können.
    private final AtomicInteger progressDoneCount;

    // BooleanSupplier liefert true/false, ob der Scan weiterlaufen soll.
    // Vorteil: ScannerApplication muss nicht wissen, WO die "running"-Variable liegt.
    private final BooleanSupplier runningSupplier;

    /**
     * Konstruktor: initialisiert alle Parameter für den Scan.
     *
     * @param host Zielhost
     * @param portStart Startport (inkl.)
     * @param portEnd Endport (inkl.)
     * @param timeout Timeout in ms
     * @param progressDoneCount gemeinsamer Zähler für erledigte Ports
     * @param runningSupplier liefert, ob weiter gescannt werden soll (Abbruch möglich)
     */
    public ScannerApplication(
            String host,
            int portStart,
            int portEnd,
            int timeout,
            AtomicInteger progressDoneCount,
            BooleanSupplier runningSupplier
    ) {
        this.host = host;
        this.portStart = portStart;
        this.portEnd = portEnd;
        this.timeout = timeout;
        this.progressDoneCount = progressDoneCount;
        this.runningSupplier = runningSupplier;
    }

    /**
     * run() ist die "Hauptfunktion" von Runnable.
     * Sie wird aufgerufen, wenn du z.B. new Thread(scanner).start() machst.
     */
    @Override
    public void run() {

        // Schleife über den gesamten Portbereich
        for (int port = portStart; port <= portEnd; port++) {

            // Fortschritt erhöhen: "ein Port wurde bearbeitet"
            // Hinweis: Bei einem Abbruch (break) kann es sein,
            // dass du schon gezählt hast, bevor du abbrichst.
            progressDoneCount.incrementAndGet();

            // Prüfen, ob dieser Port offen ist (connect klappt)
            if (pingHost(host, port, timeout)) {
                openPorts.add(port); // Port merken
            }

            // Abbruchbedingung:
            // Wenn runningSupplier false liefert, soll dieser Scanner stoppen.
            // Das ist wichtig, wenn der User auf "Stop" klickt.
            if (!runningSupplier.getAsBoolean()) {
                break;
            }
        }
    }

    /**
     * Getter, damit die GUI/andere Teile die gefundenen offenen Ports abholen können.
     * (Nach dem Scan oder währenddessen)
     */
    public List<Integer> getOpenPorts() {
        return openPorts;
    }

    /**
     * pingHost versucht, eine TCP-Verbindung zu host:port aufzubauen.
     * Wenn connect() erfolgreich ist -> Port ist offen (oder zumindest erreichbar).
     *
     * @param host Zielhost
     * @param port Zielport
     * @param timeout Timeout in ms
     * @return true wenn Verbindung aufgebaut werden konnte, sonst false
     */
    public static boolean pingHost(String host, int port, int timeout) {

        // try-with-resources:
        // Der Socket wird am Ende automatisch geschlossen (auch bei Exception).
        try (Socket socket = new Socket()) {

            // Adresse: (host, port)
            InetSocketAddress address = new InetSocketAddress(host, port);

            // connect versucht, die TCP-Verbindung herzustellen.
            // - Wenn der Port geschlossen ist: oft "Connection refused" (IOException)
            // - Wenn keine Antwort rechtzeitig kommt: SocketTimeoutException
            socket.connect(address, timeout);

            // Wenn wir hier ankommen, war connect erfolgreich -> Port vermutlich offen
            return true;

        } catch (SocketTimeoutException e) {
            // Timeout: Ziel hat nicht rechtzeitig geantwortet
            // -> für Portscan meistens als "nicht offen / nicht erreichbar" behandeln
            return false;

        } catch (IOException e) {
            // Alle anderen I/O-Probleme (z.B. Connection refused, Host unreachable, etc.)
            // -> ebenfalls als "nicht offen" behandeln
            return false;
        }
    }
}
