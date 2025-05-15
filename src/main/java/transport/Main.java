package transport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

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
            
            try {
                // Display error dialog
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR
                );
                alert.setTitle("Erreur");
                alert.setHeaderText("Erreur de démarrage");
                alert.setContentText("Une erreur est survenue lors du démarrage de l'application:\n\n" + e.getMessage());
                
                // Make sure dialog is visible and properly sized
                alert.setResizable(true);
                alert.getDialogPane().setPrefSize(550, 270);
                
                // Add exception details in expandable content area
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                
                TextArea textArea = new TextArea(sw.toString());
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                
                alert.getDialogPane().setExpandableContent(new javafx.scene.layout.GridPane());
                alert.getDialogPane().setExpanded(true);
                
                System.out.println("Showing application error dialog");
                alert.showAndWait();
            } catch (Exception dialogEx) {
                System.err.println("Failed to show error dialog: " + dialogEx.getMessage());
                dialogEx.printStackTrace();
            }
            
            throw e;
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
