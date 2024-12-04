package com.example.microservices.core.product.persistence;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataMongoTest
class PersistenceTests extends MongoDbTestBase {

  @Autowired
  private ProductRepository repository;

  private ProductEntity savedEntity;

  @BeforeEach
  void setupDb() {
    Mono<ProductEntity> productEntityMono = repository.deleteAll()
        .thenReturn(new ProductEntity(1, "n", 1))
        .flatMap(repository::save)
        .doOnSuccess(entity -> savedEntity = entity);
    StepVerifier.create(productEntityMono)
        .assertNext(entity -> assertEqualsProduct(savedEntity, entity))
        .verifyComplete();
  }


  @Test
  void create() {
    ProductEntity newEntity = new ProductEntity(2, "n", 2);

    StepVerifier.create(repository.save(newEntity).then(repository.count()))
        .expectNext(2L)
        .verifyComplete();

    StepVerifier.create(repository.findById(newEntity.getId()))
        .assertNext(found -> assertEqualsProduct(newEntity, found))
        .verifyComplete();
  }

  @Test
  void update() {
    savedEntity.setName("n2");
    StepVerifier.create(repository.save(savedEntity).then(repository.findById(savedEntity.getId())))
        .assertNext(entity -> Assertions.assertThat(entity)
            .extracting("version", "name")
            .containsExactly(1, "n2"))
        .verifyComplete();
  }

  @Test
  void delete() {
    StepVerifier.create(repository.delete(savedEntity).then(repository.existsById(savedEntity.getId())))
        .expectNext(Boolean.FALSE)
        .verifyComplete();
  }

  @Test
  void getByProductId() {
    StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
        .assertNext(found -> assertEqualsProduct(savedEntity, found))
        .verifyComplete();
  }

  @Test
  void duplicateError() {
    ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
    StepVerifier.create(repository.save(entity))
        .expectError(DuplicateKeyException.class)
        .verify();
  }

  @Test
  void optimisticLockError() {
    // Store the saved entity in two separate entity objects
    ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
    ProductEntity entity2 = repository.findById(savedEntity.getId()).block();
    assertNotNull(entity1);
    assertNotNull(entity2);

    // Update the entity using the first entity object
    entity1.setName("n1");
    StepVerifier
        .create(repository.save(entity1))
        .assertNext(saved -> Assertions.assertThat(saved).extracting("name").isEqualTo("n1"))
        .verifyComplete();

    // Update the entity using the second entity object.
    // This should fail since the second entity now holds an old version number, i.e. an Optimistic Lock Error
    entity2.setName("n2");
    StepVerifier
        .create(repository.save(entity2))
        .expectError(OptimisticLockingFailureException.class)
        .verify();

    // Get the updated entity from the database and verify its new state
    StepVerifier.create(repository.findById(savedEntity.getId()))
        .assertNext(e ->
            Assertions.assertThat(e).extracting("version", "name").containsExactly(1, "n1"))
        .verifyComplete();
  }

  private void assertEqualsProduct(ProductEntity expectedEntity, ProductEntity actualEntity) {
    Assertions.assertThat(actualEntity).usingRecursiveComparison().isEqualTo(expectedEntity);
  }
}
