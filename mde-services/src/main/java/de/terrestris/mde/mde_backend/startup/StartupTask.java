package de.terrestris.mde.mde_backend.startup;

import de.terrestris.mde.mde_backend.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupTask {

  @Autowired
  private SearchService searchService;

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    searchService.reindexAll();
  }
}
