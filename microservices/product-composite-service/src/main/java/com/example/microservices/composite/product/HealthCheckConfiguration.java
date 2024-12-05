package com.example.microservices.composite.product;

import com.example.microservices.composite.product.services.ProductCompositeIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthCheckConfiguration {
  @Autowired
  private ProductCompositeIntegration integration;
}
