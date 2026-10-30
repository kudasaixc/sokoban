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

scenario "victoire au niveau 1 avec z" "1\nz\n4\n" "Nombre de mouvements : 1"
scenario "commande inconnue refusee" "1\nw\nx\n4\n" "Commande invalide"
scenario "commande trop longue refusee" "1\nzz\nx\n4\n" "tapez une seule lettre"
scenario "mur infranchissable" "1\nq\nq\nq\nq\nx\n4\n" "Deplacement impossible"
scenario "choix de menu invalide" "9\n4\n" "Saisie invalide"
scenario "texte au lieu d'un nombre" "abc\n4\n" "Saisie invalide"
scenario "choix du niveau 3 puis abandon" "2\n3\nx\n4\n" "Partie abandonnee"
scenario "regles affichees" "3\n4\n" "REGLES"
scenario "fin de l'entree standard geree" "1\n" "Au revoir"

if [ "$echec" -ne 0 ]; then
    exit 1
fi
echo
echo "Tous les scenarios sont passes."
