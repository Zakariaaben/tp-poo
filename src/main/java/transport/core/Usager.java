package transport.core;
import java.time.*;

// Classe Usager
public class Usager extends Personne {
    
    // Constructor used by the application
    public Usager(String name, String familyName, LocalDate birthData, boolean hasHandicap) {
        super(name, familyName, birthData, hasHandicap);
    }
    
    // Default constructor for GSON
    public Usager() {
        super("", "", LocalDate.now(), false);
    }
}