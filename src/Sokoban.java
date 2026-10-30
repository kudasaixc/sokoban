import java.util.Scanner;

/* Projet Java - Sokoban : niveau standard, jeu console. */
public class Sokoban {

    // ===================================================================
    // Niveaux (stockes dans le programme sous forme de lignes de caracteres)
    // # mur, ' ' case libre, @ joueur, $ caisse, . objectif,
    // * caisse sur objectif, + joueur sur objectif
    // ===================================================================

    static final String[] NOMS_NIVEAUX = {
        "Premiers pas",
        "Deux caisses",
        "Le couloir",
        "L'entrepot"
    };

    static final String[][] NIVEAUX = {
        {
            "########",
            "#      #",
            "#   .  #",
            "#   $  #",
            "#   @  #",
            "#      #",
            "########"
        },
        {
            "#######",
            "#     #",
            "# .$. #",
            "#  $  #",
            "#  @  #",
            "#######"
        },
        {
            "#########",
            "#   #   #",
            "# $   $ #",
            "#  ##.  #",
            "# . @ . #",
            "#  $##  #",
            "#       #",
            "#########"
        },
        {
            " ########",
            " #   #  #",
            "##$  $  #",
            "#  ##  .#",
            "# @ $ # #",
            "#.  #   #",
            "###   .##",
            "  ######"
        }
    };

    // Directions : indice 0 = haut, 1 = bas, 2 = gauche, 3 = droite
    static final char[] TOUCHES = {'z', 's', 'q', 'd'};
    static final int[] DL = {-1, 1, 0, 0};
    static final int[] DC = {0, 0, -1, 1};

    // ===================================================================
    // Etat de la partie en cours
    // ===================================================================

    // grille contient seulement '#', ' ', '@' et '$' ; les objectifs sont a part
    static char[][] grille;
    static boolean[][] objectifs;
    static int hauteur;
    static int largeur;
    static int joueurLigne;
    static int joueurColonne;
    static int nbMouvements;
    static int niveauCourant = 0;

    static Scanner clavier = new Scanner(System.in);

    // ===================================================================
    // Programme principal et menus
    // ===================================================================

    public static void main(String[] args) {
        menuPrincipal();
    }

    static void menuPrincipal() {
        while (true) {
            System.out.println();
            System.out.println("================================");
            System.out.println("          SOKOBAN JAVA");
            System.out.println("================================");
            System.out.println("Niveau actuel : " + (niveauCourant + 1) + " - " + NOMS_NIVEAUX[niveauCourant]);
            System.out.println("1. Nouvelle partie");
            System.out.println("2. Choisir un niveau");
            System.out.println("3. Afficher les regles");
            System.out.println("4. Quitter");
            System.out.print("Votre choix : ");

            int choix = lireChoix(1, 4);
            if (choix == -1 || choix == 4) {
                System.out.println("Au revoir !");
                return;
            }
            if (choix == 0) {
                continue;
            }
            if (choix == 1) {
                chargerNiveau(niveauCourant);
                jouer();
            } else if (choix == 2) {
                choisirNiveau();
            } else {
                afficherRegles();
            }
        }
    }

    // Affiche la liste des niveaux, puis lance une partie sur le niveau choisi
    static void choisirNiveau() {
        System.out.println();
        System.out.println("===== SOKOBAN =====");
        for (int i = 0; i < NIVEAUX.length; i++) {
            System.out.println((i + 1) + ". Niveau " + (i + 1) + " - " + NOMS_NIVEAUX[i]);
        }
        System.out.println((NIVEAUX.length + 1) + ". Retour");
        System.out.print("Votre choix : ");

        int choix = lireChoix(1, NIVEAUX.length + 1);
        if (choix <= 0 || choix == NIVEAUX.length + 1) {
            return;
        }
        niveauCourant = choix - 1;
        chargerNiveau(niveauCourant);
        jouer();
    }

    static void afficherRegles() {
        System.out.println();
        System.out.println("===== REGLES =====");
        System.out.println("Placez toutes les caisses ($) sur les objectifs (.).");
        System.out.println("Le joueur (@) se deplace d'une case a la fois, sans traverser les murs (#).");
        System.out.println("Il peut pousser une caisse si la case derriere elle est libre,");
        System.out.println("mais il ne peut jamais tirer une caisse ni en pousser deux a la fois.");
        System.out.println();
        System.out.println("Symboles : # mur, @ joueur, $ caisse, . objectif,");
        System.out.println("           * caisse sur objectif, + joueur sur objectif");
        System.out.println();
        System.out.println("Commandes : z haut, s bas, q gauche, d droite, x quitter");
    }

    // Lit un entier entre min et max. Renvoie 0 si la saisie est invalide, -1 en fin d'entree
    static int lireChoix(int min, int max) {
        String saisie = lireLigne();
        if (saisie == null) {
            return -1;
        }
        int valeur;
        try {
            valeur = Integer.parseInt(saisie);
        } catch (NumberFormatException e) {
            System.out.println("Saisie invalide : entrez un nombre entre " + min + " et " + max + ".");
            return 0;
        }
        if (valeur < min || valeur > max) {
            System.out.println("Saisie invalide : entrez un nombre entre " + min + " et " + max + ".");
            return 0;
        }
        return valeur;
    }

    // Lit une ligne au clavier (sans espaces autour, en minuscules). Renvoie null en fin d'entree
    static String lireLigne() {
        if (!clavier.hasNextLine()) {
            return null;
        }
        return clavier.nextLine().trim().toLowerCase();
    }

    // ===================================================================
    // Chargement et verification d'un niveau
    // ===================================================================

    static void chargerNiveau(int numero) {
        chargerGrille(NIVEAUX[numero]);
    }

    // Construit la grille de jeu a partir des lignes d'un niveau
    static void chargerGrille(String[] lignes) {
        String erreur = verifierNiveau(lignes);
        if (erreur != null) {
            throw new IllegalArgumentException("Niveau invalide : " + erreur);
        }

        hauteur = lignes.length;
        largeur = 0;
        for (int l = 0; l < hauteur; l++) {
            if (lignes[l].length() > largeur) {
                largeur = lignes[l].length();
            }
        }

        grille = new char[hauteur][largeur];
        objectifs = new boolean[hauteur][largeur];
        for (int l = 0; l < hauteur; l++) {
            for (int c = 0; c < largeur; c++) {
                // les lignes plus courtes sont completees par des cases vides
                char s = ' ';
                if (c < lignes[l].length()) {
                    s = lignes[l].charAt(c);
                }
                objectifs[l][c] = (s == '.' || s == '*' || s == '+');
                if (s == '#') {
                    grille[l][c] = '#';
                } else if (s == '$' || s == '*') {
                    grille[l][c] = '$';
                } else if (s == '@' || s == '+') {
                    grille[l][c] = '@';
                    joueurLigne = l;
                    joueurColonne = c;
                } else {
                    grille[l][c] = ' ';
                }
            }
        }
        nbMouvements = 0;
    }

    // Verifie les regles du sujet. Renvoie null si le niveau est correct, sinon un message
    static String verifierNiveau(String[] lignes) {
        int joueurs = 0;
        int caisses = 0;
        int cibles = 0;
        int murs = 0;
        int libres = 0;
        for (int l = 0; l < lignes.length; l++) {
            for (int c = 0; c < lignes[l].length(); c++) {
                char s = lignes[l].charAt(c);
                if (s == '#') {
                    murs++;
                } else if (s == ' ') {
                    libres++;
                } else if (s == '.') {
                    cibles++;
                    libres++;
                } else if (s == '$') {
                    caisses++;
                } else if (s == '*') {
                    caisses++;
                    cibles++;
                } else if (s == '@') {
                    joueurs++;
                } else if (s == '+') {
                    joueurs++;
                    cibles++;
                } else {
                    return "caractere inconnu '" + s + "'";
                }
            }
        }
        if (joueurs != 1) {
            return "il faut exactement un joueur";
        }
        if (caisses < 1) {
            return "il faut au moins une caisse";
        }
        if (caisses != cibles) {
            return "il faut autant d'objectifs que de caisses";
        }
        if (murs == 0) {
            return "il faut des murs";
        }
        if (libres == 0) {
            return "il faut des cases libres";
        }
        return null;
    }

    // ===================================================================
    // Partie A : affichage et deplacements
    // ===================================================================

    static void afficherGrille() {
        for (int l = 0; l < hauteur; l++) {
            StringBuilder ligne = new StringBuilder();
            for (int c = 0; c < largeur; c++) {
                ligne.append(symbole(l, c));
            }
            System.out.println(ligne);
        }
    }

    // Caractere a afficher pour une case (tient compte des objectifs)
    static char symbole(int l, int c) {
        char contenu = grille[l][c];
        if (contenu == '$') {
            return objectifs[l][c] ? '*' : '$';
        }
        if (contenu == '@') {
            return objectifs[l][c] ? '+' : '@';
        }
        if (contenu == ' ' && objectifs[l][c]) {
            return '.';
        }
        return contenu;
    }

    static boolean estDansGrille(int l, int c) {
        return l >= 0 && l < hauteur && c >= 0 && c < largeur;
    }

    // Une case hors de la grille est consideree comme un mur
    static boolean estMur(int l, int c) {
        return !estDansGrille(l, c) || grille[l][c] == '#';
    }

    // Le joueur peut-il avancer de (dl, dc) ?
    static boolean deplacementValide(int dl, int dc) {
        int l = joueurLigne + dl;
        int c = joueurColonne + dc;
        if (estMur(l, c)) {
            return false;
        }
        if (grille[l][c] == '$') {
            return peutPousser(l, c, dl, dc);
        }
        return true;
    }

    // La caisse en (l, c) peut-elle etre poussee de (dl, dc) ? La case derriere doit etre libre
    static boolean peutPousser(int l, int c, int dl, int dc) {
        int l2 = l + dl;
        int c2 = c + dc;
        return estDansGrille(l2, c2) && grille[l2][c2] == ' ';
    }

    static void deplacerCaisse(int l, int c, int dl, int dc) {
        grille[l + dl][c + dc] = '$';
        grille[l][c] = ' ';
    }

    // Deplace le joueur dans la direction donnee (0 a 3). Renvoie false si c'est impossible
    static boolean deplacerJoueur(int direction) {
        int dl = DL[direction];
        int dc = DC[direction];
        if (!deplacementValide(dl, dc)) {
            return false;
        }
        int l = joueurLigne + dl;
        int c = joueurColonne + dc;
        boolean poussee = (grille[l][c] == '$');
        if (poussee) {
            deplacerCaisse(l, c, dl, dc);
        }
        grille[joueurLigne][joueurColonne] = ' ';
        grille[l][c] = '@';
        joueurLigne = l;
        joueurColonne = c;
        nbMouvements++;
        return true;
    }

    // Renvoie l'indice de direction associe a une touche, ou -1
    static int directionDepuisTouche(char touche) {
        for (int i = 0; i < TOUCHES.length; i++) {
            if (TOUCHES[i] == touche) {
                return i;
            }
        }
        return -1;
    }

    // Partie B : verification de la victoire

    // Vrai si toutes les caisses sont sur des objectifs
    static boolean partieTerminee() {
        for (int l = 0; l < hauteur; l++) {
            for (int c = 0; c < largeur; c++) {
                if (grille[l][c] == '$' && !objectifs[l][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    // ===================================================================
    // Boucle de jeu
    // ===================================================================

    static void jouer() {
        while (true) {
            System.out.println();
            afficherGrille();
            System.out.println("Mouvements : " + nbMouvements);

            if (partieTerminee()) {
                System.out.println("Victoire !");
                System.out.println("Nombre de mouvements : " + nbMouvements);
                return;
            }
            System.out.print("Commande (z/q/s/d, x quitter) : ");
            String saisie = lireLigne();
            if (saisie == null) {
                return;
            }
            if (saisie.length() != 1) {
                System.out.println("Commande invalide : tapez une seule lettre.");
                continue;
            }

            char commande = saisie.charAt(0);
            if (commande == 'x') {
                System.out.println("Partie abandonnee.");
                return;
            } else {
                int direction = directionDepuisTouche(commande);
                if (direction == -1) {
                    System.out.println("Commande invalide : utilisez z, q, s, d ou x.");
                } else if (!deplacerJoueur(direction)) {
                    System.out.println("Deplacement impossible (mur ou caisse bloquee).");
                }
            }
        }
    }

}
