package transport.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import transport.core.CartePersonnelle;
import transport.core.Personne;
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
    private TableColumn<TitreTransport, TitreTransport> actionCol;

    private ObservableList<TitreTransport> titreList;
    private TitreTransportService titreService;
    private PersonneService personneService;

    @FXML
    public void initialize() {
        try {
            personneService = new PersonneService();
            titreService = new TitreTransportService(personneService);
            titreList = FXCollections.observableArrayList(titreService.getAllTitres());
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
                return Bindings.createStringBinding(() -> "Carte");
            }
            return Bindings.createStringBinding(() -> "Inconnu");
        });

        personneCol.setCellValueFactory(data -> {
            // Get person by ID
            Personne personne = personneService.getPersonneById(data.getValue().getPersonneId());
            return Bindings.createStringBinding(()
                    -> personne != null ? personne.getName() + " " + personne.getFamilyName() : "Inconnu");
        });

        dateCol.setCellValueFactory(data -> Bindings.createStringBinding(() -> {
            LocalDate date = data.getValue().getDateAchat().toLocalDate();
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }));

        prixCol.setCellValueFactory(data -> Bindings.createStringBinding(()
                -> data.getValue().getPrix() + " DA"));

        validiteCol.setCellValueFactory(data -> {
            boolean valid = data.getValue().estValide(LocalDate.now());
            return Bindings.createStringBinding(() -> valid ? "Valide" : "Non valide");
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

        actionCol.setCellFactory(col -> new TableCell<TitreTransport, TitreTransport>() {
            private final Button useButton = new Button("Utiliser");
            private final Button deleteButton = new Button("Supprimer");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, useButton, deleteButton);

            {
                useButton.setOnAction(event -> {
                    TitreTransport titre = getTableView().getItems().get(getIndex());
                    if (titre instanceof Ticket) {
                        try {
                            boolean success = titreService.useTicket(titre);
                            if (success) {
                                refreshTable();
                                showInfo("Ticket utilisé avec succès");
                            } else {
                                showError("Utilisation impossible",
                                        new Exception("Ce ticket n'est pas valide ou a déjà été utilisé"));
                            }
                        } catch (Exception e) {
                            showError("Erreur lors de l'utilisation du ticket", e);
                        }
                    } else {
                        showInfo("Les cartes ne peuvent pas être 'utilisées', elles sont toujours valides");
                    }
                });

                deleteButton.setOnAction(event -> {
                    try {
                        TitreTransport titre = getTableView().getItems().get(getIndex());

                        // Confirmation dialog
                        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmation.setTitle("Confirmation de suppression");
                        confirmation.setHeaderText("Êtes-vous sûr de vouloir supprimer ce titre de transport ?");
                        confirmation.setContentText("Cette action est irréversible.");

                        Optional<ButtonType> result = confirmation.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            titreService.deleteTitre(titre.getCurrentId());
                            refreshTable();
                        }
                    } catch (Exception e) {
                        showError("Erreur lors de la suppression", e);
                    }
                });
            }

            @Override
            protected void updateItem(TitreTransport item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    // For tickets that are already used, disable the use button
                    if (item instanceof Ticket) {
                        Ticket ticket = (Ticket) item;
                        useButton.setDisable(ticket.isUsed() || !ticket.isValid());
                    } else {
                        useButton.setDisable(true);
                    }
                    setGraphic(box);
                }
            }
        });
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
        titreList.clear();
        titreList.addAll(titreService.getAllTitres());
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
            javafx.stage.Window owner = null;
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
