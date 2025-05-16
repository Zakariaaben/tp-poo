package transport.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import transport.services.PersonneService;
import transport.services.ReclamationService;
import transport.services.TitreTransportService;

import java.io.IOException;
import java.net.URL;

public class MainWindowController {

    @FXML
    private BorderPane mainLayout;

    @FXML
    private Button personnesBtn;

    @FXML
    private Button titresBtn;

    @FXML
    private Button reclamationsBtn;

    private PersonneService personneService;
    private TitreTransportService titreTransportService;
    private ReclamationService reclamationService;

    @FXML
    public void initialize() {
        // Initialize services
        personneService = new PersonneService();
        titreTransportService = new TitreTransportService(personneService);
        reclamationService = new ReclamationService(personneService);

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
    public void showTitresView() {
        try {
            URL fxmlUrl = getClass().getResource("/ui/TitresTransportView.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Pane titresView = loader.load();

            // Initialize controller with services
            TitresTransportViewController controller = loader.getController();
            controller.initialize();

            mainLayout.setCenter(titresView);
        } catch (IOException e) {
            showError("Erreur lors du chargement de la vue Titres de transport", e);
        }
    }

    @FXML
    public void showReclamationsView() {
        try {
            URL fxmlUrl = getClass().getResource("/ui/ReclamationsView.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Pane reclamationsView = loader.load();

            // Initialize controller with services
            ReclamationsViewController controller = loader.getController();
            controller.initialize(reclamationService, personneService);

            mainLayout.setCenter(reclamationsView);
        } catch (IOException e) {
            showError("Erreur lors du chargement de la vue Réclamations", e);
        }
    }

    private void showError(String message, Exception e) {
        System.err.println(message);
        e.printStackTrace();

        // Create a simple error dialog or notification
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message + "\n\n" + e.getMessage());
        alert.showAndWait();
    }
}
