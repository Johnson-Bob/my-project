package com.example.microservices.composite.product;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.product-service")
public record ProductServiceHostProperties(String host, int port) {
}
