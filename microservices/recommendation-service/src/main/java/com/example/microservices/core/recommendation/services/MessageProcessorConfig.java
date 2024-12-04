package com.example.microservices.core.recommendation.services;

import com.example.api.core.recommendation.Recommendation;
import com.example.api.core.recommendation.RecommendationService;
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
  private final RecommendationService recommendationService;

  @Bean
  Function<Flux<Event<Integer, Recommendation>>, Flux<Void>> messageProcessor() {
    return eventFlux -> eventFlux
        .flatMap(event ->
            switch (event.eventType()) {
              case CREATE -> recommendationService.createRecommendation(event.data()).then();
              case DELETE -> recommendationService.deleteRecommendations(event.key());
            });
  }
}
