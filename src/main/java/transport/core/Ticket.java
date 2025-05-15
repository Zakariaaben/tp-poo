package transport.core;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Ticket extends TitreTransport {
    private boolean used = false;

    public Ticket(Personne personne, LocalDateTime dateAchat) {
        super(personne, dateAchat);
        this.prix = 50;
    }



    @Override
    public boolean isValid() {
        // Valide si non utilisé et la date d'achat est aujourd'hui
        return !used && dateAchat.toLocalDate().equals(LocalDate.now());
    }

    public boolean isUsed() {
        return used;
    }
    
    public void setUsed(boolean used) {
        this.used = used;
    }

    public void useTicket() {
        if (!isValid()) {
            throw new IllegalStateException("Le ticket n'est pas valide ou déjà utilisé.");
        }
        this.used = true;
    }
}
