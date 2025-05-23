package com.example.microservices.composite.product.services;

import com.example.api.core.product.Product;
import com.example.api.core.product.ProductService;
import com.example.api.core.recommendation.Recommendation;
import com.example.api.core.recommendation.RecommendationService;
import com.example.api.core.review.Review;
import com.example.api.core.review.ReviewService;
import com.example.api.event.Event;
import com.example.api.exception.InvalidInputException;
import com.example.api.exception.NotFoundException;
import com.example.util.http.HttpErrorInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;

import static com.example.api.event.Event.Type.CREATE;
import static com.example.api.event.Event.Type.DELETE;
import static java.util.logging.Level.FINE;

@Service
@Slf4j
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
  private static final String PRODUCT_SERVICE_URL = "http://product";
  private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation";
  private static final String REVIEW_SERVICE_URL = "http://review";

  private final WebClient webClient;
  private final ObjectMapper mapper;
  private final StreamBridge streamBridge;
  private final Scheduler publishEventScheduler;

  @Autowired
  public ProductCompositeIntegration(WebClient.Builder webClientbuilder, ObjectMapper mapper, StreamBridge streamBridge,
                                     @Qualifier("publishEventScheduler") Scheduler publishEventScheduler) {
    this.webClient = webClientbuilder.build();
    this.mapper = mapper;
    this.streamBridge = streamBridge;
    this.publishEventScheduler = publishEventScheduler;
  }

  @Override
  public Mono<Product> getProduct(int productId) {
    String url = PRODUCT_SERVICE_URL + "/product/" + productId;
    return webClient.get().uri(url).retrieve()
        .bodyToMono(Product.class)
        .log(log.getName(), FINE)
        .onErrorMap(WebClientResponseException.class, this::handleException);
  }

  @Override
  public Mono<Product> createProduct(Product body) {
    return Mono.fromCallable(() -> {
          sendMessage("products-out-0", new Event<>(CREATE, body.getProductId(), body));
          return body;
        })
        .subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    return Mono.fromRunnable(() -> sendMessage("products-out-0", new Event<>(DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {
    String url = RECOMMENDATION_SERVICE_URL + "/recommendation?productId=" + productId;
    return webClient.get().uri(url).retrieve()
        .bodyToFlux(Recommendation.class)
        .log(log.getName(), FINE)
        .onErrorResume(error -> Flux.empty());
  }

  @Override
  public Mono<Recommendation> createRecommendation(Recommendation body) {
    return Mono.fromCallable(() -> {
      sendMessage("recommendations-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Void> deleteRecommendations(int productId) {
    return Mono.fromRunnable(() -> sendMessage("recommendations-out-0", new Event<>(DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  @Override
  public Flux<Review> getReviews(int productId) {
    String url = REVIEW_SERVICE_URL + "/review?productId=" + productId;
    return webClient.get().uri(url).retrieve()
        .bodyToFlux(Review.class)
        .log(log.getName(), FINE)
        .onErrorResume(error -> Flux.empty());
  }

  @Override
  public Mono<Review> createReview(Review body) {
    return Mono.fromCallable(() -> {
      sendMessage("reviews-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Void> deleteReviews(int productId) {
    return Mono.fromRunnable(() -> sendMessage("reviews-out-0", new Event<>(DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  private RuntimeException handleException(WebClientResponseException ex) {
    switch (HttpStatus.resolve(ex.getStatusCode().value())) {
      case NOT_FOUND: return new NotFoundException(getErrorMessage(ex));
      case UNPROCESSABLE_ENTITY: return new InvalidInputException(getErrorMessage(ex));
      case null:
      default:
        log.warn("Got an unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
        log.warn("Error body: {}", ex.getResponseBodyAsString());
        return ex;
    }
  }

  private String getErrorMessage(WebClientResponseException ex) {
    try {
      return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).message();
    } catch (IOException ioex) {
      return ex.getMessage();
    }
  }

  private <K, T> void sendMessage(String bindingName, Event<K, T> event) {
    Message<Event<K, T>> message = MessageBuilder
        .withPayload(event)
        .setHeader("partitionKey", event.key())
        .build();
    streamBridge.send(bindingName, message);
  }
}
