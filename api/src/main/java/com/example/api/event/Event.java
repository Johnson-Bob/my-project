package com.example.api.event;

import java.time.ZonedDateTime;

public record Event<K, T> (Event.Type eventType, K key, T data, ZonedDateTime eventCreatedAt) {
  public enum Type {CREATE, DELETE}

  public Event(Type eventType, K key, T data) {
    this(eventType, key, data, ZonedDateTime.now());
  }
}
