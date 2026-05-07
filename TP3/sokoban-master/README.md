Play the game on [CodinGame](https://www.codingame.com/training/hard/sokoban)

Maven is needed.

## Installation de PDDL4J dans Maven

Avant de compiler le projet, il faut installer `pddl4j-4.0.0.jar` dans le dépôt Maven local.

Sur la machine virtuelle, on utilise `MAVEN_OPTS` pour prendre en compte les proxies système :

```bash
MAVEN_OPTS="-Djava.net.useSystemProxies=true" mvn install:install-file \
   -Dfile=pddl4j-4.0.0.jar \
   -DgroupId=fr.uga \
   -DartifactId=pddl4j \
   -Dversion=4.0.0 \
   -Dpackaging=jar \
   -DgeneratePom=true 
 ````
 ##Compilation
 mvn clean
mvn compile
mvn test
mvn package

Work with maven: mvn clean, mvn compile, mvn test, mvn package

Run with: 
 ````
java --add-opens java.base/java.lang=ALL-UNNAMED \
      -server -Xms2048m -Xmx2048m \
      -cp target/sokoban-1.0-SNAPSHOT-jar-with-dependencies.jar \
      sokoban.SokobanMain
  ````     
Sorry ```mvn exec:java``` has still an open issue ("Directory src/main/resources/view/assets not found.")

See planning solutions at
ssh -L 8888:localhost:8888 ecloud@adress
http://localhost:8888/test.html
