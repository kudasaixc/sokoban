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

    public static void main(String[] args) throws InterruptedException {
        // Meme grande pile que le programme principal (DFS recursif)
        Thread t = new Thread(null, SokobanTest::lancerTests, "tests", Sokoban.TAILLE_PILE);
        t.start();
        t.join();
        System.out.println();
        System.out.println(reussis + " test(s) reussi(s), " + echecs + " echec(s)");
        if (echecs > 0) {
            System.exit(1);
        }
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

    // Rejoue une solution sur le niveau et indique si elle mene a la victoire
    static boolean solutionGagnante(int niveau, char[] solution) {
        Sokoban.niveauCourant = niveau;
        Sokoban.chargerNiveau(niveau);
        for (int i = 0; i < solution.length; i++) {
            if (!Sokoban.deplacerJoueur(Sokoban.directionDepuisTouche(solution[i]))) {
                return false;
            }
        }
        return Sokoban.partieTerminee();
    }

    static final int HAUT = 0;
    static final int BAS = 1;
    static final int GAUCHE = 2;
    static final int DROITE = 3;

    static void lancerTests() {
        testerNiveaux();
        testerVerification();
        testerDeplacements();
        testerAnnulation();
        testerSituationBloquee();
        testerSolveurs();
        testerSansSolution();
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

    static void testerAnnulation() {
        System.out.println("--- Annulation (bonus) ---");
        Sokoban.chargerNiveau(1);
        String avant = texteGrille();
        verifier(!Sokoban.annulerDernierCoup(), "rien a annuler au depart");
        Sokoban.deplacerJoueur(GAUCHE);
        Sokoban.deplacerJoueur(HAUT);
        Sokoban.deplacerJoueur(DROITE);
        String apres = texteGrille();
        verifier(!avant.equals(apres), "la poussee a modifie la grille");
        Sokoban.annulerDernierCoup();
        Sokoban.annulerDernierCoup();
        Sokoban.annulerDernierCoup();
        verifier(avant.equals(texteGrille()), "trois annulations restaurent la grille (caisse comprise)");
        verifier(Sokoban.nbMouvements == 0, "le compteur revient a 0");
    }

    static void testerSituationBloquee() {
        System.out.println("--- Situations bloquees (bonus) ---");
        Sokoban.chargerGrille(new String[] {
            "######",
            "#    #",
            "# $@.#",
            "######"
        });
        verifier(!Sokoban.situationBloquee(), "caisse libre : pas de blocage");
        verifier(Sokoban.estCoinMort(1, 1), "un coin sans objectif est un coin mort");
        verifier(!Sokoban.estCoinMort(2, 4), "un coin avec objectif n'est pas un coin mort");
        Sokoban.deplacerJoueur(GAUCHE);
        verifier(Sokoban.situationBloquee(), "caisse poussee dans un coin : blocage detecte");
    }

    static void testerSolveurs() {
        System.out.println("--- Solveurs DFS et BFS ---");
        for (int i = 0; i < Sokoban.NIVEAUX.length; i++) {
            Sokoban.niveauCourant = i;
            char[] bfs = Sokoban.resoudreBFS();
            int etatsBFS = Sokoban.etatsExplores;
            verifier(bfs != null, "niveau " + (i + 1) + " : BFS trouve une solution");
            verifier(bfs != null && solutionGagnante(i, bfs), "niveau " + (i + 1) + " : solution BFS gagnante");

            char[] dfs = Sokoban.resoudreDFS();
            verifier(dfs != null, "niveau " + (i + 1) + " : DFS trouve une solution");
            verifier(dfs != null && solutionGagnante(i, dfs), "niveau " + (i + 1) + " : solution DFS gagnante");
            verifier(bfs != null && dfs != null && bfs.length <= dfs.length,
                     "niveau " + (i + 1) + " : BFS pas plus long que DFS");
            verifier(etatsBFS > 0 && Sokoban.etatsExplores > 0, "niveau " + (i + 1) + " : etats explores comptes");
        }
        Sokoban.niveauCourant = 0;
        char[] bfs = Sokoban.resoudreBFS();
        verifier(bfs != null && bfs.length == 1 && bfs[0] == 'z', "niveau 1 : solution optimale = z");

        // Verification independante de l'optimalite : aucune suite de 13 coups ne gagne le niveau 2,
        // alors que BFS en trouve une de 14
        Sokoban.niveauCourant = 1;
        bfs = Sokoban.resoudreBFS();
        verifier(bfs != null && bfs.length == 14, "niveau 2 : BFS trouve 14 mouvements");
        verifier(!gagnableEnAuPlus(1, 13), "niveau 2 : aucune solution en 13 mouvements ou moins (recherche exhaustive)");
    }

    // Recherche exhaustive naive (sans etats visites) : existe-t-il une victoire en au plus n coups ?
    static boolean gagnableEnAuPlus(int niveau, int n) {
        Sokoban.niveauCourant = niveau;
        Sokoban.chargerNiveau(niveau);
        return essayer(n);
    }

    static boolean essayer(int coupsRestants) {
        if (Sokoban.partieTerminee()) {
            return true;
        }
        if (coupsRestants == 0) {
            return false;
        }
        for (int d = 0; d < 4; d++) {
            if (Sokoban.deplacerJoueur(d)) {
                boolean gagne = essayer(coupsRestants - 1);
                Sokoban.annulerDernierCoup();
                if (gagne) {
                    return true;
                }
            }
        }
        return false;
    }

    static void testerSansSolution() {
        System.out.println("--- Niveau sans solution ---");
        String[] impossible = {
            "#######",
            "#$    #",
            "#  @ .#",
            "#######"
        };
        String[] sauvegarde = Sokoban.NIVEAUX[0];
        Sokoban.NIVEAUX[0] = impossible;
        Sokoban.niveauCourant = 0;
        verifier(Sokoban.resoudreBFS() == null && !Sokoban.limiteAtteinte, "BFS : aucune solution, sans atteindre la limite");
        verifier(Sokoban.resoudreDFS() == null && !Sokoban.limiteAtteinte, "DFS : aucune solution, sans atteindre la limite");
        Sokoban.NIVEAUX[0] = sauvegarde;
    }
}
