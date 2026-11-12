package dev.oushodh;

import org.jspecify.annotations.Nullable;

/** 400 {@code bad_request}: a bad parameter. */
public class BadRequestException extends OushodhException {
  private static final long serialVersionUID = 1L;

  public BadRequestException(String message, @Nullable Integer status, @Nullable String code) {
    super(message, status, code, null);
  }
}
