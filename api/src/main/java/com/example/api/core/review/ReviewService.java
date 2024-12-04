package com.example.api.core.review;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReviewService {

  @GetMapping(value = "/review", produces = "application/json")
  Flux<Review> getReviews(@RequestParam(value = "productId") int productId);

  @PostMapping(value = "/review", consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  Mono<Review> createReview(@RequestBody Review body);

  @DeleteMapping(value = "/review")
  Mono<Void> deleteReviews(@RequestParam(value = "productId") int productId);

}
