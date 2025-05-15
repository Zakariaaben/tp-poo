package transport.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import transport.core.Employe;
import transport.core.Personne;
import transport.core.Usager;
import transport.services.PersonneService;

public class PersonnesViewController {

    @FXML
    private BorderPane view;

    @FXML
    private TableView<Personne> personneTable;

    @FXML
    private TableColumn<Personne, String> typeCol;

    @FXML
    private TableColumn<Personne, String> nameCol;

    @FXML
    private TableColumn<Personne, String> familyNameCol;

    @FXML
    private TableColumn<Personne, String> birthDateCol;

    @FXML
    private TableColumn<Personne, String> handicapCol;

    @FXML
    private TableColumn<Personne, String> matriculeCol;

    @FXML
    private TableColumn<Personne, String> actionCol;

    private ObservableList<Personne> personneList;

    private PersonneService personneService;

    @FXML
    public void initialize() {
        try {
            personneService = new PersonneService();
            personneList = FXCollections.observableArrayList(personneService.getAllPersonnes());
            
            // Sort the list by birth date in descending order (youngest first)
            personneList.sort((p1, p2) -> p2.getBirthDate().compareTo(p1.getBirthDate()));
            
            personneTable.setItems(personneList);
            setupTableColumns();
        } catch (Exception e) {
            showError("Erreur lors de l'initialisation de la vue", e);
        }
    }

    private void setupTableColumns() {
        typeCol.setCellValueFactory(data -> {
            if (data.getValue() instanceof Employe) {
                return Bindings.createStringBinding(() -> "Employé");
            } else if (data.getValue() instanceof Usager) {
                return Bindings.createStringBinding(() -> "Usager");
            }
            return Bindings.createStringBinding(() -> "Autre");
        });

        nameCol.setCellValueFactory(data -> Bindings.createStringBinding(
                () -> data.getValue().getName()));

        familyNameCol.setCellValueFactory(data -> Bindings.createStringBinding(
                () -> data.getValue().getFamilyName()));

        birthDateCol.setCellValueFactory(data -> Bindings.createStringBinding(
                () -> data.getValue().getBirthDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        handicapCol.setCellValueFactory(data -> Bindings.createStringBinding(
                () -> data.getValue().hasHandicap() ? "Oui" : "Non"));

        matriculeCol.setCellValueFactory(data -> {
            if (data.getValue() instanceof Employe) {
                return Bindings.createStringBinding(
                        () -> ((Employe) data.getValue()).getMatricule());
            }
            return Bindings.createStringBinding(() -> "");
        });

        actionCol.setCellFactory(col -> new TableCell<Personne, String>() {
            private final Button deleteButton = new Button("Supprimer");

            {
                deleteButton.setOnAction(event -> {
                    try {
                        Personne personne = getTableView().getItems().get(getIndex());

                        // Confirmation dialog
                        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmation.setTitle("Confirmation de suppression");
                        confirmation.setHeaderText("Êtes-vous sûr de vouloir supprimer cette personne ?");
                        confirmation.setContentText("Cette action est irréversible.");

                        Optional<ButtonType> result = confirmation.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            personneService.deletePersonne(personne.getId());
                            refreshTable();
                        }
                    } catch (Exception e) {
                        showError("Erreur lors de la suppression", e);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                }
            }
        });
    }

    @FXML
    public void showAddPersonneDialog() {
        try {
            URL fxmlUrl = getClass().getResource("/ui/PersonneFormDialog.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            VBox dialogContent = loader.load();

            Dialog<Personne> dialog = new Dialog<>();
            dialog.setTitle("Ajouter une personne");
            dialog.setHeaderText("Entrez les informations de la personne");

            // Set the button types
            ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Get the controller and set the dialog
            PersonneFormDialogController controller = loader.getController();
            controller.setDialog(dialog);

            // Set the content
            dialog.getDialogPane().setContent(dialogContent);

            // Make the dialog resizable
            dialog.getDialogPane().setPrefSize(500, 500);

            // Set modality
            dialog.initModality(Modality.APPLICATION_MODAL);

            // Show dialog and handle result
            Optional<Personne> result = dialog.showAndWait();
            result.ifPresent(personne -> {
                try {
                    // Verify that the Personne object is valid before saving
                    if (personne.getName() == null || personne.getName().trim().isEmpty()) {
                        showError("Validation Error", new Exception("Le nom ne peut pas être vide"));
                        return;
                    }

                    if (personne.getFamilyName() == null || personne.getFamilyName().trim().isEmpty()) {
                        showError("Validation Error", new Exception("Le prénom ne peut pas être vide"));
                        return;
                    }

                    if (personne.getBirthDate() == null) {
                        showError("Validation Error", new Exception("La date de naissance ne peut pas être vide"));
                        return;
                    }

                    // Additional validation for Employe
                    if (personne instanceof Employe) {
                        Employe employe = (Employe) personne;
                        if (employe.getMatricule() == null || employe.getMatricule().trim().isEmpty()) {
                            showError("Validation Error", new Exception("Le matricule ne peut pas être vide"));
                            return;
                        }

                        if (employe.getFonction() == null) {
                            showError("Validation Error", new Exception("La fonction ne peut pas être vide"));
                            return;
                        }
                    }

                    // Save the person if validation passes
                    personneService.savePersonne(personne);
                    refreshTable();
                } catch (Exception e) {
                    showError("Erreur lors de l'enregistrement", e);
                }
            });
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture du formulaire", e);
        }
    }

    private void refreshTable() {
        try {
            personneList.clear();
            List<Personne> allPersonnes = personneService.getAllPersonnes();
            
            // Sort by birth date in descending order before adding to table
            allPersonnes.sort((p1, p2) -> p2.getBirthDate().compareTo(p1.getBirthDate()));
            
            personneList.addAll(allPersonnes);
        } catch (Exception e) {
            showError("Erreur lors du rafraîchissement des données", e);
        }
    }

    private void showError(String message, Exception e) {
        System.err.println(message);
        if (e != null) {
            e.printStackTrace();
        }

        try {
            // Get the current window for proper ownership
            javafx.stage.Window owner = null;
            if (view != null && view.getScene() != null) {
                owner = view.getScene().getWindow();
            }

            // Create a simple error dialog
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(message);
            alert.setContentText(e != null ? e.getMessage() : "Une erreur inconnue s'est produite");

            // Set the owner to ensure proper modal behavior
            if (owner != null) {
                alert.initOwner(owner);
            }

            // Make sure alert appears on top and is properly sized
            alert.setResizable(true);

            System.out.println("Showing error dialog: " + message);
            alert.showAndWait();
        } catch (Exception dialogEx) {
            System.err.println("Failed to show error dialog: " + dialogEx.getMessage());
            dialogEx.printStackTrace();
        }
    }
}
