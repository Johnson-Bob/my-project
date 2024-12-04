package com.example.microservices.core.product.services;

import com.example.api.core.product.Product;
import com.example.api.core.product.ProductService;
import com.example.api.event.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MessageProcessorConfig {
  private final ProductService productService;

  @Bean
  Function<Flux<Event<Integer, Product>>, Flux<Void>> messageProcessor() {
    return events -> events.flatMap(
        event -> switch (event.eventType()) {
          case CREATE -> productService.createProduct(event.data()).then();
          case DELETE -> productService.deleteProduct(event.key());
        }
    );
  }
}
