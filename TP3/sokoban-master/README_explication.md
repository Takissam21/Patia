# Sokoban avec PDDL4J

Ce projet implémente une résolution automatique de niveaux Sokoban avec un planificateur PDDL.

Le but est de lire un niveau Sokoban, de le transformer en problème PDDL, de résoudre ce problème avec PDDL4J, puis d’exécuter le plan obtenu dans l’interface web du jeu.

## Fichiers importants

Les fichiers principaux sont :

- `src/main/java/sokoban/Agent.java` : agent qui lit l’état du jeu, génère le problème PDDL, lance le planificateur et joue les actions.
- `src/main/java/sokoban/MakePddl.java` : fichier que nous avons ajouté pour générer automatiquement un problème PDDL à partir d’un niveau Sokoban.
- `pddl/sokoban/domain.pddl` : domaine PDDL du Sokoban.
- `pddl/sokoban/from_agent.pddl` : problème PDDL généré automatiquement par l’agent.
- `config/test*.json` : niveaux Sokoban fournis.

## Principe général

Au premier tour, l’agent reçoit les informations du niveau :

- la largeur et la hauteur de la carte ;
- le nombre de caisses ;
- la carte du niveau ;
- la position du joueur ;
- les positions des caisses.

À partir de ces informations, notre fichier `MakePddl.java` construit un problème PDDL.

Ensuite, l’agent résout ce problème avec PDDL4J. Le plan obtenu est transformé en mouvements simples pour le jeu :

- les actions contenant `up` deviennent `U` ;
- les actions contenant `down` deviennent `D` ;
- les actions contenant `left` deviennent `L` ;
- les actions contenant `right` deviennent `R`.

L’agent envoie ensuite un mouvement à chaque tour au moteur Sokoban.

## Encodage PDDL du Sokoban

Dans notre encodage, chaque case accessible du niveau est représentée par un objet de type `cell`.

Chaque caisse est représentée par un objet de type `box`.

Les murs ne sont pas représentés comme des objets. On les ignore simplement dans les relations de voisinage. Donc si une case est un mur, elle n’apparaît pas comme case accessible dans le problème PDDL.

Les prédicats principaux utilisés dans le domaine sont :

- `player-at` : indique la position actuelle du joueur ;
- `box-at` : indique la position d’une caisse ;
- `clear` : indique qu’une case est libre ;
- `goal` : indique qu’une case est une destination ;
- `filled` : indique qu’une destination est occupée par une caisse ;
- `deadlock` : indique une case dangereuse où il ne faut pas pousser une caisse ;
- `up`, `down`, `left`, `right` : indiquent les relations de voisinage entre les cases.

Le but du problème est que toutes les destinations soient remplies, c’est-à-dire que chaque case objectif vérifie le prédicat `filled`.

## Domaine PDDL

Notre domaine PDDL contient deux grandes familles d’actions.

### Déplacements simples

Les actions `move-up`, `move-down`, `move-left` et `move-right` permettent de déplacer le joueur sans pousser de caisse.

Pour faire un déplacement simple, il faut :

- que le joueur soit sur la case de départ ;
- que la case d’arrivée soit voisine dans la bonne direction ;
- que la case d’arrivée soit libre.

Après l’action, le joueur est déplacé sur la nouvelle case.

### Poussées de caisses

Les actions `push-up`, `push-down`, `push-left` et `push-right` permettent de pousser une caisse.

Pour pousser une caisse, il faut :

- que le joueur soit derrière la caisse ;
- que la caisse soit dans la direction du mouvement ;
- que la case après la caisse soit libre ;
- que la case d’arrivée de la caisse ne soit pas une case deadlock.

Après l’action :

- le joueur prend l’ancienne position de la caisse ;
- la caisse avance d’une case ;
- l’ancienne position du joueur devient libre ;
- la nouvelle position de la caisse n’est plus libre.

Nous avons aussi séparé les actions de poussée selon les cas liés aux objectifs :

- poussée normale ;
- poussée vers un objectif ;
- poussée depuis un objectif ;
- poussée d’un objectif vers un autre objectif.

Cette séparation permet de bien mettre à jour le prédicat `filled`.

Par exemple, si une caisse est poussée vers une case objectif, on ajoute `filled` sur cette case. Si une caisse quitte une case objectif, on enlève `filled` de cette case.

## Gestion des deadlocks

Nous avons ajouté une détection simple de deadlocks dans `MakePddl.java`.

L’idée est d’éviter de pousser une caisse dans une case où elle serait définitivement bloquée.

Pour cela, pendant la génération du problème PDDL, on repère les cases qui sont des coins et qui ne sont pas des objectifs.

Une case est considérée comme un deadlock si elle est bloquée par deux murs perpendiculaires, par exemple :

- mur au-dessus et mur à gauche ;
- mur au-dessus et mur à droite ;
- mur en dessous et mur à gauche ;
- mur en dessous et mur à droite.

Si cette case n’est pas un objectif, alors pousser une caisse dessus rendrait souvent le niveau impossible à résoudre.

Ces cases sont donc ajoutées dans le problème PDDL avec le prédicat `deadlock`.

Dans le domaine PDDL, les actions de poussée vérifient que la case d’arrivée de la caisse n’est pas un deadlock. Cela réduit l’espace de recherche et évite certains mauvais plans.

Cette gestion ne détecte pas tous les deadlocks possibles du Sokoban, mais elle permet déjà d’éliminer les cas les plus simples et les plus fréquents.

## Génération du problème PDDL

Le fichier `MakePddl.java` génère automatiquement :

- la liste des cases accessibles ;
- la liste des caisses ;
- les positions initiales du joueur et des caisses ;
- les cases objectifs ;
- les cases libres ;
- les relations de voisinage entre cases ;
- les cases deadlock ;
- le but du problème.

Le fichier généré est :

    pddl/sokoban/from_agent.pddl

Ce fichier est recréé à chaque lancement de l’agent. Il n’est donc pas nécessaire de le modifier à la main.
## Installation de PDDL4J

Avant de compiler le projet, il faut installer `pddl4j-4.0.0.jar` dans le dépôt Maven local.

Sur la machine virtuelle, on utilise aussi l’option `MAVEN_OPTS` pour prendre en compte les proxies système :

    MAVEN_OPTS="-Djava.net.useSystemProxies=true" mvn install:install-file \
       -Dfile=pddl4j-4.0.0.jar \
       -DgroupId=fr.uga \
       -DartifactId=pddl4j \
       -Dversion=4.0.0 \
       -Dpackaging=jar \
       -DgeneratePom=true

## Compilation

Depuis la racine du projet, lancer :

    mvn clean
    mvn compile
    mvn test
    mvn package

La commande `mvn package` génère le fichier exécutable :

    target/sokoban-1.0-SNAPSHOT-jar-with-dependencies.jar

## Lancement

Après compilation, lancer :

    java --add-opens java.base/java.lang=ALL-UNNAMED \
      -server -Xms2048m -Xmx2048m \
      -cp target/sokoban-1.0-SNAPSHOT-jar-with-dependencies.jar \
      sokoban.SokobanMain

Il faut laisser ce terminal ouvert, car il lance le serveur local permettant de visualiser les solutions.

## Visualisation des solutions

L’interface web est ensuite disponible ici :

    http://localhost:8888/test.html

Si le programme est lancé sur une machine distante en SSH, il faut créer un tunnel SSH depuis la machine locale.

Dans un deuxième terminal local, lancer :

    ssh -L 8888:localhost:8888 ecloud@10.0.22.87

Ensuite, ouvrir dans le navigateur local :

    http://localhost:8888/test.html

Le premier terminal doit rester ouvert avec `SokobanMain`, et le deuxième terminal doit rester ouvert avec le tunnel SSH.

## Choix du niveau

Le niveau utilisé est choisi dans le fichier :

    src/main/java/sokoban/SokobanMain.java

Par exemple :

    gameRunner.setTestCase("test20.json");

Pour tester un autre niveau, il suffit de remplacer `test20.json` par un autre fichier présent dans le dossier `config`, par exemple `test1.json`, `test5.json`, etc.

