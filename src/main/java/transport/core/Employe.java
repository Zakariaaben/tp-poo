package transport.core;
import java.time.LocalDate;

// Classe Employé
public class Employe extends Personne {
    private String matricule;
    private Fonction fonction;
    

    // Constructor for the application
    public Employe(String name, String familyName, LocalDate birthDate, boolean hasHandicap, String matricule,Fonction fonction) {
        super(name, familyName, birthDate, hasHandicap);
        this.matricule = matricule;
        this.fonction = fonction;
    }
    
    // Default constructor for GSON
    public Employe() {
        super("", "", LocalDate.now(), false);
    }
    
    public String getMatricule() {
        return matricule;
    }
    
    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }
    
    public Fonction getFonction() {
        return fonction;
    }
    
    public void setFonction(Fonction fonction) {
        this.fonction = fonction;
    }


}