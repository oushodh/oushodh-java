package dev.oushodh;

import org.jspecify.annotations.Nullable;

/**
 * 429 {@code rate_limited} or {@code quota_exceeded}. {@link #getRetryAfter()} is the seconds to
 * wait. For {@code quota_exceeded} it is the seconds to the start of next month.
 */
public class RateLimitException extends OushodhException {
  private static final long serialVersionUID = 1L;

  private final @Nullable Long retryAfter;

  public RateLimitException(
      String message, @Nullable Integer status, @Nullable String code, @Nullable Long retryAfter) {
    super(message, status, code, null);
    this.retryAfter = retryAfter;
  }

  /** Seconds from the {@code Retry-After} header, or {@code null} when it had none. */
  public @Nullable Long getRetryAfter() {
    return retryAfter;
  }
}
