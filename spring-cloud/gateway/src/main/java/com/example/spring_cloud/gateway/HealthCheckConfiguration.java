package com.example.spring_cloud.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.logging.Level.FINE;

@Configuration
@Slf4j
public class HealthCheckConfiguration {

  private final WebClient webClient;

  @Autowired
  public HealthCheckConfiguration(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.build();
  }

  @Bean
  ReactiveHealthContributor coreServices() {
    final Map<String, ReactiveHealthIndicator> registry = new LinkedHashMap<>();
    registry.put("product", () -> getHealth("http://product"));
    registry.put("recommendation", () -> getHealth("http://recommendation"));
    registry.put("review", () -> getHealth("http://review"));
    registry.put("product-composite", () -> getHealth("http://product-composite"));
    return CompositeReactiveHealthContributor.fromMap(registry);
  }

  private Mono<Health> getHealth(String baseUrl) {
    var url = baseUrl + "/actuator/health";
    log.debug("Will call the Health API on URL: {}", url);
    return webClient.get().uri(url).retrieve().bodyToMono(String.class)
        .map(s -> new Health.Builder().up().build())
        .onErrorResume(ex -> Mono.just(new
            Health.Builder().down(ex).build()))
        .log(log.getName(), FINE);
  }
}
