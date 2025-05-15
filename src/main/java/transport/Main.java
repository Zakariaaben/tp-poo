package transport;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            URL fxmlUrl = getClass().getResource("/ui/MainWindow.fxml");
            Parent root = FXMLLoader.load(fxmlUrl);
            
            Scene scene = new Scene(root, 1000, 600);
            primaryStage.setTitle("Transport Management System");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
            
            // Display error dialog
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "Une erreur est survenue lors du démarrage de l'application:\n\n" + e.getMessage()
            );
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur de démarrage");
            alert.showAndWait();
            
            throw e;
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
