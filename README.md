## Running the importer

To run the importer, adapt the `application.properties` in the `resources` folder of the importer to point to your database.
Make sure to run the importer with the `-d` parameter pointing to the directories where the XML files to import live,
for example `-d /home/user/geodata/berlin/export/`. You'll also need to set the environment variable `VARIABLE_FILE` to
the location of your variables mapping file.
