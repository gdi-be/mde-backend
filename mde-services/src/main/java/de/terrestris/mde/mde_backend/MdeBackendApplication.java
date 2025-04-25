package de.terrestris.mde.mde_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableScheduling
@Configuration
@PropertySource(value = "classpath:/application.properties", encoding = "UTF8")
@PropertySource(
    ignoreResourceNotFound = true,
    value = "file:///config/application.properties",
    encoding = "UTF8")
public class MdeBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(MdeBackendApplication.class, args);
  }
}
