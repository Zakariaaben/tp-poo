package transport.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
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
import javafx.stage.Window;
import transport.core.CartePersonnelle;
import transport.core.Ticket;
import transport.core.TitreTransport;
import transport.services.PersonneService;
import transport.services.TitreTransportService;

public class TitresTransportViewController {

    @FXML
    private BorderPane view;

    @FXML
    private TableView<TitreTransport> titreTable;

    @FXML
    private TableColumn<TitreTransport, String> typeCol;

    @FXML
    private TableColumn<TitreTransport, String> personneCol;

    @FXML
    private TableColumn<TitreTransport, String> dateCol;

    @FXML
    private TableColumn<TitreTransport, String> prixCol;

    @FXML
    private TableColumn<TitreTransport, String> validiteCol;

    @FXML
    private TableColumn<TitreTransport, String> detailsCol;

    @FXML
    private TableColumn<TitreTransport, String> actionCol;

    private ObservableList<TitreTransport> titreList;
    private TitreTransportService titreService;
    private PersonneService personneService;

    @FXML
    public void initialize() {
        try {
            personneService = new PersonneService();
            titreService = new TitreTransportService(personneService);
            titreList = FXCollections.observableArrayList(titreService.getAllTitres());

            // Sort the list by date in descending order (most recent first)
            titreList.sort((t1, t2) -> t2.getDateAchat().compareTo(t1.getDateAchat()));

            titreTable.setItems(titreList);
            setupTableColumns();
        } catch (Exception e) {
            showError("Erreur lors de l'initialisation de la vue", e);
        }
    }

    private void setupTableColumns() {
        typeCol.setCellValueFactory(data -> {
            if (data.getValue() instanceof Ticket) {
                return Bindings.createStringBinding(() -> "Ticket");
            } else if (data.getValue() instanceof CartePersonnelle) {
                return Bindings.createStringBinding(() -> "Carte Personnelle");
            }
            return Bindings.createStringBinding(() -> "Inconnu");
        });

        personneCol.setCellValueFactory(data -> {
            try {
                return Bindings.createStringBinding(() -> {
                    var personne = personneService.getPersonneById(data.getValue().getPersonneId());
                    return personne != null ? personne.getName() + " " + personne.getFamilyName() : "N/A";
                });
            } catch (Exception e) {
                return Bindings.createStringBinding(() -> "Erreur");
            }
        });

        dateCol.setCellValueFactory(data -> Bindings.createStringBinding(() -> {
            LocalDate date = data.getValue().getDateAchat().toLocalDate();
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }));

        prixCol.setCellValueFactory(data -> Bindings.createStringBinding(() -> data.getValue().getPrix() + " €"));

        validiteCol.setCellValueFactory(data -> {
            if (data.getValue() instanceof Ticket) {
                Ticket ticket = (Ticket) data.getValue();
                return Bindings.createStringBinding(()
                        -> ticket.isUsed() ? "Utilisé" : (ticket.isValid() ? "Valide" : "Expiré"));
            } else if (data.getValue() instanceof CartePersonnelle) {
                CartePersonnelle carte = (CartePersonnelle) data.getValue();
                return Bindings.createStringBinding(()
                        -> carte.isValid() ? "Valide" : "Invalide");
            }
            return Bindings.createStringBinding(() -> "Inconnu");
        });

        detailsCol.setCellValueFactory(data -> {
            TitreTransport titre = data.getValue();
            if (titre instanceof CartePersonnelle) {
                CartePersonnelle carte = (CartePersonnelle) titre;
                return Bindings.createStringBinding(() -> carte.getType().getLibelle());
            } else if (titre instanceof Ticket) {
                Ticket ticket = (Ticket) titre;
                return Bindings.createStringBinding(() -> ticket.isUsed() ? "Utilisé" : "Non utilisé");
            }
            return Bindings.createStringBinding(() -> "");
        });

        actionCol.setCellFactory(col -> new TableCell<TitreTransport, String>() {
            private final Button useButton = new Button("Utiliser");

            {
                useButton.setOnAction(event -> {
                    TitreTransport titre = getTableView().getItems().get(getIndex());
                    handleUseTicket(titre);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(useButton);
                }
            }
        });
    }

    private void handleUseTicket(TitreTransport titre) {
        try {
            if (titre.isValid()) {
                if (titre instanceof Ticket) {
                    Ticket ticket = (Ticket) titre;
                    titreService.useTicket(ticket);

                    // Show success dialog
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Titre utilisé");
                    success.setHeaderText("Titre utilisé avec succès");
                    success.setContentText("Le ticket a bien été validé.");
                    success.showAndWait();

                    // Refresh the table to show updated state
                    refreshTable();
                } else {
                    // Show generic usage success for other titre types
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Titre utilisé");
                    success.setHeaderText("Titre utilisé avec succès");
                    success.setContentText("Le titre de transport a bien été utilisé.");
                    success.showAndWait();
                }
            } else {
                // Show invalid ticket dialog
                Alert invalidAlert = new Alert(Alert.AlertType.ERROR);
                invalidAlert.setTitle("Titre invalide");
                invalidAlert.setHeaderText("Ce titre n'est pas valide");

                if (titre instanceof Ticket) {
                    invalidAlert.setContentText("Ce ticket n'est plus valide, il a déjà été utilisé ou est expiré.");
                } else if (titre instanceof CartePersonnelle) {
                    invalidAlert.setContentText("Cette carte n'est plus valide.");
                } else {
                    invalidAlert.setContentText("Ce titre de transport n'est plus valide.");
                }

                invalidAlert.showAndWait();
            }
        } catch (Exception e) {
            showError("Erreur lors de l'utilisation du titre", e);
        }
    }

    @FXML
    public void showAddTitreDialog() {
        try {
            URL fxmlUrl = getClass().getResource("/ui/TitreTransportFormDialog.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            VBox dialogContent = loader.load();

            Dialog<TitreTransport> dialog = new Dialog<>();
            dialog.setTitle("Ajouter un titre de transport");
            dialog.setHeaderText("Créer un nouveau titre de transport");

            // Set the button types
            ButtonType saveButtonType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Get the controller and set dialog and services
            TitreTransportFormDialogController controller = loader.getController();
            controller.setDialog(dialog);
            controller.setServices(personneService, titreService);

            // Set the content
            dialog.getDialogPane().setContent(dialogContent);
            dialog.getDialogPane().setPrefSize(500, 400);
            dialog.initModality(Modality.APPLICATION_MODAL);

            // Show dialog and handle result
            Optional<TitreTransport> result = dialog.showAndWait();
            result.ifPresent(titre -> {
                refreshTable();
            });
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture du formulaire", e);
        }
    }

    private void refreshTable() {
        try {
            titreList.clear();
            List<TitreTransport> allTitres = titreService.getAllTitres();

            // Sort by date in descending order before adding to table
            allTitres.sort((t1, t2) -> t2.getDateAchat().compareTo(t1.getDateAchat()));

            titreList.addAll(allTitres);
        } catch (Exception e) {
            showError("Erreur lors du rafraîchissement des données", e);
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message, Exception e) {
        System.err.println(message);
        if (e != null) {
            e.printStackTrace();
        }

        try {
            // Get the current window for proper ownership
            Window owner = null;
            if (view != null && view.getScene() != null) {
                owner = view.getScene().getWindow();
            }

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(message);
            alert.setContentText(e != null ? e.getMessage() : "Une erreur inconnue s'est produite");

            // Set the owner to ensure proper modal behavior
            if (owner != null) {
                alert.initOwner(owner);
            }

            // Make sure alert is displayed above all other windows
            alert.setResizable(true);

            System.out.println("Showing error dialog: " + message);
            alert.showAndWait();
        } catch (Exception dialogException) {
            // If the alert itself fails, log the error
            System.err.println("Failed to display error dialog: " + dialogException.getMessage());
            dialogException.printStackTrace();
        }
    }

    private void showError(String message) {
        showError(message, null);
    }
}
