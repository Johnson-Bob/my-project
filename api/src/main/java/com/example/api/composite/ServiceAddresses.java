package com.example.api.composite;

public record ServiceAddresses(
    String compositeAddress,
    String productAddress,
    String reviewAddress,
    String recommendationAddress) {}