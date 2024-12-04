package com.example.api.core.recommendation;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationService {

  @GetMapping(value = "/recommendation", produces = "application/json")
  Flux<Recommendation> getRecommendations(@RequestParam(value = "productId") int productId);

  @PostMapping(value = "/recommendation", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  Mono<Recommendation> createRecommendation(@RequestBody Recommendation body);

  @DeleteMapping(value = "/recommendation")
  Mono<Void> deleteRecommendations(@RequestParam(value = "productId") int productId);
}
