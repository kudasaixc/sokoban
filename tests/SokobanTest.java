/*
 * Tests du projet Sokoban, sans bibliotheque externe.
 * Chaque verification affiche OK ou ECHEC ; le programme se termine
 * avec le code 1 si au moins un test echoue.
 *
 * Compilation et lancement : voir run-tests.sh
 */
public class SokobanTest {

    static int reussis = 0;
    static int echecs = 0;

    public static void main(String[] args) {
        lancerTests();
        System.out.println(reussis + " test(s) reussi(s), " + echecs + " echec(s)");
        if (echecs > 0) System.exit(1);
    }

    static void verifier(boolean condition, String description) {
        if (condition) {
            reussis++;
            System.out.println("OK     " + description);
        } else {
            echecs++;
            System.out.println("ECHEC  " + description);
        }
    }

    // Texte de la grille courante tel qu'il est affiche
    static String texteGrille() {
        StringBuilder texte = new StringBuilder();
        for (int l = 0; l < Sokoban.hauteur; l++) {
            for (int c = 0; c < Sokoban.largeur; c++) {
                texte.append(Sokoban.symbole(l, c));
            }
            texte.append('\n');
        }
        return texte.toString();
    }

    static final int HAUT = 0;
    static final int BAS = 1;
    static final int GAUCHE = 2;
    static final int DROITE = 3;

    static void lancerTests() {
        testerNiveaux();
        testerVerification();
        testerDeplacements();
    }

    static void testerNiveaux() {
        System.out.println("--- Niveaux fournis ---");
        verifier(Sokoban.NIVEAUX.length >= 3, "au moins trois niveaux");
        verifier(Sokoban.NIVEAUX.length == Sokoban.NOMS_NIVEAUX.length, "un nom par niveau");
        for (int i = 0; i < Sokoban.NIVEAUX.length; i++) {
            verifier(Sokoban.verifierNiveau(Sokoban.NIVEAUX[i]) == null, "niveau " + (i + 1) + " valide");
            Sokoban.chargerNiveau(i);
            verifier(chargementFidele(i), "niveau " + (i + 1) + " affiche tel qu'il est defini");
            verifier(!Sokoban.partieTerminee(), "niveau " + (i + 1) + " non gagne au depart");
        }
    }

    // Compare case par case (les lignes courtes sont completees par des espaces)
    static boolean chargementFidele(int niveau) {
        String[] lignes = Sokoban.NIVEAUX[niveau];
        for (int l = 0; l < lignes.length; l++) {
            for (int c = 0; c < Sokoban.largeur; c++) {
                char attendu = c < lignes[l].length() ? lignes[l].charAt(c) : ' ';
                if (Sokoban.symbole(l, c) != attendu) {
                    return false;
                }
            }
        }
        return true;
    }

    static void testerVerification() {
        System.out.println("--- Verification des niveaux ---");
        verifier(Sokoban.verifierNiveau(new String[] {"#####", "# $.#", "#####"}) != null, "refus sans joueur");
        verifier(Sokoban.verifierNiveau(new String[] {"######", "#@@$.#", "######"}) != null, "refus avec deux joueurs");
        verifier(Sokoban.verifierNiveau(new String[] {"#####", "#@ .#", "#####"}) != null, "refus sans caisse");
        verifier(Sokoban.verifierNiveau(new String[] {"######", "#@$$.#", "######"}) != null, "refus caisses != objectifs");
        verifier(Sokoban.verifierNiveau(new String[] {"#####", "#@$X#", "#####"}) != null, "refus caractere inconnu");
        verifier(Sokoban.verifierNiveau(new String[] {"######", "#+$ *#", "# $. #", "######"}) == null,
                 "accepte + et * (joueur / caisse sur objectif)");
    }

    static void testerDeplacements() {
        System.out.println("--- Deplacements ---");
        Sokoban.chargerGrille(new String[] {
            "#######",
            "#     #",
            "# @$ .#",
            "#     #",
            "#######"
        });
        verifier(Sokoban.joueurLigne == 2 && Sokoban.joueurColonne == 2, "position initiale du joueur");

        verifier(Sokoban.deplacerJoueur(HAUT), "deplacement vers une case libre");
        verifier(!Sokoban.deplacerJoueur(HAUT), "impossible de traverser un mur");
        verifier(Sokoban.nbMouvements == 1, "un mouvement refuse n'est pas compte");

        Sokoban.deplacerJoueur(BAS);
        verifier(Sokoban.deplacerJoueur(DROITE), "pousser une caisse vers une case libre");
        verifier(Sokoban.grille[2][4] == '$' && Sokoban.grille[2][3] == '@', "la caisse avance avec le joueur");
        verifier(!Sokoban.partieTerminee(), "pas encore de victoire");
        verifier(Sokoban.deplacerJoueur(DROITE), "pousser la caisse sur l'objectif");
        verifier(Sokoban.symbole(2, 5) == '*', "caisse sur objectif affichee *");
        verifier(Sokoban.partieTerminee(), "victoire detectee");
        verifier(Sokoban.nbMouvements == 4, "compteur de mouvements = 4");
        verifier(!Sokoban.deplacerJoueur(DROITE), "impossible de pousser une caisse dans un mur");

        Sokoban.chargerGrille(new String[] {
            "########",
            "#@$$ ..#",
            "########"
        });
        verifier(!Sokoban.deplacerJoueur(DROITE), "impossible de pousser deux caisses");
        verifier(Sokoban.directionDepuisTouche('z') == HAUT && Sokoban.directionDepuisTouche('d') == DROITE,
                 "correspondance des touches z/s/q/d");
        verifier(Sokoban.directionDepuisTouche('w') == -1, "touche inconnue refusee");
    }

}
