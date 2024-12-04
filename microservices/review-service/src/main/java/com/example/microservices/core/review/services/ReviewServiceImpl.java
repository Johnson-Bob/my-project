package com.example.microservices.core.review.services;

import com.example.api.core.review.Review;
import com.example.api.core.review.ReviewService;
import com.example.api.exception.InvalidInputException;
import com.example.microservices.core.review.persistence.ReviewEntity;
import com.example.microservices.core.review.persistence.ReviewRepository;
import com.example.util.http.ServiceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

  private final ServiceUtil serviceUtil;
  private final ReviewRepository repository;
  private final ReviewMapper mapper;
  @Qualifier("jdbcScheduler")
  private final Scheduler jdbcScheduler;

  @Override
  public Flux<Review> getReviews(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
    log.info("Will get reviews for product with id={}", productId);
    return Mono.fromCallable(() -> internalGetReview(productId))
        .flatMapMany(Flux::fromIterable)
        .log(log.getName(), Level.FINE)
        .subscribeOn(jdbcScheduler);
  }

  private List<Review> internalGetReview(int productId) {
    List<Review> list = repository.findByProductId(productId).stream()
        .map(mapper::entityToApi)
        .map(r -> r.setServiceAddress(serviceUtil.getServiceAddress()))
        .toList();
    log.debug("/reviews response size: {}", list.size());
    return list;
  }

  @Override
  public Mono<Review> createReview(Review body) {
    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }
    return Mono.fromCallable(() -> internalCreateReview(body)).subscribeOn(jdbcScheduler);
  }

  private Review internalCreateReview(Review body) {
    try {
      return Optional.ofNullable(body)
          .map(mapper::apiToEntity)
          .map(entity -> {
            ReviewEntity newEntity = repository.save(entity);
            log.debug("createReview: created a review entity: {}/{}", newEntity.getProductId(), newEntity.getReviewId());
            return newEntity;
          })
          .map(mapper::entityToApi)
          .orElseThrow(() -> new InvalidInputException("The new review is null"));
    } catch (DataIntegrityViolationException e) {
      String exceptionMessage = Optional.ofNullable(body)
          .map(r ->
              String.format("Duplicate key, Product Id: %d, Review Id: %d", r.getProductId(), r.getReviewId()))
          .orElse("Duplicate key");
      throw new InvalidInputException(exceptionMessage);
    }
  }

  @Override
  public Mono<Void> deleteReviews(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
    log.debug("deleteReview: tries to delete reviews for the product with productId: {}", productId);
    return Mono.fromRunnable(() -> internalDeleteReviews(productId)).subscribeOn(jdbcScheduler).then();
  }

  private void internalDeleteReviews(int productId) {
    repository.deleteAll(repository.findByProductId(productId));
  }
}
