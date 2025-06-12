package de.terrestris.mde.mde_backend.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetails {

  private String organisation;

  private String firstName;

  private String lastName;

  private String phone;

  private String street;

  private String postalCode;

  private String city;

  private String email;
}
