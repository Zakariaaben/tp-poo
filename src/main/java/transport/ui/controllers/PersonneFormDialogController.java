package transport.ui.controllers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import transport.core.Employe;
import transport.core.Fonction;
import transport.core.Personne;
import transport.core.Usager;

public class PersonneFormDialogController {

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private TextField nameField;

    @FXML
    private TextField familyNameField;

    @FXML
    private DatePicker birthDatePicker;

    @FXML
    private CheckBox handicapCheckBox;

    @FXML
    private GridPane employeFields;

    @FXML
    private TextField matriculeField;

    @FXML
    private ComboBox<Fonction> fonctionComboBox;

    private Dialog<Personne> dialog;
    private Personne editPersonne;

    @FXML
    public void initialize() {
        // Setup the type choice options
        typeComboBox.getItems().addAll("Employé", "Usager");
        typeComboBox.setValue("Employé"); // Default

        // Show/hide employe fields based on selection
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            employeFields.setVisible("Employé".equals(newVal));
            employeFields.setManaged("Employé".equals(newVal));
        });

        // Configure date picker format
        birthDatePicker.setConverter(new StringConverter<LocalDate>() {
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate date) {
                return date != null ? formatter.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                return string != null && !string.isEmpty() ? LocalDate.parse(string, formatter) : null;
            }
        });

        // Initialize fonction combo box with enum values
        fonctionComboBox.getItems().addAll(Fonction.values());
    }

    public void setDialog(Dialog<Personne> dialog) {
        this.dialog = dialog;

        // Set the result converter to create the appropriate Personne subclass
        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData().isCancelButton()) {
                return null;
            }

            // Validate form before creating objects
            if (!validateForm()) {
                return null;
            }

            // Create the appropriate subclass
            if ("Employé".equals(typeComboBox.getValue())) {
                Employe employe = new Employe(
                        nameField.getText(),
                        familyNameField.getText(),
                        birthDatePicker.getValue(),
                        handicapCheckBox.isSelected(),
                        matriculeField.getText(),
                        fonctionComboBox.getValue()
                );
                if (editPersonne != null) {
                    employe.setId(editPersonne.getId());
                }
                return employe;
            } else {
                Usager usager = new Usager(
                        nameField.getText(),
                        familyNameField.getText(),
                        birthDatePicker.getValue(),
                        handicapCheckBox.isSelected()
                );
                if (editPersonne != null) {
                    usager.setId(editPersonne.getId());
                }
                return usager;
            }
        });
    }

    public void setPersonne(Personne personne) {
        this.editPersonne = personne;

        if (personne instanceof Employe) {
            typeComboBox.setValue("Employé");
            matriculeField.setText(((Employe) personne).getMatricule());
            fonctionComboBox.setValue(((Employe) personne).getFonction());
        } else if (personne instanceof Usager) {
            typeComboBox.setValue("Usager");
        }

        nameField.setText(personne.getName());
        familyNameField.setText(personne.getFamilyName());
        birthDatePicker.setValue(personne.getBirthDate());
        handicapCheckBox.setSelected(personne.hasHandicap());
    }

    // Add validation method
    private boolean validateForm() {
        // Basic validation
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showAlert("Le nom est obligatoire");
            return false;
        }

        if (familyNameField.getText() == null || familyNameField.getText().trim().isEmpty()) {
            showAlert("Le prénom est obligatoire");
            return false;
        }

        if (birthDatePicker.getValue() == null) {
            showAlert("La date de naissance est obligatoire");
            return false;
        }

        // If employee is selected, validate employee-specific fields
        if ("Employé".equals(typeComboBox.getValue())) {
            if (matriculeField.getText() == null || matriculeField.getText().trim().isEmpty()) {
                showAlert("Le matricule est obligatoire pour un employé");
                return false;
            }

            if (fonctionComboBox.getValue() == null) {
                showAlert("La fonction est obligatoire pour un employé");
                return false;
            }
        }

        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
