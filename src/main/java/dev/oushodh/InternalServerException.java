package dev.oushodh;

import org.jspecify.annotations.Nullable;

/** 5xx, such as {@code internal}. */
public class InternalServerException extends OushodhException {
  private static final long serialVersionUID = 1L;

  public InternalServerException(String message, @Nullable Integer status, @Nullable String code) {
    super(message, status, code, null);
  }
}
