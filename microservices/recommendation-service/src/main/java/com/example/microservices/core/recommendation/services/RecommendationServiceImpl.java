package com.example.microservices.core.recommendation.services;

import com.example.api.core.recommendation.Recommendation;
import com.example.api.core.recommendation.RecommendationService;
import com.example.api.exception.InvalidInputException;
import com.example.microservices.core.recommendation.persistence.RecommendationRepository;
import com.example.util.http.ServiceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.logging.Level;

@RestController
@Slf4j
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

  private final ServiceUtil serviceUtil;
  private final RecommendationRepository repository;
  private final RecommendationMapper mapper;

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {
    log.debug("Will get recommendations for product with id={}", productId);
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
    return repository.findByProductId(productId)
        .log(log.getName(), Level.FINE)
        .map(mapper::entityToApi)
        .map(r -> r.setServiceAddress(serviceUtil.getServiceAddress()));
  }

  @Override
  public Mono<Recommendation> createRecommendation(Recommendation body) {
    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }
    return Mono.just(body)
        .map(mapper::apiToEntity)
        .flatMap(repository::save)
        .log(log.getName(), Level.FINE)
        .onErrorMap(DuplicateKeyException.class,
            e -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", " +
                "Recommendation Id:" + body.getRecommendationId()))
        .map(mapper::entityToApi);
  }

  @Override
  public Mono<Void> deleteRecommendations(int productId) {
    log.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
    return repository.deleteAll(repository.findByProductId(productId));
  }
}
