package de.terrestris.mde.mde_backend.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.service.GeneratorUtils;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/codelists")
public class CodelistController {

  @GetMapping("/inspire/schema/{theme}")
  public ResponseEntity<List<String>> getSchemas(@PathVariable("theme") String theme) {
    try {
      var schemas =
          GeneratorUtils.INSPIRE_THEME_APPSCHEMA_MAP.get(
              JsonIsoMetadata.InspireTheme.valueOf(theme));
      if (schemas == null || schemas.isEmpty()) {
        return new ResponseEntity<>(NOT_FOUND);
      }
      return new ResponseEntity<>(schemas, OK);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid Inspire theme: {}", theme);
      log.trace("Stack trace:", e);
      return new ResponseEntity<>(NOT_FOUND);
    }
  }
}
