package transport.ui.controllers;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public class MainWindowController {

    @FXML
    private BorderPane mainLayout;

    @FXML
    private Button personnesBtn;

    @FXML
    private Button titresTransportBtn;

    @FXML
    public void initialize() {
        // Load PersonnesView by default
        showPersonnesView();
    }

    @FXML
    public void showPersonnesView() {
        try {
            URL fxmlUrl = getClass().getResource("/ui/PersonnesView.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Pane personnesView = loader.load();
            mainLayout.setCenter(personnesView);
        } catch (IOException e) {
            showError("Erreur lors du chargement de la vue Personnes", e);
        }
    }

    @FXML
    public void showTitresTransportView() {
        try {
            URL fxmlUrl = getClass().getResource("/ui/TitresTransportView.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Pane titresTransportView = loader.load();
            mainLayout.setCenter(titresTransportView);
        } catch (IOException e) {
            showError("Erreur lors du chargement de la vue Titres de Transport", e);
        }
    }

    private void showError(String message, Exception e) {
        System.err.println(message);
        if (e != null) {
            e.printStackTrace();
        }

        try {
            // Get the main application window as owner
            javafx.stage.Window owner = null;
            if (mainLayout != null && mainLayout.getScene() != null) {
                owner = mainLayout.getScene().getWindow();
            }

            // Create a simple error dialog or notification
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Une erreur est survenue");
            alert.setContentText(message + "\n\n" + (e != null ? e.getMessage() : ""));

            // Ensure the alert is a modal dialog with proper owner
            if (owner != null) {
                alert.initOwner(owner);
            }

            // Make dialog resizable to better display long error messages
            alert.setResizable(true);

            System.out.println("Showing error dialog for: " + message);
            alert.showAndWait();
        } catch (Exception dialogEx) {
            // If we can't even show the dialog, log to console
            System.err.println("Failed to show error dialog: " + dialogEx.getMessage());
            dialogEx.printStackTrace();
        }
    }
}
