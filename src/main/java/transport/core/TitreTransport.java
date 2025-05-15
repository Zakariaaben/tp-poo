package transport.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class TitreTransport {
       
    protected static Integer id = 0;
    protected Integer currentId;
    protected LocalDateTime dateAchat;
    protected Integer prix;
    protected UUID personneId;

    public Integer getCurrentId() {
        return currentId;
    }

    public UUID getPersonneId() {
        return personneId;
    }

    TitreTransport(Personne usager, LocalDateTime dateAchat) {
        id++;
        currentId = id;
        this.personneId = usager.getId();
        this.dateAchat = dateAchat;
    }

    public boolean estValide(LocalDate of) {
        return  true;
    }

    public LocalDateTime getDateAchat() {
        return dateAchat;
    }

    public String getId() {
        return id.toString();
    }

    public String getPrix() {
        return this.prix.toString();
    }

    abstract boolean isValid();
}