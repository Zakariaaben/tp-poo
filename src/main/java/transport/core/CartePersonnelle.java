package transport.core;
import java.time.LocalDateTime;

public class CartePersonnelle extends TitreTransport {
    TypeCarte type;

    public CartePersonnelle(Personne usager) throws ReductionImpossibleException {
        super(usager, LocalDateTime.now());
        this.prix = 5000;

        // Calcul des réductions possibles
        double minPrix = this.prix;
        TypeCarte bestType = null;

        // Employé : 40% réduction
        if (usager instanceof Employe) {
            double prixReduit = this.prix * 0.6;
            if (prixReduit < minPrix) {
                minPrix = prixReduit;
                bestType = TypeCarte.PARTENAIRE;
            }
        }

        // Handicap : 50% réduction
        if (usager.hasHandicap()) {
            double prixReduit = this.prix * 0.5;
            if (prixReduit < minPrix) {
                minPrix = prixReduit;
                bestType = TypeCarte.SOLIDATIE;
            }
        }

        // Age
        int age = LocalDateTime.now().getYear() - usager.getBirthDate().getYear();
        if (age < 25) {
            double prixReduit = this.prix * 0.7;
            if (prixReduit < minPrix) {
                minPrix = prixReduit;
                bestType = TypeCarte.JUNIOR;
            }
        } else if (age > 65) {
            double prixReduit = this.prix * 0.75;
            if (prixReduit < minPrix) {
                minPrix = prixReduit;
                bestType = TypeCarte.SENIOR;
            }
        }

        if (bestType == null) {
            throw new ReductionImpossibleException("Pas de réduction applicable à cette personne.");
        }

        this.type = bestType;
        this.prix = (int) minPrix;
    }

    @Override
    boolean isValid() {
        return false;
    }
}
