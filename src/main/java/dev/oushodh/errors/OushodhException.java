package dev.oushodh.errors;

import org.jspecify.annotations.Nullable;

/**
 * The base of every exception the SDK throws.
 *
 * <p>{@link #getStatus()} is the HTTP status and {@link #getCode()} the API's error code, such as
 * {@code not_found}. Both are {@code null} when no answer came back. Match on the code or the
 * class, never on the message: it is for people and may change.
 */
public class OushodhException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final @Nullable Integer status;
  private final @Nullable String code;

  public OushodhException(String message) {
    this(message, null, null, null);
  }

  public OushodhException(
      String message, @Nullable Integer status, @Nullable String code, @Nullable Throwable cause) {
    super(message, cause);
    this.status = status;
    this.code = code;
  }

  /** The HTTP status, or {@code null} when no answer came back. */
  public @Nullable Integer getStatus() {
    return status;
  }

  /** The API's error code, or {@code null} when the answer had none. */
  public @Nullable String getCode() {
    return code;
  }
}
