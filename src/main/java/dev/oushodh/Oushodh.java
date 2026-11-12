package dev.oushodh;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import dev.oushodh.errors.ApiConnectionException;
import dev.oushodh.errors.ApiTimeoutException;
import dev.oushodh.errors.AuthenticationException;
import dev.oushodh.errors.BadRequestException;
import dev.oushodh.errors.InternalServerException;
import dev.oushodh.errors.NotFoundException;
import dev.oushodh.errors.OushodhException;
import dev.oushodh.errors.PermissionDeniedException;
import dev.oushodh.errors.RateLimitException;
import dev.oushodh.http.JdkTransport;
import dev.oushodh.http.Transport;
import dev.oushodh.models.Medicine;
import dev.oushodh.models.MedicineDetail;
import dev.oushodh.models.Meta;
import dev.oushodh.models.Page;
import dev.oushodh.models.SearchResult;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Oushodh API client. Build one with {@link #builder()}. It is safe to share between threads.
 *
 * <pre>{@code
 * Oushodh client = Oushodh.builder().apiKey("oush_...").build();
 * SearchResult result = client.search("napa 500");
 * }</pre>
 */
public final class Oushodh {
  /** The version of this SDK. */
  public static final String VERSION = "0.1.0";

  /** The address of the API. */
  public static final String DEFAULT_BASE_URL = "https://api.oushodh.dev";

  private static final String USER_AGENT = "oushodh-java/" + VERSION;

  private static final JsonMapper JSON =
      JsonMapper.builder()
          .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
          .changeDefaultVisibility(checker ->
              checker
                  .withFieldVisibility(Visibility.ANY)
                  .withGetterVisibility(Visibility.NONE)
                  .withIsGetterVisibility(Visibility.NONE)
                  .withSetterVisibility(Visibility.NONE))
          .disable(MapperFeature.USE_GETTERS_AS_SETTERS)
          .build();

  private final String apiKey;
  private final String baseUrl;
  private final Duration timeout;
  private final int maxRetries;
  private final Transport transport;

  private Oushodh(Builder builder) {
    String key = builder.apiKey;

    if (key == null || key.isBlank()) {
      key = System.getenv("OUSHODH_API_KEY");
    }

    if (key == null || key.isBlank()) {
      throw new OushodhException(
          "No API key: pass apiKey(...) or set the OUSHODH_API_KEY environment variable");
    }

    if (builder.maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries must be 0 or more");
    }

    this.apiKey = key;
    this.baseUrl = builder.baseUrl.replaceAll("/+$", "");
    this.timeout = builder.timeout;
    this.maxRetries = builder.maxRetries;
    this.transport = builder.transport != null ? builder.transport : new JdkTransport();
  }

  /** A new builder. With no key, {@link Builder#build()} reads {@code OUSHODH_API_KEY}. */
  public static Builder builder() {
    return new Builder();
  }

  /** The account the key belongs to. Use it to check that a key works. */
  public Meta meta() {
    return getJson("/v1/meta", Map.of(), Meta.class);
  }

  /** The first page of medicines, with the API's defaults. */
  public Page listMedicines() {
    return listMedicines(new MedicineQuery());
  }

  /** One page of medicines, in the order of their permanent number. */
  public Page listMedicines(MedicineQuery query) {
    return listPage(query, query.getCursor());
  }

  /** Every medicine, page after page to the end. */
  public Iterable<Medicine> medicines() {
    return medicines(new MedicineQuery());
  }

  /**
   * Every medicine that matches the query, page after page to the end. Each page is fetched as the
   * loop reaches it. Each new loop starts again from the query's cursor.
   */
  public Iterable<Medicine> medicines(MedicineQuery query) {
    return () -> new Pages(query);
  }

  /** The best matches for what a person typed, best match first. */
  public SearchResult search(String q) {
    return search(q, null);
  }

  /** At most {@code limit} (1 to 50) best matches for what a person typed, best match first. */
  public SearchResult search(String q, @Nullable Integer limit) {
    Map<String, String> params = new LinkedHashMap<>();

    params.put("q", q);

    if (limit != null) {
      params.put("limit", limit.toString());
    }

    return getJson("/v1/medicines/search", params, SearchResult.class);
  }

  /** One medicine by its ID in any profile, an old ID, or its UUID. */
  public MedicineDetail getMedicine(String ref) {
    return getJson("/v1/medicines/" + encodePath(ref), Map.of(), MedicineDetail.class);
  }

  /** The address of a picture, as {@code productImage} gives it. It needs no key. */
  public String imageUrl(String file) {
    return baseUrl + "/v1/images/" + encodePath(file);
  }

  /** The bytes of a picture, by its file name. The request carries no key. */
  public byte[] getImage(String file) {
    return send(URI.create(imageUrl(file)), false).body();
  }

  private Page listPage(MedicineQuery query, @Nullable String cursor) {
    Map<String, String> params = new LinkedHashMap<>();
    Instant since = query.getUpdatedSince();

    putIfSet(params, "limit", query.getLimit());
    putIfSet(params, "cursor", cursor);

    if (since != null) {
      params.put("updated_since", since.truncatedTo(ChronoUnit.SECONDS).toString());
    }

    putIfSet(params, "q", query.getQ());

    return getJson("/v1/medicines", params, Page.class);
  }

  private <T> T getJson(String path, Map<String, String> params, Class<T> type) {
    Transport.Response response = send(URI.create(baseUrl + path + queryString(params)), true);

    try {
      return JSON.readValue(response.body(), type);
    } catch (JacksonException e) {
      throw new OushodhException(
          "The API's answer is not the JSON this SDK expects", response.status(), null, e);
    }
  }

  private Transport.Response send(URI uri, boolean auth) {
    Map<String, String> headers = new LinkedHashMap<>();

    headers.put("User-Agent", USER_AGENT);

    if (auth) {
      headers.put("Authorization", "Bearer " + apiKey);
      headers.put("Accept", "application/json");
    }

    for (int attempt = 0; ; attempt++) {
      OushodhException error;

      try {
        Transport.Response response = transport.get(uri, headers, timeout);

        if (response.status() >= 200 && response.status() < 300) {
          return response;
        }

        error = apiError(response);
      } catch (InterruptedIOException e) {
        error = new ApiTimeoutException("The request timed out", e);
      } catch (IOException e) {
        error = new ApiConnectionException("Could not reach the API: " + e.getClass().getName(), e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();

        throw new ApiConnectionException("The request was interrupted", e);
      }

      long delay = delayMillis(error, attempt);

      if (delay < 0) {
        throw error;
      }

      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();

        throw error;
      }
    }
  }

  private long delayMillis(OushodhException error, int attempt) {
    if (attempt >= maxRetries) {
      return -1;
    }

    // quota_exceeded waits until next month, so only rate_limited is retried.
    if (error instanceof RateLimitException rateLimit) {
      if (!"rate_limited".equals(rateLimit.getCode())) {
        return -1;
      }

      Long retryAfter = rateLimit.getRetryAfter();

      return retryAfter != null ? retryAfter * 1000 : backoffMillis(attempt);
    }

    if (error instanceof InternalServerException || error instanceof ApiConnectionException) {
      return backoffMillis(attempt);
    }

    return -1;
  }

  private static long backoffMillis(int attempt) {
    double base = Math.min(500 * Math.pow(2, attempt), 8000);

    return (long) (base * ThreadLocalRandom.current().nextDouble(0.5, 1.0));
  }

  private static OushodhException apiError(Transport.Response response) {
    int status = response.status();
    String code = null;
    String message = "HTTP " + status;

    try {
      JsonNode error = JSON.readTree(response.body()).path("error");

      if (error.path("code").isString()) {
        code = error.path("code").stringValue();
      }

      if (error.path("message").isString()) {
        message = error.path("message").stringValue();
      }
    } catch (JacksonException e) {
      // Not JSON: keep code null and the message "HTTP <status>".
    }

    if (status == 400) {
      return new BadRequestException(message, status, code);
    }

    if (status == 401) {
      return new AuthenticationException(message, status, code);
    }

    if (status == 403) {
      return new PermissionDeniedException(message, status, code);
    }

    if (status == 404) {
      return new NotFoundException(message, status, code);
    }

    if (status == 429) {
      return new RateLimitException(message, status, code, retryAfter(response));
    }

    if (status >= 500) {
      return new InternalServerException(message, status, code);
    }

    return new OushodhException(message, status, code, null);
  }

  private static @Nullable Long retryAfter(Transport.Response response) {
    String value = response.header("Retry-After");

    if (value == null) {
      return null;
    }

    try {
      return Math.max(0, Long.parseLong(value.trim()));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static void putIfSet(Map<String, String> params, String name, @Nullable Object value) {
    if (value != null) {
      params.put(name, value.toString());
    }
  }

  private static String queryString(Map<String, String> params) {
    StringBuilder query = new StringBuilder();

    for (Map.Entry<String, String> param : params.entrySet()) {
      query.append(query.length() == 0 ? '?' : '&');
      query.append(encodePath(param.getKey())).append('=').append(encodePath(param.getValue()));
    }

    return query.toString();
  }

  private static String encodePath(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /** Builds an {@link Oushodh} client. */
  public static final class Builder {
    private @Nullable String apiKey;
    private String baseUrl = DEFAULT_BASE_URL;
    private Duration timeout = Duration.ofSeconds(30);
    private int maxRetries = 2;
    private @Nullable Transport transport;

    private Builder() {}

    /** Your API key. It starts with {@code oush_}. The default is {@code OUSHODH_API_KEY}. */
    public Builder apiKey(@Nullable String apiKey) {
      this.apiKey = apiKey;

      return this;
    }

    /** Where the API is. The default is {@value Oushodh#DEFAULT_BASE_URL}. */
    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;

      return this;
    }

    /** How long to wait for each request. The default is 30 seconds. */
    public Builder timeout(Duration timeout) {
      this.timeout = timeout;

      return this;
    }

    /** Retries after a {@code 429 rate_limited}, a 5xx or a network error. The default is 2. */
    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;

      return this;
    }

    /** How requests are sent. The default is a {@link JdkTransport}. */
    public Builder transport(Transport transport) {
      this.transport = transport;

      return this;
    }

    /** The client. It throws {@link OushodhException} when there is no API key. */
    public Oushodh build() {
      return new Oushodh(this);
    }
  }

  private final class Pages implements Iterator<Medicine> {
    private final MedicineQuery query;
    private @Nullable String cursor;
    private Iterator<Medicine> items = Collections.emptyIterator();
    private boolean last;

    Pages(MedicineQuery query) {
      this.query = query;
      this.cursor = query.getCursor();
    }

    @Override
    public boolean hasNext() {
      while (!items.hasNext() && !last) {
        Page page = listPage(query, cursor);

        items = page.getItems().iterator();
        cursor = page.getNext();
        last = cursor == null;
      }

      return items.hasNext();
    }

    @Override
    public Medicine next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return items.next();
    }
  }
}
