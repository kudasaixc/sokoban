import java.util.Scanner;

/*
 * Projet Java - Sokoban : jeu et resolution algorithmique
 *
 * Programme console en une seule classe :
 *  - partie A/B/C : jeu au clavier, detection de la victoire, plusieurs niveaux ;
 *  - partie D/E/F : solveur automatique par DFS (recursif) et BFS (file) ;
 *  - comparaison des deux methodes.
 *
 * Aucune collection Java (ArrayList, HashMap, ...) n'est utilisee :
 * la file, la pile et l'ensemble des etats visites sont codes avec des tableaux.
 */
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

    // Nombre maximal d'etats explores par un solveur avant abandon
    static final int LIMITE_ETATS = 1000000;

    // Taille de la pile d'appels du programme (le DFS recursif peut etre profond)
    static final long TAILLE_PILE = 512L * 1024 * 1024;

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

    // Pile des coups joues (pour l'annulation) : code = direction * 2 + (1 si poussee)
    static int[] pileCoups = new int[64];
    static int nbCoups = 0;

    static Scanner clavier = new Scanner(System.in);

    // ===================================================================
    // Programme principal et menus
    // ===================================================================

    public static void main(String[] args) throws InterruptedException {
        // Le DFS est recursif : on lance tout le programme dans un thread
        // qui dispose d'une grande pile d'appels pour eviter un StackOverflowError
        Thread programme = new Thread(null, Sokoban::menuPrincipal, "sokoban", TAILLE_PILE);
        programme.start();
        programme.join();
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
            System.out.println("3. Resoudre automatiquement");
            System.out.println("4. Comparer DFS / BFS");
            System.out.println("5. Afficher les regles");
            System.out.println("6. Quitter");
            System.out.print("Votre choix : ");

            int choix = lireChoix(1, 6);
            if (choix == -1 || choix == 6) {
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
            } else if (choix == 3) {
                menuResolution();
            } else if (choix == 4) {
                comparerAlgorithmes();
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
        System.out.println("Bonus     : u annuler le dernier mouvement, r recommencer le niveau");
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
        nbCoups = 0;
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
        empilerCoup(direction * 2 + (poussee ? 1 : 0));
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

    // ===================================================================
    // Bonus : pile des coups pour annuler le dernier mouvement
    // ===================================================================

    static void empilerCoup(int code) {
        if (nbCoups == pileCoups.length) {
            int[] plusGrand = new int[pileCoups.length * 2];
            for (int i = 0; i < nbCoups; i++) {
                plusGrand[i] = pileCoups[i];
            }
            pileCoups = plusGrand;
        }
        pileCoups[nbCoups] = code;
        nbCoups++;
    }

    // Annule le dernier coup (depile). Renvoie false s'il n'y a rien a annuler
    static boolean annulerDernierCoup() {
        if (nbCoups == 0) {
            return false;
        }
        nbCoups--;
        int code = pileCoups[nbCoups];
        int direction = code / 2;
        boolean poussee = (code % 2 == 1);
        int dl = DL[direction];
        int dc = DC[direction];

        int ancienneLigne = joueurLigne - dl;
        int ancienneColonne = joueurColonne - dc;
        grille[joueurLigne][joueurColonne] = ' ';
        if (poussee) {
            // la caisse revient sur la case que le joueur occupait
            grille[joueurLigne + dl][joueurColonne + dc] = ' ';
            grille[joueurLigne][joueurColonne] = '$';
        }
        grille[ancienneLigne][ancienneColonne] = '@';
        joueurLigne = ancienneLigne;
        joueurColonne = ancienneColonne;
        nbMouvements--;
        return true;
    }

    // ===================================================================
    // Partie B : victoire, et bonus : situations bloquees
    // ===================================================================

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

    // Une case est un "coin mort" si elle n'est pas un objectif et touche
    // un mur vertical ET un mur horizontal : une caisse poussee la ne bouge plus
    static boolean estCoinMort(int l, int c) {
        if (estMur(l, c) || objectifs[l][c]) {
            return false;
        }
        boolean murVertical = estMur(l - 1, c) || estMur(l + 1, c);
        boolean murHorizontal = estMur(l, c - 1) || estMur(l, c + 1);
        return murVertical && murHorizontal;
    }

    // Vrai si une caisse est coincee dans un coin mort (la partie est perdue)
    static boolean situationBloquee() {
        for (int l = 0; l < hauteur; l++) {
            for (int c = 0; c < largeur; c++) {
                if (grille[l][c] == '$' && estCoinMort(l, c)) {
                    return true;
                }
            }
        }
        return false;
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
            if (situationBloquee()) {
                System.out.println("Attention : une caisse est bloquee dans un coin, le niveau ne peut plus etre gagne.");
                System.out.println("Utilisez u pour annuler ou r pour recommencer.");
            }

            System.out.print("Commande (z/q/s/d, u annuler, r recommencer, x quitter) : ");
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
            } else if (commande == 'u') {
                if (!annulerDernierCoup()) {
                    System.out.println("Aucun mouvement a annuler.");
                }
            } else if (commande == 'r') {
                chargerNiveau(niveauCourant);
                System.out.println("Niveau recommence.");
            } else {
                int direction = directionDepuisTouche(commande);
                if (direction == -1) {
                    System.out.println("Commande invalide : utilisez z, q, s, d, u, r ou x.");
                } else if (!deplacerJoueur(direction)) {
                    System.out.println("Deplacement impossible (mur ou caisse bloquee).");
                }
            }
        }
    }

    // ===================================================================
    // Partie D : representation d'un etat pour le solveur
    //
    // La grille est mise "a plat" : la case (l, c) a l'indice l * largeur + c.
    // Un etat est une chaine de hauteur * largeur caracteres :
    // '@' pour le joueur, '$' pour une caisse, ' ' sinon.
    // Les murs et les objectifs ne bougent jamais : ils sont stockes une seule
    // fois dans les tableaux ci-dessous.
    // ===================================================================

    static boolean[] caseMur;
    static boolean[] caseObjectif;
    static boolean[] caseMorte;
    static int nbCases;

    // Statistiques du dernier appel a un solveur
    static int etatsExplores;
    static boolean limiteAtteinte;

    // Recharge le niveau courant et prepare les tableaux fixes du solveur
    static String preparerSolveur() {
        chargerNiveau(niveauCourant);
        nbCases = hauteur * largeur;
        caseMur = new boolean[nbCases];
        caseObjectif = new boolean[nbCases];
        caseMorte = new boolean[nbCases];
        char[] depart = new char[nbCases];
        for (int l = 0; l < hauteur; l++) {
            for (int c = 0; c < largeur; c++) {
                int i = l * largeur + c;
                caseMur[i] = (grille[l][c] == '#');
                caseObjectif[i] = objectifs[l][c];
                caseMorte[i] = estCoinMort(l, c);
                depart[i] = (grille[l][c] == '#') ? ' ' : grille[l][c];
            }
        }
        etatsExplores = 0;
        limiteAtteinte = false;
        viderVisites();
        return new String(depart);
    }

    // Etat obtenu en jouant la direction donnee, ou null si le coup est impossible
    static String etatSuivant(String etat, int direction) {
        int joueur = etat.indexOf('@');
        int l = joueur / largeur + DL[direction];
        int c = joueur % largeur + DC[direction];
        if (!estDansGrille(l, c) || caseMur[l * largeur + c]) {
            return null;
        }
        int cible = l * largeur + c;
        char[] cases = etat.toCharArray();

        if (cases[cible] == '$') {
            int l2 = l + DL[direction];
            int c2 = c + DC[direction];
            if (!estDansGrille(l2, c2)) {
                return null;
            }
            int derriere = l2 * largeur + c2;
            if (caseMur[derriere] || cases[derriere] == '$') {
                return null;
            }
            // Optimisation : inutile de pousser une caisse dans un coin mort
            if (caseMorte[derriere]) {
                return null;
            }
            cases[derriere] = '$';
        }
        cases[joueur] = ' ';
        cases[cible] = '@';
        return new String(cases);
    }

    // Vrai si toutes les caisses de l'etat sont sur des objectifs
    static boolean estVictoire(String etat) {
        for (int i = 0; i < nbCases; i++) {
            if (etat.charAt(i) == '$' && !caseObjectif[i]) {
                return false;
            }
        }
        return true;
    }

    // ===================================================================
    // Ensemble des etats deja visites : table de hachage maison
    // (adressage ouvert avec sondage lineaire, agrandie quand elle est a moitie pleine)
    // ===================================================================

    static String[] tableVisites;
    static int nbVisites;

    static void viderVisites() {
        tableVisites = new String[1024];
        nbVisites = 0;
    }

    static int hachage(String etat, int taille) {
        int h = 0;
        for (int i = 0; i < etat.length(); i++) {
            h = 31 * h + etat.charAt(i);
        }
        return (h & 0x7fffffff) % taille;
    }

    // Ajoute l'etat s'il est nouveau. Renvoie false s'il avait deja ete visite
    static boolean ajouterVisite(String etat) {
        if (2 * (nbVisites + 1) > tableVisites.length) {
            agrandirVisites();
        }
        int i = hachage(etat, tableVisites.length);
        while (tableVisites[i] != null) {
            if (tableVisites[i].equals(etat)) {
                return false;
            }
            i = (i + 1) % tableVisites.length;
        }
        tableVisites[i] = etat;
        nbVisites++;
        return true;
    }

    // Double la taille de la table et y replace tous les etats
    static void agrandirVisites() {
        String[] ancienne = tableVisites;
        tableVisites = new String[ancienne.length * 2];
        for (int k = 0; k < ancienne.length; k++) {
            if (ancienne[k] != null) {
                int i = hachage(ancienne[k], tableVisites.length);
                while (tableVisites[i] != null) {
                    i = (i + 1) % tableVisites.length;
                }
                tableVisites[i] = ancienne[k];
            }
        }
    }

    // ===================================================================
    // Partie E : recherche en profondeur (DFS recursif)
    // ===================================================================

    static char[] cheminDFS;
    static char[] solutionDFS;

    // Renvoie la suite de touches d'une solution, ou null si aucune n'a ete trouvee
    static char[] resoudreDFS() {
        String depart = preparerSolveur();
        cheminDFS = new char[1024];
        solutionDFS = null;
        try {
            dfs(depart, 0);
        } catch (StackOverflowError e) {
            // securite : recursion trop profonde malgre la grande pile
            limiteAtteinte = true;
        }
        return solutionDFS;
    }

    // Explore l'etat puis, recursivement, ses successeurs.
    // profondeur = nombre de coups deja joues depuis le depart (stockes dans cheminDFS)
    static boolean dfs(String etat, int profondeur) {
        // Cas de base 1 : etat deja visite, on ne le traite pas une seconde fois
        if (!ajouterVisite(etat)) {
            return false;
        }
        etatsExplores++;

        // Cas de base 2 : victoire, on recopie le chemin courant
        if (estVictoire(etat)) {
            solutionDFS = new char[profondeur];
            for (int i = 0; i < profondeur; i++) {
                solutionDFS[i] = cheminDFS[i];
            }
            return true;
        }

        // Condition d'arret de securite : trop d'etats explores
        if (etatsExplores >= LIMITE_ETATS) {
            limiteAtteinte = true;
            return false;
        }

        // Cas recursif : on essaie chaque direction, on s'arrete des qu'une branche reussit
        for (int direction = 0; direction < 4; direction++) {
            String suivant = etatSuivant(etat, direction);
            if (suivant != null) {
                if (profondeur == cheminDFS.length) {
                    agrandirCheminDFS();
                }
                cheminDFS[profondeur] = TOUCHES[direction];
                if (dfs(suivant, profondeur + 1)) {
                    return true;
                }
                if (limiteAtteinte) {
                    return false;
                }
            }
        }
        // Aucune direction ne mene a la victoire depuis cet etat : retour arriere
        return false;
    }

    static void agrandirCheminDFS() {
        char[] plusGrand = new char[cheminDFS.length * 2];
        for (int i = 0; i < cheminDFS.length; i++) {
            plusGrand[i] = cheminDFS[i];
        }
        cheminDFS = plusGrand;
    }

    // ===================================================================
    // Partie F : recherche en largeur (BFS avec une file)
    //
    // La file est faite de trois tableaux paralleles : l'etat, l'indice de
    // son parent et la touche jouee pour y arriver. "tete" designe le prochain
    // etat a traiter, "fin" la premiere place libre. Les etats deja traites
    // restent dans le tableau car ils servent a reconstruire le chemin.
    // ===================================================================

    static String[] fileEtats;
    static int[] fileParents;
    static char[] fileCoups;

    static char[] resoudreBFS() {
        String depart = preparerSolveur();
        fileEtats = new String[1024];
        fileParents = new int[1024];
        fileCoups = new char[1024];
        int tete = 0;
        int fin = 0;

        // Enfiler l'etat initial
        fileEtats[fin] = depart;
        fileParents[fin] = -1;
        fin++;
        ajouterVisite(depart);

        while (tete < fin) {
            // Defiler : les etats sortent dans l'ordre ou ils sont entres
            int indice = tete;
            String etat = fileEtats[tete];
            tete++;
            etatsExplores++;

            if (estVictoire(etat)) {
                return reconstruireChemin(indice);
            }
            if (etatsExplores >= LIMITE_ETATS) {
                limiteAtteinte = true;
                return null;
            }

            for (int direction = 0; direction < 4; direction++) {
                String suivant = etatSuivant(etat, direction);
                // On marque l'etat comme visite des qu'il entre dans la file
                if (suivant != null && ajouterVisite(suivant)) {
                    if (fin == fileEtats.length) {
                        agrandirFile();
                    }
                    fileEtats[fin] = suivant;
                    fileParents[fin] = indice;
                    fileCoups[fin] = TOUCHES[direction];
                    fin++;
                }
            }
        }
        // File vide : tous les etats atteignables ont ete vus, pas de solution
        return null;
    }

    static void agrandirFile() {
        int taille = fileEtats.length * 2;
        String[] etats = new String[taille];
        int[] parents = new int[taille];
        char[] coups = new char[taille];
        for (int i = 0; i < fileEtats.length; i++) {
            etats[i] = fileEtats[i];
            parents[i] = fileParents[i];
            coups[i] = fileCoups[i];
        }
        fileEtats = etats;
        fileParents = parents;
        fileCoups = coups;
    }

    // Remonte les parents depuis l'etat gagnant jusqu'au depart
    static char[] reconstruireChemin(int indice) {
        int longueur = 0;
        for (int i = indice; fileParents[i] != -1; i = fileParents[i]) {
            longueur++;
        }
        char[] chemin = new char[longueur];
        int position = longueur - 1;
        for (int i = indice; fileParents[i] != -1; i = fileParents[i]) {
            chemin[position] = fileCoups[i];
            position--;
        }
        return chemin;
    }

    // ===================================================================
    // Resolution automatique et comparaison
    // ===================================================================

    static void menuResolution() {
        System.out.println();
        System.out.println("Resolution du niveau " + (niveauCourant + 1) + " - " + NOMS_NIVEAUX[niveauCourant]);
        System.out.println("1. Recherche en profondeur (DFS)");
        System.out.println("2. Recherche en largeur (BFS, solution la plus courte)");
        System.out.println("3. Retour");
        System.out.print("Votre choix : ");
        int choix = lireChoix(1, 3);
        if (choix <= 0 || choix == 3) {
            return;
        }

        long debut = System.nanoTime();
        char[] solution = (choix == 1) ? resoudreDFS() : resoudreBFS();
        long duree = System.nanoTime() - debut;

        System.out.println();
        System.out.println(choix == 1 ? "DFS" : "BFS");
        afficherResultat(solution, duree);
        if (solution == null) {
            return;
        }
        System.out.println("Solution : " + formaterSolution(solution));
        System.out.print("Afficher la solution pas a pas ? (o/n) : ");
        String reponse = lireLigne();
        if (reponse != null && reponse.equals("o")) {
            rejouerSolution(solution);
        }
    }

    static void comparerAlgorithmes() {
        long debut = System.nanoTime();
        char[] solutionDFS = resoudreDFS();
        long dureeDFS = System.nanoTime() - debut;
        int etatsDFS = etatsExplores;

        debut = System.nanoTime();
        char[] solutionBFS = resoudreBFS();
        long dureeBFS = System.nanoTime() - debut;
        int etatsBFS = etatsExplores;

        System.out.println();
        System.out.println("Niveau " + (niveauCourant + 1) + " - " + NOMS_NIVEAUX[niveauCourant]);
        System.out.println("========== COMPARAISON ==========");
        System.out.println("DFS");
        afficherStatistiques(solutionDFS, etatsDFS, dureeDFS);
        System.out.println("BFS");
        afficherStatistiques(solutionBFS, etatsBFS, dureeBFS);
        System.out.println("=================================");
        System.out.println("BFS explore les etats par distance croissante : sa solution est la plus courte.");
        System.out.println("DFS suit une branche jusqu'au bout : sa solution est souvent plus longue.");
    }

    // Affiche le resultat du dernier solveur appele
    static void afficherResultat(char[] solution, long dureeNano) {
        afficherStatistiques(solution, etatsExplores, dureeNano);
    }

    static void afficherStatistiques(char[] solution, int etats, long dureeNano) {
        if (solution != null) {
            System.out.println("Solution trouvee : oui");
            System.out.println("Mouvements : " + solution.length);
        } else if (limiteAtteinte) {
            System.out.println("Solution trouvee : non (limite de " + LIMITE_ETATS + " etats atteinte)");
        } else {
            System.out.println("Solution trouvee : non (aucune solution n'existe)");
        }
        System.out.println("Etats explores : " + etats);
        System.out.printf("Temps : %.2f ms%n", dureeNano / 1000000.0);
    }

    // Met la solution en forme par groupes de 10 touches
    static String formaterSolution(char[] solution) {
        StringBuilder texte = new StringBuilder();
        for (int i = 0; i < solution.length; i++) {
            if (i > 0 && i % 10 == 0) {
                texte.append(' ');
            }
            texte.append(solution[i]);
        }
        return texte.toString();
    }

    // Rejoue la solution sur la grille du niveau en affichant chaque etape
    static void rejouerSolution(char[] solution) {
        chargerNiveau(niveauCourant);
        System.out.println();
        afficherGrille();
        for (int i = 0; i < solution.length; i++) {
            deplacerJoueur(directionDepuisTouche(solution[i]));
            System.out.println();
            System.out.println("Mouvement " + (i + 1) + " / " + solution.length + " : " + solution[i]);
            afficherGrille();
        }
        if (partieTerminee()) {
            System.out.println("Victoire !");
            System.out.println("Nombre de mouvements : " + nbMouvements);
        }
    }
}
