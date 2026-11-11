# RandomMinigames (Paper 26.x)

Co 5 minut losuje minigre: TNT Run, Spleef, Elytra Race. Areny buduja sie i sprzataja same.

## Budowanie
Wymagane: JDK 25 + Maven.

    mvn package

Gotowy plik: `target/RandomMinigames-1.0.0.jar` -> wrzuc do `plugins/`.

Bez instalowania Javy: wrzuc projekt na GitHub, zakladka Actions -> Build -> pobierz artefakt `RandomMinigames`.

Jesli Maven nie znajdzie `paper-api`, ustaw w pom.xml konkretna wersje w `<paper.version>`
(lista: https://repo.papermc.io/service/rest/repository/browse/maven-public/io/papermc/paper/paper-api/).

## Komendy
/minigame join | leave | list
/minigame start [gra] | stop | reload   (uprawnienie minigame.admin)

Konfiguracja: plugins/RandomMinigames/config.yml
