# Fonctionnement du jeu standard

## Représentation

`grille` est un tableau de caractères à deux dimensions contenant les murs, les caisses et le joueur. `objectifs` est un tableau de booléens de mêmes dimensions. Séparer les objectifs permet de les conserver quand le joueur ou une caisse quitte leur case. `symbole` combine ces informations pour afficher `.`, `*` ou `+`.

## Chargement

`verifierNiveau` vérifie les caractères, la présence d'un seul joueur, de murs et de cases libres, ainsi qu'un nombre égal et non nul de caisses et d'objectifs. `chargerNiveau` sélectionne un niveau et `chargerGrille` construit les tableaux et remet le compteur à zéro.

## Déplacement

`directionDepuisTouche` traduit z/s/q/d en direction. `deplacementValide` refuse les murs et les sorties de grille. Si la case suivante contient une caisse, `peutPousser` vérifie que la case derrière est libre. `deplacerCaisse` puis `deplacerJoueur` actualisent les positions. Un mouvement réussi augmente le compteur d'une unité ; un refus ne le modifie pas.

## Victoire et boucle de jeu

`partieTerminee` parcourt les cases : si une caisse n'est pas sur un objectif, la partie continue. Sinon, toutes les caisses sont placées et la victoire est affichée avec le nombre de mouvements.

`jouer` affiche la grille, vérifie la victoire, lit et valide une commande, puis tente le déplacement. `x` revient au menu. Une fin d'entrée termine proprement le programme.

## Coût des opérations

Pour une grille de H lignes et L colonnes, affichage, chargement et vérification de victoire coûtent O(H × L). Un déplacement coûte O(1). Les deux tableaux occupent O(H × L) cases mémoire.
