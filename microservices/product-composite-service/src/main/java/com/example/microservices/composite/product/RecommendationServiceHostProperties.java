package com.example.microservices.composite.product;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.recommendation-service")
public record RecommendationServiceHostProperties(String host, int port) {

}
