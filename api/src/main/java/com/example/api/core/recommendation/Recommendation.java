package com.example.api.core.recommendation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Recommendation {
  private int productId;
  private int recommendationId;
  private String author;
  private int rate;
  private String content;
  private String serviceAddress;
}
