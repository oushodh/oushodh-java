package dev.oushodh.errors;

/** No answer came back: the network failed. */
public class ApiConnectionException extends OushodhException {
  private static final long serialVersionUID = 1L;

  public ApiConnectionException(String message, Throwable cause) {
    super(message, null, null, cause);
  }
}
