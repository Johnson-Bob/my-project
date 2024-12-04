package com.example.microservices.core.recommendation.services;

import com.example.api.core.recommendation.Recommendation;
import com.example.microservices.core.recommendation.persistence.RecommendationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {
  @Mappings({
      @Mapping(target = "id", ignore = true),
      @Mapping(target = "version", ignore = true),
      @Mapping(target = "rating", source = "api.rate")
  })
  RecommendationEntity apiToEntity(Recommendation api);

  @Mappings({
      @Mapping(target = "serviceAddress", ignore = true),
      @Mapping(target = "rate", source = "entity.rating")
  })
  Recommendation entityToApi(RecommendationEntity entity);
}
