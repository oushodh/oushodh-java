package dev.oushodh;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Sends one GET request. The default is {@link JdkTransport}, which uses {@code java.net.http}.
 * Android has no {@code java.net.http}, so pass your own, for example one built on OkHttp.
 *
 * <p>Throw an {@link java.io.InterruptedIOException}, such as {@link
 * java.net.SocketTimeoutException}, when the request times out, and any other {@link IOException}
 * when the network fails. Return every answer that came back, whatever its status.
 */
@FunctionalInterface
public interface Transport {
  Response get(URI uri, Map<String, String> headers, Duration timeout)
      throws IOException, InterruptedException;

  /** The answer: its status, its headers by name, and its body. */
  record Response(int status, Map<String, List<String>> headers, byte[] body) {
    /** The first value of a header, matched without regard to case. */
    public @Nullable String header(String name) {
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        if (entry.getKey().equalsIgnoreCase(name) && !entry.getValue().isEmpty()) {
          return entry.getValue().get(0);
        }
      }

      return null;
    }
  }
}
