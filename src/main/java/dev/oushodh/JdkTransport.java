package dev.oushodh;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;

/** A {@link Transport} on {@link HttpClient}, the JDK's own HTTP client. */
public final class JdkTransport implements Transport {
  private final HttpClient http;

  /** A transport on a new {@link HttpClient}. */
  public JdkTransport() {
    this(HttpClient.newHttpClient());
  }

  /** A transport on your own {@link HttpClient}, for example one with a proxy. */
  public JdkTransport(HttpClient http) {
    this.http = http;
  }

  @Override
  public Response get(URI uri, Map<String, String> headers, Duration timeout)
      throws IOException, InterruptedException {
    HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(timeout).GET();

    headers.forEach(request::header);

    try {
      HttpResponse<byte[]> response =
          http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());

      return new Response(response.statusCode(), response.headers().map(), response.body());
    } catch (HttpTimeoutException e) {
      SocketTimeoutException timedOut = new SocketTimeoutException(e.getMessage());

      timedOut.initCause(e);

      throw timedOut;
    }
  }
}
