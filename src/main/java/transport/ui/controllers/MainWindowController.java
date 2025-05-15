package transport.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.net.URL;

public class MainWindowController {
    @FXML
    private BorderPane mainLayout;
    
    @FXML
    private Button personnesBtn;
    
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
    
    private void showError(String message, Exception e) {
        System.err.println(message);
        e.printStackTrace();
        
        // Create a simple error dialog or notification
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR,
                message + "\n\n" + e.getMessage()
        );
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur est survenue");
        alert.showAndWait();
    }
}
