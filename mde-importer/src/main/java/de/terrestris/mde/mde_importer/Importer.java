package de.terrestris.mde.mde_importer;

import de.terrestris.mde.mde_importer.importer.ImportService;
import java.util.concurrent.Callable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "MDE Importer", version = "0.0.1", mixinStandardHelpOptions = true)
public class Importer implements Callable<Boolean> {

  @Option(
      names = {"-d", "--directory"},
      required = true,
      description = "the directory where the XML files to import are located")
  private String directory;

  public static void main(String[] args) {
    int code = new CommandLine(new Importer()).execute(args);
    System.exit(code);
  }

  @Override
  public Boolean call() throws Exception {
    var ctx =
        new AnnotationConfigApplicationContext(
            "de.terrestris.mde.mde_backend.config",
            "de.terrestris.mde.mde_backend",
            "de.terrestris.mde.mde_importer.config",
            "de.terrestris.mde.mde_importer.importer");
    ctx.start();
    var service = ctx.getBean(ImportService.class);
    return service.importMetadata(directory);
  }
}
