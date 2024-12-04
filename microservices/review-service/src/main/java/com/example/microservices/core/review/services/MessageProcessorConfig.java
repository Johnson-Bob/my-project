package com.example.microservices.core.review.services;

import com.example.api.core.review.Review;
import com.example.api.core.review.ReviewService;
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
  private final ReviewService reviewService;

  @Bean
  Function<Flux<Event<Integer, Review>>, Flux<Void>> messageProcessor() {
    return eventFlux -> eventFlux
        .flatMap(event ->
            switch (event.eventType()) {
              case CREATE -> reviewService.createReview(event.data()).then();
              case DELETE -> reviewService.deleteReviews(event.key());
            });
  }
}
