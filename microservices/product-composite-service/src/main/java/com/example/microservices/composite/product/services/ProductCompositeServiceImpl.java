package com.example.microservices.composite.product.services;

import com.example.api.composite.*;
import com.example.api.core.product.Product;
import com.example.api.core.recommendation.Recommendation;
import com.example.api.core.review.Review;
import com.example.util.http.ServiceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductCompositeServiceImpl implements ProductCompositeService {
  private final ProductCompositeIntegration integration;
  private final ServiceUtil serviceUtil;

  @Override
  public Mono<ProductAggregate> getCompositeProduct(int productId) {
    return Mono.zip(values -> createProductAggregate(
            (Product) values[0],
            (List<Recommendation>) values[1],
            (List<Review>) values[2],
            serviceUtil.getServiceAddress()
          ),
          integration.getProduct(productId),
          integration.getRecommendations(productId).collectList(),
          integration.getReviews(productId).collectList())
        .doOnError(ex -> log.warn("getCompositeProduct failed: {}", ex.toString()))
        .log(log.getName(), Level.FINE);
  }

  @Override
  public Mono<Void> createCompositeProduct(ProductAggregate body) {
    try {
      List<Mono<?>> monoList = new ArrayList<>();

      log.info("Will create a new composite entity for product.id: {}", body.productId());
      Product product = new Product().setProductId(body.productId()).setName(body.name()).setWeight(body.weight());
      monoList.add(integration.createProduct(product));

      Optional.ofNullable(body.recommendations()).orElseGet(List::of).stream()
          .map(rs -> new Recommendation()
              .setProductId(product.getProductId())
              .setRecommendationId(rs.recommendationId())
              .setAuthor(rs.author())
              .setRate(rs.rate())
              .setContent(rs.content()))
          .forEach(r -> monoList.add(integration.createRecommendation(r)));

      Optional.ofNullable(body.reviews()).orElseGet(List::of).stream()
          .map(rs -> new Review()
              .setProductId(product.getProductId())
              .setReviewId(rs.reviewId())
              .setAuthor(rs.author())
              .setSubject(rs.subject())
              .setContent(rs.content()))
          .forEach(r -> monoList.add(integration.createReview(r)));

      log.debug("createCompositeProduct: composite entities created for productId: {}", body.productId());
      return Mono.zip(monoList, o -> "")
          .doOnError(e -> log.warn("createCompositeProduct failed", e))
          .then();
    } catch (RuntimeException e) {
      log.warn("createCompositeProduct failed", e);
      throw e;
    }
  }

  @Override
  public Mono<Void> deleteCompositeProduct(int productId) {
    integration.deleteRecommendations(productId);
    integration.deleteReviews(productId);
    integration.deleteProduct(productId);
    return null;
  }

  private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations, List<Review> reviews, String compositeAddress) {
    List<RecommendationSummary> recommendationSummaryList = recommendations.stream()
        .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
        .toList();
    List<ReviewSummary> reviewSummaryList = reviews.stream()
        .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
        .toList();
    String reviewAddress = reviews.isEmpty() ? "" : reviews.getFirst().getServiceAddress();
    String recommendationAddress = recommendations.isEmpty() ? "" : recommendations.getFirst().getServiceAddress();
    ServiceAddresses serviceAddress = new ServiceAddresses(compositeAddress, product.getServiceAddress(), reviewAddress, recommendationAddress);
    return new ProductAggregate(product.getProductId(), product.getName(), product.getWeight(), recommendationSummaryList, reviewSummaryList, serviceAddress);
  }
}
