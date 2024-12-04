package com.example.microservices.composite.product;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("api.common")
public record ApiProperties(String version,
                            String title,
                            String description,
                            String termsOfService,
                            String license,
                            String licenseUrl,
                            String externalDocDesc,
                            String externalDocUrl,
                            ContactData contact) {
  public String contactName() {
    return contact.name();
  }
  public String contactUrl() {
    return contact.url();
  }
  public String contactEmail() {
    return contact.email();
  }
  record ContactData(String name, String url, String email) {
  }
}
