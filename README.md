# Superhero demo webservices

## To build the project

    $ mvn package -DskipTests

## To execute tests:
You have to provide `HERO_MONGO_URI_TEST`

    $ export HERO_MONGO_URI_TEST='mongo://host:port/testingDb?options…'
    $ mvn test

## To launch:
You have to provide `HERO_MONGO_URI` and optionally `HERO_API_HOST` (defaults to `localhost`) and `HERO_API_PORT` (defaults to `8080`)

    $ export HERO_MONGO_URI='mongo://host:port/db?options…'
    $ java -jar target/superhero-0.0.1-SNAPSHOT-fat.jar

Bind the host to all interfaces (e.g. `0.0.0.0` for TCPv4) or just pick specific one, if you want the API to be accesible from outside.
