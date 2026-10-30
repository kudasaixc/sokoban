# Sokoban

Version console du jeu Sokoban en Java, avec un solveur automatique par recherche en profondeur (DFS) et en largeur (BFS). Projet Java ESIEA, PASS Java 2026-2027.

Projet réalisé par **kudasai**.

## Prérequis

- Java 11 ou plus récent (testé avec OpenJDK 21)
- Aucune bibliothèque externe

## Compiler et lancer

```sh
javac -d out src/Sokoban.java
java -cp out Sokoban
```

Sous Windows, les mêmes commandes fonctionnent dans un terminal (`cmd` ou PowerShell).

## Jouer

Le menu principal reprend celui du sujet :

```
================================
          SOKOBAN JAVA
================================
Niveau actuel : 1 - Premiers pas
1. Nouvelle partie
2. Choisir un niveau
3. Resoudre automatiquement
4. Comparer DFS / BFS
5. Afficher les regles
6. Quitter
```

Les commandes en jeu sont : `z` haut, `s` bas, `q` gauche, `d` droite et `x` quitter. Deux commandes bonus s'y ajoutent : `u` annule le dernier mouvement et `r` recommence le niveau.

Symboles : `#` mur, `@` joueur, `$` caisse, `.` objectif, `*` caisse sur un objectif, `+` joueur sur un objectif.

Les textes affichés par le programme n'ont volontairement pas d'accents, pour s'afficher correctement dans toutes les consoles, y compris celle de Windows.

## Fonctionnement

Tout le programme tient dans une seule classe, `src/Sokoban.java`, découpée en méthodes courtes :

| Rôle | Méthodes |
|---|---|
| Niveaux | `chargerNiveau`, `chargerGrille`, `verifierNiveau` |
| Jeu | `afficherGrille`, `deplacementValide`, `peutPousser`, `deplacerCaisse`, `deplacerJoueur`, `jouer` |
| Victoire | `partieTerminee` |
| Bonus | `annulerDernierCoup` (pile), `situationBloquee` / `estCoinMort` |
| Solveur | `resoudreDFS` + `dfs` (récursif), `resoudreBFS` (file), `etatSuivant`, `estVictoire` |
| États visités | `ajouterVisite` (table de hachage écrite à la main) |
| Comparaison | `comparerAlgorithmes` (mouvements, états explorés, temps) |

Comme le demande le sujet, aucune collection Java (`ArrayList`, `HashMap`, `HashSet`, `ArrayDeque`…) n'est utilisée. La file du BFS, la pile d'annulation et l'ensemble des états visités sont écrits avec de simples tableaux.

Le choix des algorithmes (représentation d'un état, cas de base et cas récursif du DFS, file du BFS, élagage, complexité) est expliqué en détail dans [`docs/algorithmes.md`](docs/algorithmes.md).

## Résultats de la comparaison

Mesures obtenues avec l'option 4 du menu (les temps varient selon la machine) :

| Niveau | DFS : mouvements | DFS : états | BFS : mouvements | BFS : états |
|---|---:|---:|---:|---:|
| 1 - Premiers pas | 1 | 2 | 1 | 2 |
| 2 - Deux caisses | 84 | 1 808 | 14 | 1 943 |
| 3 - Le couloir | 2 791 | 139 960 | 25 | 36 144 |
| 4 - L'entrepot | 291 | 1 056 | 45 | 26 265 |

BFS donne toujours la solution la plus courte. DFS peut explorer moins d'états, comme au niveau 4, mais il rend alors un chemin beaucoup plus long.

## Tests

```sh
./run-tests.sh
```

Le script compile le projet puis lance :

- `tests/SokobanTest.java` : 72 vérifications sans framework externe (validation des niveaux, murs, poussées, victoire, compteur, annulation, blocages, solutions DFS et BFS rejouées jusqu'à la victoire, optimalité du BFS vérifiée par une recherche exhaustive indépendante) ;
- 14 scénarios qui pilotent le programme réel par l'entrée standard (commandes invalides, menus, résolution, comparaison).

## Limites connues

- Le solveur s'arrête après 1 000 000 d'états explorés pour ne pas saturer la mémoire. Au-delà, il affiche « limite atteinte » sans conclure.
- Un état correspond à un pas du joueur, et pas seulement à une poussée de caisse. C'est simple à expliquer, mais l'espace d'états grandit vite sur les grandes grilles.
- La détection des blocages se limite aux coins sans objectif. Les blocages plus subtils (deux caisses collées contre un mur, par exemple) ne sont pas détectés.
- Le DFS récursif tourne dans un thread doté d'une pile de 512 Mo. Si la récursion dépasse malgré tout cette pile, l'erreur est interceptée et signalée comme une limite atteinte.
- La présentation (`presentation/`) a été construite à partir du sujet. Celui-ci ne donne aucune consigne précise sur sa forme, donc son plan suit les points que le sujet demande d'expliquer.
