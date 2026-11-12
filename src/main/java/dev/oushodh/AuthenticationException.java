package dev.oushodh;

import org.jspecify.annotations.Nullable;

/** 401 {@code unauthorized}: no key, an unknown key or a revoked key. */
public class AuthenticationException extends OushodhException {
  private static final long serialVersionUID = 1L;

  public AuthenticationException(String message, @Nullable Integer status, @Nullable String code) {
    super(message, status, code, null);
  }
}
