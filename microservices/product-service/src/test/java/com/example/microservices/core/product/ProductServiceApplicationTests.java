package com.example.microservices.core.product;

import com.example.api.core.product.Product;
import com.example.api.event.Event;
import com.example.api.exception.InvalidInputException;
import com.example.microservices.core.product.persistence.MongoDbTestBase;
import com.example.microservices.core.product.persistence.ProductRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static com.example.api.event.Event.Type.CREATE;
import static com.example.api.event.Event.Type.DELETE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"eureka.client.enabled=false"})
@EnableTestBinder
class ProductServiceApplicationTests extends MongoDbTestBase {
  @Autowired
  private WebTestClient client;
  @Autowired
  private ProductRepository repository;
  @Autowired
  @Qualifier("messageProcessor")
  private Function<Flux<Event<Integer, Product>>, Flux<Void>> messageProcessor;

  @BeforeEach
  void setupDb() {
    StepVerifier.create(repository.deleteAll()).verifyComplete();
  }

  @Test
  void getProductById() {

    int productId = 1;

    postAndVerifyProduct(productId, ACCEPTED);

    assertNotNull(repository.findByProductId(productId).block());

    getAndVerifyProduct(productId, OK).jsonPath("$.productId").isEqualTo(productId);
  }

  @Test
  void duplicateError() {

    int productId = 1;

    sendCreateProductEvent(productId).verifyComplete();

    assertNotNull(repository.findByProductId(productId).block());

    sendCreateProductEvent(productId)
        .verifyErrorMatches(ex -> ex instanceof InvalidInputException iie
            && iie.getMessage().equals("Duplicate key, Product Id: " + productId));
  }

  @Test
  void deleteProduct() {

    int productId = 1;

    sendCreateProductEvent(productId).verifyComplete();
    StepVerifier.create(repository.findByProductId(productId)).assertNext(Assertions::assertNotNull).verifyComplete();

    sendDeleteProductEvent(productId).verifyComplete();
    StepVerifier.create(repository.findByProductId(productId)).verifyComplete();

    sendDeleteProductEvent(productId).verifyComplete();
  }

  @Test
  void getProductInvalidParameterString() {

    getAndVerifyProduct("/no-integer", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/product/no-integer")
      .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getProductNotFound() {

    int productIdNotFound = 13;
    getAndVerifyProduct(productIdNotFound, NOT_FOUND)
      .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
      .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
  }

  @Test
  void getProductInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    getAndVerifyProduct(productIdInvalid, UNPROCESSABLE_ENTITY)
      .jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
      .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }

  private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    return getAndVerifyProduct("/" + productId, expectedStatus);
  }

  private WebTestClient.BodyContentSpec getAndVerifyProduct(String productIdPath, HttpStatus expectedStatus) {
    return client.get()
      .uri("/product" + productIdPath)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private WebTestClient.BodyContentSpec postAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    Product product = new Product(productId, "Name " + productId, productId, "SA");
    return client.post()
      .uri("/product")
      .body(Mono.just(product), Product.class)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    client.delete()
        .uri("/product/" + productId)
        .exchange()
        .expectStatus().isEqualTo(expectedStatus);
  }

  private StepVerifier.FirstStep<Void> sendCreateProductEvent(int productId) {
    Product product = new Product(productId, "Name " + productId, productId,
        "SA");
    Event<Integer, Product> event = new Event<>(CREATE, productId, product);
    return StepVerifier.create(messageProcessor.apply(Flux.just(event)));
  }
  private StepVerifier.FirstStep<Void> sendDeleteProductEvent(int productId) {
    Event<Integer, Product> event = new Event<>(DELETE, productId, null);
    return StepVerifier.create(messageProcessor.apply(Flux.just(event)));
  }
}
