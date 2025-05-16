package transport.core;

import java.time.LocalDateTime;
import java.util.UUID;

public class Reclamation {

    private UUID id;
    private UUID personneId;
    private String description;
    private ReclamationType type;
    private ReclamationStatus etat;
    private LocalDateTime dateReclamation;
    private LocalDateTime dateTraitement;
    private String reponse;

    public Reclamation(Personne personne, String description, ReclamationType type) {
        this.id = UUID.randomUUID();
        this.personneId = personne.getId();
        this.description = description;
        this.etat = ReclamationStatus.EN_COURS;
        this.dateReclamation = LocalDateTime.now();
        this.type = type;
    }

    // Constructor for deserialization
    public Reclamation() {
        this.id = UUID.randomUUID();
    }

    public void traiter(String reponse) {
        this.etat = ReclamationStatus.TRAITE;
        this.dateTraitement = LocalDateTime.now();
        this.reponse = reponse;
    }

    public void refuser(String reponse) {
        this.etat = ReclamationStatus.REFUSE;
        this.dateTraitement = LocalDateTime.now();
        this.reponse = reponse;
    }

    public void annuler() {
        this.etat = ReclamationStatus.ANNULE;
        this.dateTraitement = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPersonneId() {
        return personneId;
    }

    public void setPersonneId(UUID personneId) {
        this.personneId = personneId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReclamationType getType() {
        return type;
    }

    public void setType(ReclamationType type) {
        this.type = type;
    }

    public ReclamationStatus getEtat() {
        return etat;
    }

    public void setEtat(ReclamationStatus etat) {
        this.etat = etat;
    }

    public LocalDateTime getDateReclamation() {
        return dateReclamation;
    }

    public void setDateReclamation(LocalDateTime dateReclamation) {
        this.dateReclamation = dateReclamation;
    }

    public LocalDateTime getDateTraitement() {
        return dateTraitement;
    }

    public void setDateTraitement(LocalDateTime dateTraitement) {
        this.dateTraitement = dateTraitement;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public boolean isEnCours() {
        return etat == ReclamationStatus.EN_COURS;
    }

    public boolean isTraite() {
        return etat == ReclamationStatus.TRAITE;
    }

    public boolean isRefuse() {
        return etat == ReclamationStatus.REFUSE;
    }

    public boolean isAnnule() {
        return etat == ReclamationStatus.ANNULE;
    }
}
