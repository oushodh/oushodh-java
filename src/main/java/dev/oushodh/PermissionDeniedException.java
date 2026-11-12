package dev.oushodh;

import org.jspecify.annotations.Nullable;

/** 403 {@code plan_expired} or {@code plan_limit}. */
public class PermissionDeniedException extends OushodhException {
  private static final long serialVersionUID = 1L;

  public PermissionDeniedException(
      String message, @Nullable Integer status, @Nullable String code) {
    super(message, status, code, null);
  }
}
