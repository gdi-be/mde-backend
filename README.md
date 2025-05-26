## Getting the importer

Download the latest importer jar from [here](https://github.com/gdi-be/mde-backend/packages/2390377). Make sure you
download the .jar with the spring-boot suffix.

## Running the importer

To run the importer, adapt the `application.properties` in the `resources` folder of the importer to point to your database.
Make sure to run the importer with the `-d` parameter pointing to the directories where the XML files to import live,
for example `-d /home/user/geodata/berlin/export/`. You'll also need to set the environment variable `VARIABLE_FILE` to
the location of your variables mapping file as well as the `CODELISTS_DIR` variable pointing to your `codelists` directory.
You can also modify your lucene directory to point somewhere else, it will be recreated upon startup anyway.

Example call:

```bash
CODELISTS_DIR=../../mde-docker/codelists/ VARIABLE_FILE=../../mde-docker/mde-backend/variables.json $JAVA_HOME/bin/java --add-modules jdk.incubator.vector -Dhibernate.search.backend.directory.root=/tmp/lucene -jar target/mde-importer-0.0.1-SNAPSHOT-spring-boot.jar -d ~/geodata/berlin/export/
```

Please note that the importer expects the postgres database to live on localhost port 5432, so you might need to
temporarily expose the postgres port to your machine, for example by adding a ports section to your compose file:

```yaml
  mde-postgres:
    container_name: mde-postgres
    ...
    ports:
      - 5432:5432
    ...
```

## Activate debug logging

In order to activate debug or trace logging for a specific package run:

```
curl -k https://localhost/api/actuator/loggers/de.terrestris -X POST --data-raw '{"configuredLevel":"TRACE"}' -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -D -
```

Please note that on trace logging CSW-T requests to the GNOS are written to the `/tmp/` directory in the backend
container and NOT deleted.
