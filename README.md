# Sokoban

Jeu console Java correspondant au **niveau standard** du projet ESIEA PASS Java 2026-2027 (parties A, B et C, exigences de la section 12 du sujet).

Projet réalisé par **Azdine & Steeven**.

## Compiler et lancer

Java 11 ou plus récent, sans bibliothèque externe :

```sh
javac -d out src/Sokoban.java
java -cp out Sokoban
```

Ces commandes fonctionnent également dans un terminal Windows.

## Jouer

Le menu propose une nouvelle partie, le choix parmi quatre niveaux, les règles et la sortie.

- `z` : haut ; `s` : bas ; `q` : gauche ; `d` : droite.
- `x` : abandonner la partie et revenir au menu.
- Pousser une seule caisse à la fois, uniquement si la case derrière est libre.
- Placer toutes les caisses sur les objectifs pour gagner.

Symboles : `#` mur, `@` joueur, `$` caisse, `.` objectif, `*` caisse sur objectif, `+` joueur sur objectif.

Le compteur augmente uniquement pour un déplacement réussi. Les commandes incorrectes et les mouvements impossibles sont signalés. Les textes de la console utilisent des caractères sans accents.

## Organisation

Une seule classe principale : `src/Sokoban.java`. La grille utilise un tableau `char[][]`, les objectifs un tableau `boolean[][]`. Les niveaux sont stockés dans le programme. Les méthodes de chargement, d'affichage, de déplacement et de victoire sont expliquées dans [docs/algorithmes.md](docs/algorithmes.md).

## Vérification

```sh
./run-tests.sh
```

35 vérifications du moteur et 9 scénarios console : niveaux, saisies, murs, poussées, victoire et compteur. Aucune bibliothèque de test externe.

## Présentation

[PowerPoint du projet](presentation/Sokoban-presentation.pptx) : règles, exigences standard, tableaux, méthodes, démonstration et vérifications.
