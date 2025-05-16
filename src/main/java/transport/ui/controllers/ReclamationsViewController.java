package transport.ui.controllers;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import transport.core.*;
import transport.services.PersonneService;
import transport.services.ReclamationService;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReclamationsViewController {

    @FXML
    private BorderPane view;

    @FXML
    private TableView<Reclamation> reclamationTable;

    @FXML
    private TableColumn<Reclamation, String> typeCol;

    @FXML
    private TableColumn<Reclamation, String> descriptionCol;

    @FXML
    private TableColumn<Reclamation, String> personneCol;

    @FXML
    private TableColumn<Reclamation, String> dateCol;

    @FXML
    private TableColumn<Reclamation, String> statusCol;

    @FXML
    private TableColumn<Reclamation, String> actionCol;

    @FXML
    private ComboBox<String> statusFilterComboBox;

    @FXML
    private ComboBox<String> typeFilterComboBox;

    @FXML
    private Button addButton;

    @FXML
    private Button refreshButton;

    private ObservableList<Reclamation> reclamationList;
    private FilteredList<Reclamation> filteredList;

    private ReclamationService reclamationService;
    private PersonneService personneService;

    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void initialize(ReclamationService reclamationService, PersonneService personneService) {
        this.reclamationService = reclamationService;
        this.personneService = personneService;

        // Initialize filter combo boxes
        statusFilterComboBox.getItems().add("Tous les statuts");
        statusFilterComboBox.getItems().addAll(Arrays.stream(ReclamationStatus.values())
                .map(ReclamationStatus::getStatus)
                .collect(Collectors.toList()));
        statusFilterComboBox.setValue("Tous les statuts");

        typeFilterComboBox.getItems().add("Tous les types");
        typeFilterComboBox.getItems().addAll(Arrays.stream(ReclamationType.values())
                .map(ReclamationType::getLibelle)
                .collect(Collectors.toList()));
        typeFilterComboBox.setValue("Tous les types");

        // Initialize the table
        reclamationList = FXCollections.observableArrayList(reclamationService.getAllReclamations());
        filteredList = new FilteredList<>(reclamationList, p -> true);
        reclamationTable.setItems(filteredList);

        setupTableColumns();
        setupFilters();
    }

    private void setupTableColumns() {
        typeCol.setCellValueFactory(data
                -> new SimpleStringProperty(data.getValue().getType().getLibelle()));

        descriptionCol.setCellValueFactory(data
                -> new SimpleStringProperty(data.getValue().getDescription()));

        personneCol.setCellValueFactory(data -> {
            Personne personne = reclamationService.getPersonneForReclamation(data.getValue());
            return new SimpleStringProperty(personne != null
                    ? personne.getName() + " " + personne.getFamilyName() : "Inconnu");
        });

        dateCol.setCellValueFactory(data
                -> new SimpleStringProperty(data.getValue().getDateReclamation().format(DATE_FORMATTER)));

        statusCol.setCellValueFactory(data
                -> new SimpleStringProperty(data.getValue().getEtat().getStatus()));

        // Style the status column based on the status
        statusCol.setCellFactory(column -> new TableCell<Reclamation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Reclamation reclamation = getTableView().getItems().get(getIndex());
                    if (reclamation != null) {
                        switch (reclamation.getEtat()) {
                            case EN_COURS:
                                setStyle("-fx-text-fill: #ff8c00;"); // Orange
                                break;
                            case TRAITE:
                                setStyle("-fx-text-fill: #008000;"); // Green
                                break;
                            case REFUSE:
                                setStyle("-fx-text-fill: #ff0000;"); // Red
                                break;
                            case ANNULE:
                                setStyle("-fx-text-fill: #808080;"); // Gray
                                break;
                            default:
                                setStyle("");
                                break;
                        }
                    }
                }
            }
        });

        actionCol.setCellFactory(col -> new TableCell<Reclamation, String>() {
            private final Button actionButton = new Button("Traiter");

            {
                actionButton.setOnAction(event -> {
                    Reclamation reclamation = getTableView().getItems().get(getIndex());
                    showReclamationResponseDialog(reclamation);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    Reclamation reclamation = getTableView().getItems().get(getIndex());

                    // Only show action button for pending reclamations
                    if (reclamation.isEnCours()) {
                        setGraphic(actionButton);
                    } else {
                        Button viewButton = new Button("Voir");
                        viewButton.setOnAction(event -> showReclamationResponseDialog(reclamation));
                        setGraphic(viewButton);
                    }
                }
            }
        });
    }

    private void setupFilters() {
        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        typeFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyFilters() {
        String statusFilter = statusFilterComboBox.getValue();
        String typeFilter = typeFilterComboBox.getValue();

        filteredList.setPredicate(reclamation -> {
            boolean statusMatch = "Tous les statuts".equals(statusFilter)
                    || reclamation.getEtat().getStatus().equals(statusFilter);

            boolean typeMatch = "Tous les types".equals(typeFilter)
                    || reclamation.getType().getLibelle().equals(typeFilter);

            return statusMatch && typeMatch;
        });
    }

    @FXML
    public void showAddReclamationDialog() {
        try {
            URL fxmlUrl = getClass().getResource("/ui/ReclamationFormDialog.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            VBox dialogContent = loader.load();

            Dialog<Reclamation> dialog = new Dialog<>();
            dialog.setTitle("Nouvelle réclamation");
            dialog.setHeaderText("Saisir une nouvelle réclamation");

            // Set the button types
            ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Get the controller and set the dialog with services
            ReclamationFormDialogController controller = loader.getController();
            controller.initialize(dialog, reclamationService, personneService);

            // Set the content
            dialog.getDialogPane().setContent(dialogContent);

            // Make the dialog resizable
            dialog.getDialogPane().setPrefSize(500, 400);

            // Set modality
            dialog.initModality(Modality.APPLICATION_MODAL);

            // Show dialog and handle result
            Optional<Reclamation> result = dialog.showAndWait();
            result.ifPresent(reclamation -> {
                reclamationService.saveReclamation(reclamation);
                refreshReclamations();
            });
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture du formulaire", e);
        }
    }

    private void showReclamationResponseDialog(Reclamation reclamation) {
        try {
            URL fxmlUrl = getClass().getResource("/ui/ReclamationResponseDialog.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            VBox dialogContent = loader.load();

            Dialog<Reclamation> dialog = new Dialog<>();
            dialog.setTitle("Traitement de la réclamation");
            dialog.setHeaderText("Traiter la réclamation");

            // Set the button types
            ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Get the controller and set the dialog with services
            ReclamationResponseDialogController controller = loader.getController();
            controller.initialize(dialog, reclamation, reclamationService, personneService);

            // Set the content
            dialog.getDialogPane().setContent(dialogContent);

            // Make the dialog resizable
            dialog.getDialogPane().setPrefSize(500, 500);

            // Set modality
            dialog.initModality(Modality.APPLICATION_MODAL);

            // Show dialog and handle result
            Optional<Reclamation> result = dialog.showAndWait();
            result.ifPresent(updatedReclamation -> {
                refreshReclamations();
            });
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture du dialogue de traitement", e);
        }
    }

    @FXML
    public void refreshReclamations() {
        reclamationList.clear();
        reclamationList.addAll(reclamationService.getAllReclamations());
        applyFilters();
    }

    private void showError(String message, Exception e) {
        System.err.println(message);
        e.printStackTrace();

        // Create a simple error dialog
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    public BorderPane getView() {
        return view;
    }
}
