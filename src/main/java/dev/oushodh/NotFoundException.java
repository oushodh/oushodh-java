package dev.oushodh;

import org.jspecify.annotations.Nullable;

/** 404 {@code not_found}. */
public class NotFoundException extends OushodhException {
  private static final long serialVersionUID = 1L;

  public NotFoundException(String message, @Nullable Integer status, @Nullable String code) {
    super(message, status, code, null);
  }
}
