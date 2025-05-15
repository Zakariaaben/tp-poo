package transport.core;

public enum TypeCarte {
    JUNIOR("Junior"),
    SENIOR("Senior"),
    SOLIDATIE("Solidarité"),
    PARTENAIRE("Partenaire");
    
    private final String libelle;
    
    TypeCarte(String libelle) {
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
