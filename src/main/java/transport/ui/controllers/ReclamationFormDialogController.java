package transport.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import transport.core.Personne;
import transport.core.Reclamation;
import transport.core.ReclamationType;
import transport.services.PersonneService;
import transport.services.ReclamationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReclamationFormDialogController {

    @FXML
    private ComboBox<Personne> personneComboBox;

    @FXML
    private ComboBox<ReclamationType> typeComboBox;

    @FXML
    private Label dateLabel;

    @FXML
    private TextArea descriptionTextArea;

    @FXML
    private Label errorLabel;

    private Dialog<Reclamation> dialog;
    private ReclamationService reclamationService;
    private PersonneService personneService;

    public void initialize(Dialog<Reclamation> dialog, ReclamationService reclamationService, PersonneService personneService) {
        this.dialog = dialog;
        this.reclamationService = reclamationService;
        this.personneService = personneService;

        setupControls();
        setupDialogResultConverter();
    }

    private void setupControls() {
        // Set current date
        dateLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // Load persons into combo box
        List<Personne> personnes = personneService.getAllPersonnes();
        personneComboBox.getItems().addAll(personnes);

        // Setup person display in combo box
        personneComboBox.setConverter(new StringConverter<Personne>() {
            @Override
            public String toString(Personne personne) {
                if (personne == null) {
                    return "";
                }
                return personne.getName() + " " + personne.getFamilyName();
            }

            @Override
            public Personne fromString(String string) {
                return null; // Not needed as we're not typing in the combo box
            }
        });

        // Load reclamation types
        typeComboBox.getItems().addAll(ReclamationType.values());

        // Setup type display
        typeComboBox.setConverter(new StringConverter<ReclamationType>() {
            @Override
            public String toString(ReclamationType type) {
                if (type == null) {
                    return "";
                }
                return type.getLibelle();
            }

            @Override
            public ReclamationType fromString(String string) {
                return null; // Not needed
            }
        });

        // Clear error on field changes
        personneComboBox.valueProperty().addListener((obs, oldVal, newVal) -> clearError());
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> clearError());
        descriptionTextArea.textProperty().addListener((obs, oldVal, newVal) -> clearError());
    }

    private void setupDialogResultConverter() {
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                if (validateForm()) {
                    Personne selectedPersonne = personneComboBox.getValue();
                    ReclamationType selectedType = typeComboBox.getValue();
                    String description = descriptionTextArea.getText().trim();

                    return new Reclamation(selectedPersonne, description, selectedType);
                } else {
                    return null; // Validation failed, don't close dialog
                }
            }
            return null;
        });
    }

    private boolean validateForm() {
        if (personneComboBox.getValue() == null) {
            showError("Veuillez sélectionner une personne");
            return false;
        }

        if (typeComboBox.getValue() == null) {
            showError("Veuillez sélectionner un type de réclamation");
            return false;
        }

        String description = descriptionTextArea.getText();
        if (description == null || description.trim().isEmpty()) {
            showError("Veuillez saisir une description");
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
