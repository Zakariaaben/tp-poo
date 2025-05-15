package transport.core;

public enum Fonction {
    CHAUFFEUR("Chauffeur"),
    CONTROLEUR("Contrôleur"),
    ADMINISTRATIF("Administratif"),
    TECHNIQUE("Technique");
    
    private final String libelle;
    
    Fonction(String libelle) {
        this.libelle = libelle;
    }
    
    public String getLibelle() {
        return libelle;
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}
