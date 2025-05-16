package transport.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import transport.core.Personne;
import transport.core.Reclamation;
import transport.core.ReclamationStatus;
import transport.services.PersonneService;
import transport.services.ReclamationService;

import java.time.format.DateTimeFormatter;

public class ReclamationResponseDialogController {

    @FXML
    private Label personneLabel;

    @FXML
    private Label typeLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private TextArea descriptionTextArea;

    @FXML
    private ComboBox<ReclamationStatus> actionComboBox;

    @FXML
    private TextArea responseTextArea;

    @FXML
    private Label errorLabel;

    private Dialog<Reclamation> dialog;
    private Reclamation reclamation;
    private ReclamationService reclamationService;
    private PersonneService personneService;

    public void initialize(Dialog<Reclamation> dialog, Reclamation reclamation,
            ReclamationService reclamationService, PersonneService personneService) {
        this.dialog = dialog;
        this.reclamation = reclamation;
        this.reclamationService = reclamationService;
        this.personneService = personneService;

        fillReclamationDetails();
        setupControls();
        setupDialogResultConverter();
    }

    private void fillReclamationDetails() {
        // Get the person who made the reclamation
        Personne personne = personneService.getPersonneById(reclamation.getPersonneId());
        personneLabel.setText(personne != null
                ? personne.getName() + " " + personne.getFamilyName() : "Inconnu");

        // Set other fields
        typeLabel.setText(reclamation.getType().getLibelle());
        dateLabel.setText(reclamation.getDateReclamation()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        descriptionTextArea.setText(reclamation.getDescription());

        // Set response if already processed
        if (!reclamation.isEnCours()) {
            responseTextArea.setText(reclamation.getReponse());
            actionComboBox.setValue(reclamation.getEtat());
        }
    }

    private void setupControls() {
        // If the reclamation is already processed, disable editing
        boolean isReadOnly = !reclamation.isEnCours();
        actionComboBox.setDisable(isReadOnly);
        responseTextArea.setEditable(!isReadOnly);

        // Setup actions combo box
        actionComboBox.getItems().addAll(
                ReclamationStatus.TRAITE,
                ReclamationStatus.REFUSE,
                ReclamationStatus.ANNULE
        );

        // Setup action display
        actionComboBox.setConverter(new StringConverter<ReclamationStatus>() {
            @Override
            public String toString(ReclamationStatus status) {
                if (status == null) {
                    return "";
                }
                return status.getStatus();
            }

            @Override
            public ReclamationStatus fromString(String string) {
                return null; // Not needed
            }
        });

        // Clear error on field changes
        actionComboBox.valueProperty().addListener((obs, oldVal, newVal) -> clearError());
        responseTextArea.textProperty().addListener((obs, oldVal, newVal) -> clearError());
    }

    private void setupDialogResultConverter() {
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                if (reclamation.isEnCours() && !validateForm()) {
                    return null; // Validation failed, don't close dialog
                }

                // If reclamation is not already processed, update it based on selected action
                if (reclamation.isEnCours()) {
                    ReclamationStatus selectedStatus = actionComboBox.getValue();
                    String response = responseTextArea.getText().trim();

                    reclamationService.processReclamation(reclamation, selectedStatus, response);
                }

                return reclamation;
            }
            return null;
        });
    }

    private boolean validateForm() {
        if (actionComboBox.getValue() == null) {
            showError("Veuillez sélectionner une action");
            return false;
        }

        ReclamationStatus selectedStatus = actionComboBox.getValue();
        if ((selectedStatus == ReclamationStatus.TRAITE || selectedStatus == ReclamationStatus.REFUSE)
                && (responseTextArea.getText() == null || responseTextArea.getText().trim().isEmpty())) {
            showError("Veuillez saisir une réponse pour cette action");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}
