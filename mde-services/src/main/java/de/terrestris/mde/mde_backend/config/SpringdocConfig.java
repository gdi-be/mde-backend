package de.terrestris.mde.mde_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringdocConfig {

  protected String title = "MDE REST API";
  protected String description = "This is the REST API description of the MDE API";
  protected String version = "1.0.0";
  protected String contactName = "terrestris GmbH & Co. KG";
  protected String contactUrl = "https://www.terrestris.de";
  protected String contactMail = "info@terrestris.de";
  protected String license = "Apache License, Version 2.0";
  protected String licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt";

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title(title)
                .version(version)
                .description(description)
                .contact(new Contact().name(contactName).url(contactUrl).email(contactMail))
                .license(new License().name(license).url(licenseUrl)))
        .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "Bearer Authentication",
                    new SecurityScheme()
                        .name("Bearer Authentication")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
