package com.example.api.core.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface ProductService {
  @GetMapping(value = "/product/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
  Mono<Product> getProduct(@PathVariable int productId);

  @PostMapping(value = {"/product", "/product/"}, consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  Mono<Product> createProduct(@RequestBody Product body);

  @DeleteMapping(value = "/product/{productId}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  Mono<Void> deleteProduct(@PathVariable int productId);
}
