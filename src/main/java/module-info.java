/**
 * module-info.java (Java Module System / JPMS)
 *
 * Dieses Modul beschreibt:
 * - welche Bibliotheken (Module) dein Projekt braucht (requires)
 * - welche Packages "von außen" sichtbar sind (exports)
 * - welche Packages für Reflection geöffnet werden (opens), z.B. für FXML
 */
module com.example.scanner {

    // ===== JavaFX Module, die du verwendest =====

    // JavaFX Controls: Buttons, TextField, TextArea, ProgressBar etc.
    requires javafx.controls;

    // JavaFX FXML: Laden von .fxml Dateien mit FXMLLoader
    requires javafx.fxml;

    // JavaFX Web: WebView-Komponente (Browser im Programm)
    // -> Nur nötig, wenn du WebView irgendwo nutzt.
    requires javafx.web;

    // JavaFX Swing Interop: Brücke zwischen Swing und JavaFX
    // -> Nur nötig, wenn du Swing-Komponenten einbindest.
    requires javafx.swing;

    // ===== Externe Library =====

    // ControlsFX: zusätzliche UI-Komponenten für JavaFX (z.B. Notifications, extra Controls)
    requires org.controlsfx.controls;

    // ===== Reflection Zugriff für FXML =====
    // FXML lädt Controller per Reflection (zur Laufzeit).
    // Ohne "opens" kann JavaFX nicht auf private Felder/Methoden (z.B. @FXML) zugreifen.
    opens at.ac.hcw to javafx.fxml;

    // ===== Was ist von außen sichtbar? =====
    // "exports" bedeutet: andere Module dürfen dieses Package verwenden/importieren.
    // Üblich: das Package, in dem deine Application-Klasse liegt.
    exports at.ac.hcw;
}
