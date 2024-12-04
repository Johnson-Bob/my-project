package com.example.util.http;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class ReactorTests {
  @Test
  void testFlux() {
    Flux<Integer> flux = Flux.just(1, 2, 3, 4)
        .filter(n -> n % 2 == 0)
        .map(n -> n * 2)
        .log();
    StepVerifier.create(flux)
            .expectNext(4, 8)
            .verifyComplete();
  }
}