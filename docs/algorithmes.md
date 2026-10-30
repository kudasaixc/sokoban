# Choix algorithmiques

Ce document explique comment fonctionne le solveur (parties D, E et F du sujet) et répond aux questions que le sujet pose sur le DFS et sur la comparaison des deux méthodes.

## 1. Représenter un état

Pour savoir où il en est, le solveur a besoin de connaître, pour chaque état :

| Information demandée par le sujet | Où elle se trouve |
|---|---|
| position du joueur | caractère `@` dans la chaîne d'état |
| position des caisses | caractères `$` dans la chaîne d'état |
| mouvements possibles | `etatSuivant(etat, direction)` renvoie `null` si le coup est impossible |
| états déjà explorés | table `tableVisites` (`ajouterVisite`) |
| victoire | `estVictoire(etat)` |

La grille est mise « à plat » : la case `(ligne, colonne)` reçoit l'indice `ligne * largeur + colonne`. Un état est une chaîne de `hauteur × largeur` caractères qui contient seulement `@`, `$` ou un espace. Les murs et les objectifs ne bougent jamais, donc ils sont stockés une seule fois, dans les tableaux `caseMur` et `caseObjectif`.

Avec ce choix, deux états identiques donnent la même chaîne, ce qui permet de les comparer directement avec `equals`.

Un état correspond à **un pas du joueur**. La longueur d'une solution est donc exactement le « nombre de mouvements » demandé par le sujet.

## 2. États déjà visités : une table de hachage écrite à la main

Le sujet interdit d'utiliser `HashMap` ou une structure équivalente à la place du travail algorithmique. L'ensemble des états visités est donc un simple tableau de chaînes, géré par **adressage ouvert** :

1. On calcule un indice à partir de la chaîne avec `h = 31 * h + caractère`, puis on prend le reste modulo la taille du tableau.
2. Si la case est occupée par un autre état, on essaie la case suivante (sondage linéaire).
3. On tombe soit sur l'état lui-même (déjà visité), soit sur une case vide (on l'y range).
4. Quand le tableau est à moitié plein, on double sa taille et on y replace tous les états.

Un ajout ou une recherche coûte en moyenne O(L), où L est la longueur de la chaîne. Une simple liste parcourue entièrement coûterait O(n × L) à chaque test : avec 36 000 états, la résolution deviendrait beaucoup trop lente.

## 3. Partie E : recherche en profondeur (DFS)

La méthode `dfs(etat, profondeur)` est récursive. Le tableau `cheminDFS` mémorise les touches jouées depuis le départ.

- **Cas de base 1 (état déjà visité).** Si `ajouterVisite` renvoie `false`, la méthode renvoie `false` tout de suite. C'est ce qui empêche de revisiter les mêmes états à l'infini, par exemple en faisant des allers-retours.
- **Cas de base 2 (victoire).** Si toutes les caisses sont sur des objectifs, on recopie `cheminDFS[0..profondeur-1]` dans la solution et on renvoie `true`.
- **Cas récursif.** Pour chaque direction (z, s, q, d), on calcule l'état suivant. S'il existe, on note la touche dans `cheminDFS[profondeur]` et on appelle `dfs(suivant, profondeur + 1)`. Si cet appel réussit, on remonte `true` immédiatement.
- **Retour arrière.** Si aucune direction ne mène à la victoire, on renvoie `false`. L'appelant essaie alors sa direction suivante.
- **Condition d'arrêt.** La recherche s'arrête dans trois cas : une solution est trouvée, tous les états atteignables ont été visités (pas de solution), ou la limite de 1 000 000 d'états est atteinte (sécurité).

Le nombre d'états est fini et chacun n'est visité qu'une fois, donc le DFS se termine toujours. S'il existe une solution, il finit par la trouver.

**Avantages**
- Il est court à écrire et naturel à exprimer en récursif.
- Il garde en mémoire seulement le chemin courant, plus l'ensemble des états visités.
- Il trouve parfois une solution très vite. Au niveau 4, il lui suffit de 1 056 états, contre 26 265 pour BFS.

**Limites**
- La solution trouvée n'est presque jamais la plus courte (2 791 coups au niveau 3, contre 25 pour BFS).
- La profondeur de récursion peut devenir très grande. Le programme tourne donc dans un thread avec une pile de 512 Mo, et un éventuel `StackOverflowError` est intercepté.
- Le résultat dépend de l'ordre dans lequel les directions sont essayées.

## 4. Partie F : recherche en largeur (BFS)

BFS utilise une **file** (premier entré, premier sorti) écrite avec trois tableaux parallèles :

```
fileEtats[i]   : l'etat numero i
fileParents[i] : indice de l'etat d'ou l'on vient (-1 pour le depart)
fileCoups[i]   : touche jouee pour arriver en i

tete : prochain etat a defiler
fin  : premiere place libre (on enfile en fin)
```

1. On enfile l'état initial et on le marque comme visité.
2. Tant que `tete < fin`, on défile `fileEtats[tete]`. Si c'est une victoire, on reconstruit le chemin en remontant les parents.
3. Sinon, chaque successeur jamais vu est marqué comme visité et ajouté en fin de file.

Les états sortent de la file dans l'ordre où ils y sont entrés, c'est-à-dire **par distance croissante depuis l'état initial**. Le premier état gagnant défilé est donc à la distance minimale, et la solution est **la plus courte en nombre de mouvements** (bonus « solution minimisant le nombre de mouvements »). Les tests le confirment au niveau 2 : une recherche exhaustive indépendante vérifie qu'aucune suite de 13 coups ou moins ne gagne, alors que BFS trouve une solution de 14 coups.

Les états déjà traités ne sont pas effacés de la file, car ils servent à reconstruire le chemin. C'est pourquoi BFS consomme plus de mémoire que DFS.

## 5. Pourquoi DFS et BFS donnent des résultats différents

Les deux algorithmes parcourent le même graphe d'états, mais **pas dans le même ordre** :

- DFS part dans une direction et s'enfonce aussi loin que possible avant de revenir en arrière. Le premier chemin gagnant qu'il rencontre est rarement le plus court : c'est le premier que l'ordre z, s, q, d lui fait découvrir.
- BFS visite d'abord tous les états à 1 coup, puis à 2 coups, et ainsi de suite. Il trouve forcément le chemin le plus court, mais il doit parcourir tous les états plus proches que la solution.

Le nombre d'états explorés n'a donc pas de règle fixe. Au niveau 3, DFS explore 139 960 états car il se perd dans des branches inutiles. Au niveau 4, il tombe vite sur une solution, longue mais valide.

| Niveau | DFS : coups | DFS : états | BFS : coups | BFS : états |
|---|---:|---:|---:|---:|
| 1 | 1 | 2 | 1 | 2 |
| 2 | 84 | 1 808 | 14 | 1 943 |
| 3 | 2 791 | 139 960 | 25 | 36 144 |
| 4 | 291 | 1 056 | 45 | 26 265 |

## 6. Optimisation : élagage des coins morts (bonus)

Une caisse poussée dans un coin (un mur vertical et un mur horizontal autour d'elle) qui n'est pas un objectif ne pourra plus jamais bouger. Ces cases sont calculées une fois pour toutes (`caseMorte`), et `etatSuivant` refuse toute poussée vers elles.

Une telle poussée ne peut jamais mener à une victoire. Cet élagage ne fait donc perdre aucune solution, et BFS reste optimal (les longueurs de solution sont identiques avec ou sans élagage). Nombre d'états explorés par BFS :

| Niveau | sans élagage | avec élagage | gain |
|---|---:|---:|---:|
| 2 | 2 635 | 1 943 | −26 % |
| 3 | 52 525 | 36 144 | −31 % |
| 4 | 51 346 | 26 265 | −49 % |

La même fonction sert pendant une partie : quand le joueur coince une caisse, `situationBloquee()` affiche un avertissement.

## 7. Complexité

Notons N le nombre d'états atteignables et L = hauteur × largeur la taille d'un état.

- Chaque état est généré, haché et comparé en O(L). Il a au plus 4 successeurs.
- **Temps** : O(N × L) pour DFS comme pour BFS, puisque chaque état est traité au plus une fois grâce à la table des visités.
- **Mémoire** : O(N × L) pour la table des visités. BFS y ajoute sa file (O(N) références), DFS sa pile d'appels (O(profondeur)).
- N croît de façon exponentielle avec le nombre de caisses. Avec C cases libres et k caisses, on peut avoir jusqu'à environ C × (nombre de façons de choisir k cases parmi C) états. C'est pour cela qu'une limite de 1 000 000 d'états protège le programme.
