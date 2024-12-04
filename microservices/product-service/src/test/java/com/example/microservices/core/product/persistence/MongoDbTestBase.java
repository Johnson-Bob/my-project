package com.example.microservices.core.product.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public abstract class MongoDbTestBase {
  @Container
  private static final MongoDBContainer database = new MongoDBContainer("mongo:8.0.3")
      .withStartupTimeout(Duration.of(3, ChronoUnit.MINUTES));
  
  static {
    database.start();
  }

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", database::getReplicaSetUrl);
    registry.add("spring.data.mongodb.database", () -> "test");
    registry.add("spring.data.mongodb.auto-index-creation", () -> true);
  }
}
