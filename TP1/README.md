# PATIA – Partie 1 : N-Puzzle

## But du projet

Dans cette partie, on a travaillé sur le taquin (`n-puzzle`).

Le but était de :
- résoudre le taquin avec trois méthodes de recherche :
  - BFS
  - DFS
  - A*
- générer automatiquement des instances de taquins
- comparer les performances des méthodes avec un graphe

## Contenu du dossier

- `npuzzle.py` : contient les fonctions de base pour manipuler le taquin
- `node.py` : contient la structure de nœud utilisée par les algorithmes
- `solve_npuzzle.py` : permet de résoudre un puzzle avec BFS, DFS ou A*
- `generate_npuzzle.py` : permet de générer des puzzles aléatoires
- `benchmark_npuzzle.py` : permet de mesurer les temps et de générer le graphe
- `instances_v3/` : contient le jeu d’instances final utilisé pour le benchmark

## Encodage choisi

Un état du taquin est représenté par une liste d’entiers.

Exemple pour un taquin 3x3 :

`[0, 1, 2, 3, 4, 5, 6, 7, 8]`

- chaque entier correspond à une case
- `0` représente la case vide
- l’état but est la liste ordonnée
- les mouvements possibles sont `up`, `down`, `left`, `right`

On a choisi cet encodage parce qu’il est simple à utiliser dans le code.

## Algorithmes utilisés

### BFS
BFS explore les états niveau par niveau.

Cette méthode permet de trouver une solution minimale.  
Dans notre projet, on l’utilise aussi comme référence pour classer les puzzles du plus simple au plus difficile.

### DFS
DFS explore d’abord en profondeur.

Cette méthode peut être rapide sur certains cas, mais elle est très irrégulière.  
Elle ne garantit pas de trouver la solution la plus courte.

### A*
A* utilise une heuristique pour guider la recherche.

Ici, on utilise la distance de Manhattan.  
C’est en général la méthode la plus efficace sur les puzzles difficiles.

## Génération des instances

Les puzzles sont générés à partir de l’état but, puis on applique des mouvements aléatoires légaux.

Le fichier `generate_npuzzle.py` permet de choisir :
- la taille du puzzle
- la longueur maximale de génération
- le nombre d’instances par longueur
- le dossier de sortie

Exemple utilisé pour notre jeu final :

`python3 generate_npuzzle.py -s 3 -ml 20 -n 3 instances_v3`

Cette commande génère :
- des puzzles 3x3
- pour des longueurs de 1 à 20
- avec 3 puzzles par longueur

## Résoudre un puzzle

Depuis le dossier `n-puzzle`, on peut lancer :

### Avec BFS
`python3 solve_npuzzle.py instances_v3/npuzzle_3x3_len20_0.txt -a bfs`

### Avec DFS
`python3 solve_npuzzle.py instances_v3/npuzzle_3x3_len20_0.txt -a dfs`

### Avec A*
`python3 solve_npuzzle.py instances_v3/npuzzle_3x3_len20_0.txt -a astar`

### Mode verbeux
`python3 solve_npuzzle.py instances_v3/npuzzle_3x3_len20_0.txt -a astar -v`

## Génération du graphe

Le benchmark doit être lancé depuis le dossier contenant les instances à tester.

Exemple avec notre jeu final :

1. se placer dans le bon dossier  
`cd instances_v3`

2. lancer le benchmark  
`python3 ../benchmark_npuzzle.py`

Le script :
- charge tous les fichiers `npuzzle_*.txt`
- lance BFS, DFS et A*
- mesure les temps
- trie les instances selon la difficulté mesurée par BFS
- génère le graphe final `benchmark_npuzzle_all.png`

## Ce qu’on observe sur le graphe

Sur le graphe final, on voit que :
- BFS devient plus lent quand les puzzles deviennent plus difficiles
- A* reste globalement plus rapide que BFS sur les cas difficiles
- DFS est très instable : parfois rapide, parfois très lent

C’est ce qu’on attendait globalement.

## Dépendances

Le projet utilise :
- Python 3
- `matplotlib` pour générer le graphe

Si besoin :

`pip install matplotlib`

## Jeu d’instances retenu

Pour le rendu, on a gardé le dossier `instances_v3`, car c’est celui qui donne les résultats les plus propres.

Le fichier principal à regarder pour la partie 1 est :

`instances_v3/benchmark_npuzzle_all.png`
