package com.example.microservices.core.review;

import com.example.api.core.review.Review;
import com.example.api.event.Event;
import com.example.api.exception.InvalidInputException;
import com.example.microservices.core.review.persistence.PostgresSQLTestBase;
import com.example.microservices.core.review.persistence.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static com.example.api.event.Event.Type.CREATE;
import static com.example.api.event.Event.Type.DELETE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@EnableTestBinder
class ReviewServiceApplicationTests extends PostgresSQLTestBase {
  @Autowired
  private WebTestClient client;
  @Autowired
  private ReviewRepository repository;
  @Autowired
  private Function<Flux<Event<Integer, Review>>, Flux<Void>> messageProcessor;

  @BeforeEach
  void setupDb() {
    repository.deleteAll();
  }

  @Test
  @Transactional
  void getReviewsByProductId() {

    int productId = 1;

    assertEquals(0, repository.findByProductId(productId).size());

    postAndVerifyReview(productId, 1, OK);
    postAndVerifyReview(productId, 2, OK);
    postAndVerifyReview(productId, 3, OK);

    assertEquals(3, repository.findByProductId(productId).size());

    getAndVerifyReviewsByProductId(productId, OK)
      .jsonPath("$.length()").isEqualTo(3)
      .jsonPath("$[2].productId").isEqualTo(productId)
      .jsonPath("$[2].reviewId").isEqualTo(3);
  }

  @Test
  void duplicateError() {

    int productId = 1;
    int reviewId = 1;

    assertEquals(0, repository.count());

    sendCreateProductEvent(productId, reviewId).verifyComplete();
    assertEquals(1, repository.count());

    sendCreateProductEvent(productId, reviewId)
        .verifyErrorMatches(ex -> ex instanceof InvalidInputException iie
            && iie.getMessage().equals("Duplicate key, Product Id: 1, Review Id: 1"));

    assertEquals(1, repository.count());
  }

  @Test
  void deleteReviews() {

    int productId = 1;
    int reviewId = 1;

    sendCreateProductEvent(productId, reviewId).verifyComplete();
    assertEquals(1, repository.findByProductId(productId).size());

    sendDeleteProductEvent(productId).verifyComplete();
    assertEquals(0, repository.findByProductId(productId).size());

    sendDeleteProductEvent(productId).verifyComplete();
  }

  @Test
  void getReviewsMissingParameter() {

    getAndVerifyReviewsByProductId("", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
  }

  @Test
  void getReviewsInvalidParameter() {

    getAndVerifyReviewsByProductId("?productId=no-integer", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getReviewsNotFound() {

    getAndVerifyReviewsByProductId("?productId=213", OK)
      .jsonPath("$.length()").isEqualTo(0);
  }

  @Test
  void getReviewsInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    getAndVerifyReviewsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_ENTITY)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }

  private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
    return getAndVerifyReviewsByProductId("?productId=" + productId, expectedStatus);
  }

  private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(String productIdQuery, HttpStatus expectedStatus) {
    return client.get()
      .uri("/review" + productIdQuery)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private WebTestClient.BodyContentSpec postAndVerifyReview(int productId, int reviewId, HttpStatus expectedStatus) {
    Review review = new Review(productId, reviewId, "Author " + reviewId, "Subject " + reviewId, "Content " + reviewId, "SA");
    return client.post()
      .uri("/review")
      .body(just(review), Review.class)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private void deleteAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
    client.delete()
        .uri("/review?productId=" + productId)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(expectedStatus)
        .expectBody();
  }

  private StepVerifier.FirstStep<Void> sendCreateProductEvent(int productId, int reviewId) {
    Review review = new Review(productId, reviewId, "Author " + reviewId, "Subject " + reviewId,
        "Content " + reviewId, "SA");

    Event<Integer, Review> event = new Event<>(CREATE, productId, review);
    return StepVerifier.create(messageProcessor.apply(Flux.just(event)));
  }
  private StepVerifier.FirstStep<Void> sendDeleteProductEvent(int productId) {
    Event<Integer, Review> event = new Event<>(DELETE, productId, null);
    return StepVerifier.create(messageProcessor.apply(Flux.just(event)));
  }
}
