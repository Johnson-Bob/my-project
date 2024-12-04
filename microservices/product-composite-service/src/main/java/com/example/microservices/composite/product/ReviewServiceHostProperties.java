package com.example.microservices.composite.product;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.review-service")
public record ReviewServiceHostProperties(String host, int port) {
}
