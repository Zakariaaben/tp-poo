package transport.core;

import java.time.LocalDate;
import java.util.UUID;

public class Personne {
    private String name;
    private String familyName;
    private LocalDate birthDate;
    private boolean hasHandicap;
    private UUID id;

    Personne(String name, String familyName, LocalDate birthDate, boolean hadHandicap) {
        this.name = name;
        this.familyName = familyName;
        this.birthDate = birthDate;
        this.hasHandicap = hadHandicap;
        this.id = UUID.randomUUID();
    }

    public String getName() {
        return name;
    }

    public String getFamilyName() {
        return familyName;
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public boolean hasHandicap() {
        return hasHandicap;
    }

    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    @Override
    public String toString() {
        return name + " " + familyName;
    }
}
