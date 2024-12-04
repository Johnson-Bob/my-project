package com.example.microservices.composite.product;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan("com.example")
public class ProductCompositeServiceApplication {

  public static void main(String[] args) {
    var application = new SpringApplication(ProductCompositeServiceApplication.class);
    application.setApplicationStartup(new BufferingApplicationStartup(2048));
    application.run(args);
  }

  @Bean
  public OpenAPI openApiDocumentation(@Autowired ApiProperties apiProperties) {
    return new OpenAPI()
      .info(new Info().title(apiProperties.title())
        .description(apiProperties.description())
        .version(apiProperties.version())
        .contact(new Contact()
          .name(apiProperties.contactName())
          .url(apiProperties.contactUrl())
          .email(apiProperties.contactEmail()))
        .termsOfService(apiProperties.termsOfService())
        .license(new License()
          .name(apiProperties.license())
          .url(apiProperties.licenseUrl())))
      .externalDocs(new ExternalDocumentation()
        .description(apiProperties.externalDocDesc())
        .url(apiProperties.externalDocUrl()));
  }

  @Bean
  public Scheduler publishEventScheduler(@Value("${app.threadPoolSize:10}")Integer threadPoolSize,
                                         @Value("${app.taskQueueSize:100}")Integer taskQueueSize) {
    return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "jdbc-pool");
  }
}
