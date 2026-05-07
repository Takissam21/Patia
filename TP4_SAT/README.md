# YetAnotherSATPlanner

Ce projet contient notre implémentation d’un planificateur SAT avec PDDL4J et SAT4J.

L’idée générale est de transformer un problème de planification PDDL en problème SAT, de le résoudre avec SAT4J, puis de reconstruire un plan à partir du modèle SAT obtenu.

## Compilation

Depuis la racine du projet :

```bash
javac -d classes -cp "lib/pddl4j-4.0.0.jar:lib/org.sat4j.core.jar:lib/sat4j-sat.jar" src/fr/uga/pddl4j/yasp/SATEncoding.java src/fr/uga/pddl4j/yasp/YetAnotherSATPlanner.java
```

## Lancer le SATPlanner

Exemple avec le domaine Taquin :

```bash
java -cp "classes:lib/pddl4j-4.0.0.jar:lib/org.sat4j.core.jar:lib/sat4j-sat.jar" -server -Xms2048m -Xmx2048m fr.uga.pddl4j.yasp.YetAnotherSATPlanner benchmarks/taquin/domain.pddl benchmarks/taquin/p01.pddl
```

Le plan trouvé est affiché dans le terminal et écrit dans le fichier :

```text
sat_plan.txt
```

## Validation avec VAL

Si VAL est disponible sur la machine, on peut vérifier le plan généré avec :

```bash
Validate -v -t 0.001 benchmarks/taquin/domain.pddl benchmarks/taquin/p01.pddl sat_plan.txt
```

Si le plan est correct, VAL indique que le plan est valide.

## Lancer les benchmarks

Le script Python compile le planner, lance HSP et notre SATPlanner sur plusieurs problèmes, puis génère un fichier CSV et des graphes.

```bash
python3 benchmark_planners.py
```

Les résultats sont écrits dans :

```text
results/benchmark_results.csv
```

Les sorties complètes des planificateurs sont dans :

```text
results/raw_outputs/
```

Les graphes sont dans :

```text
results/figures/
```

## Organisation du projet

```text
src/fr/uga/pddl4j/yasp/SATEncoding.java
src/fr/uga/pddl4j/yasp/YetAnotherSATPlanner.java
benchmark_planners.py
benchmarks/
lib/
results/
```

Les deux fichiers principaux sont :

```text
SATEncoding.java
YetAnotherSATPlanner.java
```

## Implémentation

La classe `YetAnotherSATPlanner` s’occupe de la partie générale du planner.

Elle fait les étapes suivantes :

- lecture du domaine et du problème PDDL avec PDDL4J ;
- instanciation du problème ;
- création d’un encodage SAT pour un horizon donné ;
- envoi des clauses à SAT4J ;
- récupération du modèle SAT si la formule est satisfaisable ;
- décodage du modèle en plan ;
- écriture du plan dans `sat_plan.txt`.

La classe `SATEncoding` contient la partie la plus importante : l’encodage du problème de planification en clauses SAT.

Dans le cours, un problème de planification est transformé en problème SAT en représentant les fluents et les actions par des variables propositionnelles indexées par le temps. Notre code suit cette idée.

Pour chaque horizon, on encode :

- l’état initial ;
- le but à l’état final ;
- les préconditions des actions ;
- les effets positifs et négatifs des actions ;
- les transitions entre deux états ;
- les contraintes entre actions.

Les clauses SAT sont représentées comme des listes d’entiers. Un entier positif représente une variable vraie, et un entier négatif représente sa négation. C’est le format attendu par SAT4J.

Par exemple, une implication du type :

```text
action -> précondition
```

est encodée sous forme de clause :

```text
not action OR précondition
```

De la même manière, les effets sont encodés comme des contraintes entre l’action choisie à une étape et l’état suivant.

Notre planner est séquentiel : il impose au plus une action par étape. Cela simplifie l’encodage et permet de reconstruire facilement un plan ordonné.

## Lien avec le cours

Le code suit directement le principe vu dans le cours de SAT Planning :

```text
Problème PDDL -> encodage SAT -> solveur SAT -> décodage -> plan
```

Le cours présente aussi les éléments à encoder :

- l’état initial ;
- le but ;
- les actions ;
- les transitions d’état ;
- les contraintes entre actions.

Ces éléments correspondent aux différentes parties de `SATEncoding.java`.

## Résultats des tests automatisés

Le script de benchmark compare notre SATPlanner avec HSP, le planificateur A* de PDDL4J.

Les métriques utilisées sont :

- le temps total d’exécution ;
- la longueur du plan.

Dans notre script final, nous testons trois domaines :

```text
Blocksworld
Logistics
Taquin
```

### Blocksworld

Sur les problèmes Blocksworld testés, HSP et SATPlanner trouvent des plans de même longueur.

SATPlanner est plus rapide sur les petites instances testées.

### Logistics

Sur Logistics, HSP résout toutes les instances testées.

SATPlanner résout plusieurs instances avec la même longueur de plan que HSP, mais certaines instances plus longues provoquent un timeout. Cela montre que l’encodage SAT devient coûteux quand l’horizon nécessaire augmente.

### Taquin

Sur Taquin, les deux planificateurs trouvent des plans sur les problèmes testés.

Les problèmes ont une difficulté progressive. Pour les premiers problèmes, SATPlanner trouve des plans de même longueur que HSP. Sur le problème le plus difficile, SATPlanner trouve un plan valide mais plus long que celui de HSP.

Cela montre que notre planner trouve des plans corrects, mais ne garantit pas toujours le plan le plus court.

## Limites

Notre SATPlanner fonctionne sur plusieurs problèmes simples et moyens, mais il n’est pas optimisé pour les grandes instances.

La principale limite vient de la taille de la formule SAT. Quand le nombre d’étapes augmente, le nombre de variables et de clauses augmente aussi. Cela peut provoquer des temps d’exécution élevés ou des timeouts.

Le planner s’arrête dès qu’un modèle SAT est trouvé pour l’horizon testé. Il cherche donc un plan satisfaisant, mais pas forcément toujours le meilleur plan possible.

## Conclusion

Ce projet montre qu’un problème de planification peut être résolu en le transformant en problème SAT.

Notre implémentation encode les états, les actions, les préconditions, les effets et les transitions sous forme de clauses SAT. SAT4J est ensuite utilisé pour trouver un modèle, et ce modèle est décodé pour obtenir un plan.

Les tests montrent que le planner fonctionne correctement sur plusieurs domaines. Il est efficace sur de petites instances, mais devient plus limité quand les problèmes demandent des horizons plus grands.