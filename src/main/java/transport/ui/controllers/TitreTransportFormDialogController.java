package transport.ui.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.util.StringConverter;
import transport.core.CartePersonnelle;
import transport.core.ModeDePaiement;
import transport.core.Personne;
import transport.core.ReductionImpossibleException;
import transport.core.TitreTransport;
import transport.services.PersonneService;
import transport.services.TitreTransportService;

public class TitreTransportFormDialogController {

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private ComboBox<Personne> personneComboBox;

    @FXML
    private ComboBox<ModeDePaiement> paiementComboBox;

    @FXML
    private TitledPane detailsPane;

    @FXML
    private Label prixLabel;

    @FXML
    private Label reductionLabel;

    @FXML
    private Label finalPrixLabel;

    @FXML
    private Label errorLabel;

    private Dialog<TitreTransport> dialog;
    private PersonneService personneService;
    private TitreTransportService titreService;

    @FXML
    public void initialize() {
        // Initialize type choices
        typeComboBox.getItems().addAll("Ticket", "Carte personnelle");
        typeComboBox.setValue("Ticket");

        // Initialize payment mode choices
        paiementComboBox.setItems(FXCollections.observableArrayList(ModeDePaiement.values()));
        paiementComboBox.setValue(ModeDePaiement.ESPECE);

        // Setup converter for personneComboBox
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
                return null; // Not used for ComboBox
            }
        });

        // Add listeners for dynamic price calculation
        personneComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePriceDetails());
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePriceDetails());

        // Initially hide price details until a person is selected
        detailsPane.setExpanded(false);
    }

    public void setDialog(Dialog<TitreTransport> dialog) {
        this.dialog = dialog;

        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                return createTitreFromForm();
            }
            return null;
        });
    }

    public void setServices(PersonneService personneService, TitreTransportService titreService) {
        this.personneService = personneService;
        this.titreService = titreService;

        // Load personne list into combo box
        personneComboBox.setItems(FXCollections.observableArrayList(personneService.getAllPersonnes()));
    }

    private TitreTransport createTitreFromForm() {
        clearError();

        // Validate form
        if (!validateForm()) {
            return null;
        }

        try {
            Personne personne = personneComboBox.getValue();
            ModeDePaiement paiement = paiementComboBox.getValue();
            String titreType = typeComboBox.getValue();

            TitreTransport titre;

            if ("Ticket".equals(titreType)) {
                titre = titreService.createTicket(personne, paiement);
                return titre;
            } else {
                try {
                    titre = titreService.createCarte(personne, paiement);
                    return titre;
                } catch (ReductionImpossibleException e) {
                    System.out.println(e.getMessage());
                    showError("Cette personne ne bénéficie d'aucune réduction pour une carte personnelle.");
                    return null;
                }
            }
        } catch (Exception e) {
            showError("Erreur lors de la création du titre: " + e.getMessage());
            return null;
        }
    }

    private boolean validateForm() {
        if (personneComboBox.getValue() == null) {
            showError("Veuillez sélectionner une personne.");
            return false;
        }

        if (paiementComboBox.getValue() == null) {
            showError("Veuillez sélectionner un mode de paiement.");
            return false;
        }

        return true;
    }

    private void updatePriceDetails() {
        Personne personne = personneComboBox.getValue();
        String type = typeComboBox.getValue();

        if (personne == null) {
            detailsPane.setExpanded(false);
            return;
        }

        detailsPane.setExpanded(true);

        if ("Ticket".equals(type)) {
            prixLabel.setText("Prix du ticket: 50 DA");
            reductionLabel.setText("Aucune réduction applicable");
            finalPrixLabel.setText("Prix final: 50 DA");
        } else {
            // For CartePersonnelle, calculate possible reductions
            prixLabel.setText("Prix de base: 5000 DA");

            try {
                // Try to simulate card creation to see if reductions apply
                CartePersonnelle simulatedCard = new CartePersonnelle(personne);
                String reductionType = simulatedCard.getType().getLibelle();
                int finalPrice = Integer.parseInt(simulatedCard.getPrix());
                int basePrice = 5000;
                int reduction = basePrice - finalPrice;

                reductionLabel.setText("Réduction (" + reductionType + "): " + reduction + " DA");
                finalPrixLabel.setText("Prix final: " + finalPrice + " DA");
            } catch (ReductionImpossibleException e) {
                reductionLabel.setText("Aucune réduction applicable");
                finalPrixLabel.setText("Prix final: 5000 DA");
                showError("Cette personne ne bénéficie d'aucune réduction pour une carte personnelle.");
            }
        }
    }

    private void showError(String message) {
        // Set the error message in the label
        errorLabel.setText(message);

        try {
            // Also show an alert dialog for better visibility
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Make sure the dialog has the proper owner (the dialog we're currently showing)
            if (dialog != null && dialog.getDialogPane() != null
                    && dialog.getDialogPane().getScene() != null
                    && dialog.getDialogPane().getScene().getWindow() != null) {
                alert.initOwner(dialog.getDialogPane().getScene().getWindow());
            }

            System.out.println("Displaying error dialog: " + message);
            alert.showAndWait();
        } catch (Exception e) {
            // If the alert itself fails, at least log the error
            System.err.println("Failed to show error dialog: " + e.getMessage());
            e.printStackTrace();
            // Fallback to console output if dialog fails
            System.err.println("ERROR: " + message);
        }
    }

    private void clearError() {
        errorLabel.setText("");
    }
}
