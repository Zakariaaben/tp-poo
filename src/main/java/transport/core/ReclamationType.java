

package transport.core;


public enum ReclamationType {
    TECHNIQUE("Technique"),
    PAIEMENT("Paiement"),
    SERVICE("Service"),
    AUTRE("Autre");
    private final String libelle;
    ReclamationType(String libelle) {
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
