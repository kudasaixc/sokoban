#!/bin/sh
# Compile le projet, lance les tests unitaires puis quelques scenarios console.
set -e
cd "$(dirname "$0")"

rm -rf out
javac -Xlint:all -d out src/Sokoban.java tests/SokobanTest.java
echo "Compilation : OK"
echo

java -cp out SokobanTest
echo

echec=0

# Lance le programme avec l'entree donnee et verifie que la sortie contient le texte attendu
scenario() {
    description="$1"
    entree="$2"
    attendu="$3"
    if printf "$entree" | java -cp out Sokoban | grep -q "$attendu"; then
        echo "OK     scenario : $description"
    else
        echo "ECHEC  scenario : $description"
        echec=1
    fi
}

scenario "victoire au niveau 1 avec z" "1\nz\n6\n" "Nombre de mouvements : 1"
scenario "commande inconnue refusee" "1\nw\nx\n6\n" "Commande invalide"
scenario "commande trop longue refusee" "1\nzz\nx\n6\n" "tapez une seule lettre"
scenario "mur infranchissable" "1\nq\nq\nq\nq\nx\n6\n" "Deplacement impossible"
scenario "choix de menu invalide" "9\n6\n" "Saisie invalide"
scenario "texte au lieu d'un nombre" "abc\n6\n" "Saisie invalide"
scenario "annulation sans mouvement" "1\nu\nx\n6\n" "Aucun mouvement a annuler"
scenario "redemarrage du niveau" "1\nd\nr\nx\n6\n" "Niveau recommence"
scenario "choix du niveau 3 puis abandon" "2\n3\nx\n6\n" "Partie abandonnee"
scenario "resolution BFS du niveau 2" "2\n2\nx\n3\n2\nn\n6\n" "Mouvements : 14"
scenario "solution rejouee pas a pas" "3\n2\no\n6\n" "Victoire"
scenario "comparaison DFS / BFS" "4\n6\n" "COMPARAISON"
scenario "regles affichees" "5\n6\n" "REGLES"
scenario "fin de l'entree standard geree" "1\n" "Au revoir"

if [ "$echec" -ne 0 ]; then
    exit 1
fi
echo
echo "Tous les scenarios sont passes."
