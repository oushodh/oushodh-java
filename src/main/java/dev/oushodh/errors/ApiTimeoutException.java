package dev.oushodh.errors;

/** No answer came back in time. */
public class ApiTimeoutException extends ApiConnectionException {
  private static final long serialVersionUID = 1L;

  public ApiTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
