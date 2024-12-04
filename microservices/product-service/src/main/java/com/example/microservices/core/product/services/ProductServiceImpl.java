package com.example.microservices.core.product.services;

import com.example.api.core.product.Product;
import com.example.api.core.product.ProductService;
import com.example.api.exception.InvalidInputException;
import com.example.api.exception.NotFoundException;
import com.example.microservices.core.product.persistence.ProductRepository;
import com.example.util.http.ServiceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.logging.Level;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
  private final ServiceUtil serviceUtil;
  private final ProductRepository repository;
  private final ProductMapper mapper;

  @Override
  public Mono<Product> getProduct(int productId) {
    log.debug("/product return the found product for productId={}", productId);
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
    return repository.findByProductId(productId)
        .log(log.getName(), Level.FINE)
        .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
        .map(mapper::entityToApi)
        .map(p -> p.setServiceAddress(serviceUtil.getServiceAddress()));
  }

  @Override
  public Mono<Product> createProduct(Product body) {
    Integer productId = Optional.ofNullable(body)
        .map(Product::getProductId)
        .orElseThrow(() -> new InvalidInputException("The new product is null"));
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }
    log.debug("createProduct: tries to create an entity with productId: {}", productId);
    return Mono.just(body)
        .map(mapper::apiToEntity)
        .flatMap(repository::save)
        .onErrorMap(DuplicateKeyException.class,
            e -> new InvalidInputException("Duplicate key, Product Id: " + productId))
        .log(log.getName(), Level.FINE)
        .map(entity -> {
          log.info("Saved Entity: {}", entity);
          return mapper.entityToApi(entity);
        });
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
    log.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
    return repository.findByProductId(productId).log(log.getName(), Level.FINE).map(repository::delete).flatMap(v -> v);
  }
}
