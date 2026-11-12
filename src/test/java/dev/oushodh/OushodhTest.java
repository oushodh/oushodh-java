package dev.oushodh;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OushodhTest {
  private static final String KEY = "oush_test_123";

  private static final String MEDICINE =
      """
      {
        "id": "MED-012933",
        "uuid": "01a1ba6f-f4d6-7143-ac84-9376f520eef8",
        "brand": "Napa",
        "category": null,
        "product_name": "Napa 500 mg Tablet",
        "product_image": "https://api.oushodh.dev/v1/images/abc.jpg",
        "images": ["https://api.oushodh.dev/v1/images/abc.jpg"],
        "unit": "Strip",
        "size": "10 Tablets",
        "price": 1.2,
        "price_per": "unit",
        "price_source": "dgda_price",
        "unit_prices": [{"size": "15 gm in Tube Pack", "price": 3.5}],
        "prices": {
          "currency": "BDT",
          "unit": {"amount": 1.2, "source": "dgda_price"},
          "sizes": [{"size": null, "amount": 3.5, "source": "dgda_price"}],
          "strip": {"amount": 12, "units": 10, "source": "dgda_price", "strip_note": "new"},
          "packs": [{"amount": 120, "units": null, "label": "100's pack", "source": "dgda_price"}]
        },
        "description_en": null,
        "description_bn": null,
        "dosage_form": "Tablet",
        "strength": "500 mg",
        "generic_name": "Paracetamol",
        "manufacturer": "Beximco Pharmaceuticals PLC.",
        "source_url": null,
        "last_updated": "2026-11-09T15:27:34Z",
        "dgda_listed": true,
        "medicine_type": "allopathic",
        "added_later": {"kept": true}
      }
      """;

  private HttpServer server;
  private ExecutorService executor;
  private final Deque<Reply> replies = new ConcurrentLinkedDeque<>();
  private final List<Request> requests = new CopyOnWriteArrayList<>();

  private record Reply(int status, byte[] body, Map<String, String> headers, long delayMillis) {
    static Reply json(int status, String body) {
      return new Reply(status, body.getBytes(StandardCharsets.UTF_8), Map.of(), 0);
    }

    static Reply error(int status, String code) {
      return json(
          status, "{\"error\":{\"code\":\"" + code + "\",\"message\":\"Says " + code + "\"}}");
    }

    Reply header(String name, String value) {
      return new Reply(status, body, Map.of(name, value), delayMillis);
    }
  }

  private record Request(String path, String query, Headers headers) {}

  @BeforeEach
  void start() throws IOException {
    executor = Executors.newCachedThreadPool();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(executor);
    server.createContext(
        "/",
        exchange -> {
          requests.add(
              new Request(
                  exchange.getRequestURI().getRawPath(),
                  exchange.getRequestURI().getRawQuery(),
                  exchange.getRequestHeaders()));

          Reply reply = replies.isEmpty() ? Reply.error(500, "internal") : replies.poll();

          sleep(reply.delayMillis());
          reply.headers().forEach(exchange.getResponseHeaders()::add);
          exchange.sendResponseHeaders(reply.status(), reply.body().length);

          try (OutputStream body = exchange.getResponseBody()) {
            body.write(reply.body());
          }
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
    executor.shutdownNow();
  }

  private Oushodh client() {
    return client(2);
  }

  private Oushodh client(int maxRetries) {
    return Oushodh.builder()
        .apiKey(KEY)
        .baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/")
        .maxRetries(maxRetries)
        .build();
  }

  private static String page(String next, String... ids) {
    List<String> items = new ArrayList<>();

    for (String id : ids) {
      items.add(MEDICINE.replace("MED-012933", id));
    }

    String cursor = next == null ? "null" : "\"" + next + "\"";

    return "{\"items\":[" + String.join(",", items) + "],\"next\":" + cursor + ",\"total\":3}";
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void metaSendsTheKeyAndReadsTheAccount() {
    replies.add(
        Reply.json(
            200,
            "{\"client\":\"Acme\",\"id_profile\":\"MED\",\"medicines\":58937,"
                + "\"last_import\":\"2026-11-09T15:27:40Z\"}"));

    Meta meta = client().meta();
    Request request = requests.get(0);

    assertEquals("/v1/meta", request.path());
    assertNull(request.query());
    assertEquals("Bearer " + KEY, request.headers().getFirst("Authorization"));
    assertEquals("application/json", request.headers().getFirst("Accept"));
    assertEquals(
        "oushodh-java/" + System.getProperty("oushodh.version"),
        request.headers().getFirst("User-Agent"));
    assertEquals("Acme", meta.getClient());
    assertEquals("MED", meta.getIdProfile());
    assertEquals(58937, meta.getMedicines());
    assertEquals(Instant.parse("2026-11-09T15:27:40Z"), meta.getLastImport());
  }

  @Test
  void listMedicinesSendsEveryParameterAndReadsTheMedicine() {
    replies.add(Reply.json(200, page(null, "MED-012933")));

    Page result =
        client()
            .listMedicines(
                new MedicineQuery()
                    .limit(500)
                    .cursor("abc")
                    .updatedSince(Instant.parse("2026-11-08T09:30:00.789Z"))
                    .q("napa 500"));
    Request request = requests.get(0);
    Medicine medicine = result.getItems().get(0);

    assertEquals("/v1/medicines", request.path());
    assertEquals(
        "limit=500&cursor=abc&updated_since=2026-11-08T09%3A30%3A00Z&q=napa%20500",
        request.query());
    assertNull(result.getNext());
    assertEquals(3, result.getTotal());
    assertEquals("MED-012933", medicine.getId());
    assertEquals("Napa 500 mg Tablet", medicine.getProductName());
    assertNull(medicine.getCategory());
    assertEquals(new BigDecimal("1.2"), medicine.getPrice());
    assertEquals(new BigDecimal("3.5"), medicine.getUnitPrices().get(0).getPrice());
    assertEquals("BDT", medicine.getPrices().getCurrency());
    assertEquals(new BigDecimal("1.2"), medicine.getPrices().getUnit().getAmount());
    assertNull(medicine.getPrices().getSizes().get(0).getSize());
    assertEquals(new BigDecimal("10"), medicine.getPrices().getStrip().getUnits());
    assertEquals("100's pack", medicine.getPrices().getPacks().get(0).getLabel());
    assertNull(medicine.getPrices().getPacks().get(0).getUnits());
    assertEquals(Instant.parse("2026-11-09T15:27:34Z"), medicine.getLastUpdated());
    assertTrue(medicine.isDgdaListed());
    assertEquals("allopathic", medicine.getMedicineType());
    assertEquals(Map.of("kept", true), medicine.getExtra().get("added_later"));
    assertEquals("new", medicine.getPrices().getStrip().getExtra().get("strip_note"));
  }

  @Test
  void medicinesPagesToTheEnd() {
    replies.add(Reply.json(200, page("c1", "MED-1", "MED-2")));
    replies.add(Reply.json(200, page("c2")));
    replies.add(Reply.json(200, page(null, "MED-3")));

    List<String> ids = new ArrayList<>();

    for (Medicine medicine : client().medicines(new MedicineQuery().limit(2).q("napa"))) {
      ids.add(medicine.getId());
    }

    assertEquals(List.of("MED-1", "MED-2", "MED-3"), ids);
    assertEquals(3, requests.size());
    assertEquals("limit=2&q=napa", requests.get(0).query());
    assertEquals("limit=2&cursor=c1&q=napa", requests.get(1).query());
    assertEquals("limit=2&cursor=c2&q=napa", requests.get(2).query());
  }

  @Test
  void searchSendsQAndLimit() {
    replies.add(Reply.json(200, "{\"items\":[" + MEDICINE + "],\"total\":41}"));

    SearchResult result = client().search("napa 500", 5);

    assertEquals("/v1/medicines/search", requests.get(0).path());
    assertEquals("q=napa%20500&limit=5", requests.get(0).query());
    assertEquals(41, result.getTotal());
    assertEquals("Napa", result.getItems().get(0).getBrand());
  }

  @Test
  void getMedicineEscapesTheRefAndReadsTheMonograph() {
    String detail =
        MEDICINE.replace(
            "\"added_later\"",
            "\"monograph\": {\"source\": \"label\", \"indications\": \"Fever\","
                + " \"composition\": null, \"pharmacology\": null, \"dosage\": null,"
                + " \"interactions\": null, \"contraindications\": null, \"side_effects\": \"Rash\","
                + " \"pregnancy_lactation\": null, \"precautions\": null, \"overdose\": null,"
                + " \"storage\": null}, \"added_later\"");

    replies.add(Reply.json(200, detail));
    replies.add(Reply.json(200, MEDICINE.replace("\"added_later\"", "\"monograph\": null, \"x\"")));

    MedicineDetail medicine = client().getMedicine("MED/1 2?");
    MedicineDetail bare = client().getMedicine("MED-012933");

    assertEquals("/v1/medicines/MED%2F1%202%3F", requests.get(0).path());
    assertEquals("Fever", medicine.getMonograph().getIndications());
    assertEquals("Rash", medicine.getMonograph().getSideEffects());
    assertNull(bare.getMonograph());
  }

  @Test
  void imagesNeedNoKey() {
    byte[] jpeg = {(byte) 0xff, (byte) 0xd8, 1, 2};

    replies.add(new Reply(200, jpeg, Map.of("Content-Type", "image/jpeg"), 0));

    Oushodh client = client();

    assertArrayEquals(jpeg, client.getImage("abc.jpg"));
    assertEquals("/v1/images/abc.jpg", requests.get(0).path());
    assertNull(requests.get(0).headers().getFirst("Authorization"));
    assertTrue(client.imageUrl("a/b.jpg").endsWith("/v1/images/a%2Fb.jpg"));
  }

  @ParameterizedTest
  @CsvSource({
    "400, bad_request, dev.oushodh.BadRequestException",
    "401, unauthorized, dev.oushodh.AuthenticationException",
    "403, plan_expired, dev.oushodh.PermissionDeniedException",
    "403, plan_limit, dev.oushodh.PermissionDeniedException",
    "404, not_found, dev.oushodh.NotFoundException",
    "429, rate_limited, dev.oushodh.RateLimitException",
    "500, internal, dev.oushodh.InternalServerException",
    "409, internal, dev.oushodh.OushodhException",
  })
  void errorsHaveAClassPerFamily(int status, String code, String type) throws Exception {
    replies.add(Reply.error(status, code).header("Retry-After", "7"));

    OushodhException error = assertThrows(OushodhException.class, () -> client(0).meta());

    assertEquals(Class.forName(type), error.getClass());
    assertEquals(status, error.getStatus());
    assertEquals(code, error.getCode());
    assertEquals("Says " + code, error.getMessage());

    if (error instanceof RateLimitException rateLimit) {
      assertEquals(7L, rateLimit.getRetryAfter());
    }
  }

  @Test
  void rateLimitedIsRetriedAfterRetryAfter() {
    replies.add(Reply.error(429, "rate_limited").header("Retry-After", "1"));
    replies.add(Reply.json(200, page(null)));

    long started = System.nanoTime();

    client().listMedicines();

    assertEquals(2, requests.size());
    assertTrue(Duration.ofNanos(System.nanoTime() - started).toMillis() >= 1000);
  }

  @Test
  void quotaExceededIsNotRetried() {
    replies.add(Reply.error(429, "quota_exceeded").header("Retry-After", "1728000"));

    RateLimitException error = assertThrows(RateLimitException.class, () -> client().meta());

    assertEquals(1, requests.size());
    assertEquals("quota_exceeded", error.getCode());
    assertEquals(1728000L, error.getRetryAfter());
  }

  @Test
  void serverErrorIsRetriedThenSucceeds() {
    replies.add(Reply.error(503, "internal"));
    replies.add(Reply.json(200, "{\"items\":[],\"total\":0}"));

    assertEquals(0, client().search("napa").getTotal());
    assertEquals(2, requests.size());
  }

  @Test
  void serverErrorStopsAfterMaxRetries() {
    assertThrows(InternalServerException.class, () -> client(1).meta());
    assertEquals(2, requests.size());
  }

  @Test
  void notFoundIsNotRetried() {
    replies.add(Reply.error(404, "not_found"));

    assertThrows(NotFoundException.class, () -> client().getMedicine("MED-000000"));
    assertEquals(1, requests.size());
  }

  @Test
  void anErrorThatIsNotJsonHasNoCode() {
    replies.add(
        new Reply(502, "<html>Bad gateway</html>".getBytes(StandardCharsets.UTF_8), Map.of(), 0));

    OushodhException error = assertThrows(InternalServerException.class, () -> client(0).meta());

    assertNull(error.getCode());
    assertEquals("HTTP 502", error.getMessage());
  }

  @Test
  void aNetworkFailureIsAConnectionError() throws IOException {
    int port;

    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }

    Oushodh client =
        Oushodh.builder().apiKey(KEY).baseUrl("http://127.0.0.1:" + port).maxRetries(0).build();
    ApiConnectionException error = assertThrows(ApiConnectionException.class, client::meta);

    assertNull(error.getStatus());
    assertKeyNotIn(error);
  }

  @Test
  void aSlowAnswerIsATimeout() {
    replies.add(new Reply(200, "{}".getBytes(StandardCharsets.UTF_8), Map.of(), 2000));

    Oushodh client =
        Oushodh.builder()
            .apiKey(KEY)
            .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
            .timeout(Duration.ofMillis(200))
            .maxRetries(0)
            .build();

    assertKeyNotIn(assertThrows(ApiTimeoutException.class, client::meta));
  }

  @Test
  void theKeyIsNeverInAnError() {
    replies.add(Reply.error(401, "unauthorized"));

    assertKeyNotIn(assertThrows(AuthenticationException.class, () -> client().meta()));
  }

  @Test
  void aTransportCanBeInjected() {
    List<String> uris = new ArrayList<>();
    Oushodh client =
        Oushodh.builder()
            .apiKey(KEY)
            .transport((uri, headers, timeout) -> {
              uris.add(uri.toString());

              return new Transport.Response(
                  200,
                  Map.of("content-type", List.of("application/json")),
                  "{\"items\":[],\"total\":0}".getBytes(StandardCharsets.UTF_8));
            })
            .build();

    client.search("napa");

    assertEquals(List.of("https://api.oushodh.dev/v1/medicines/search?q=napa"), uris);
  }

  @Test
  void noKeyFailsAtBuild() {
    Assumptions.assumeTrue(System.getenv("OUSHODH_API_KEY") == null);

    OushodhException error = assertThrows(OushodhException.class, () -> Oushodh.builder().build());

    assertTrue(error.getMessage().contains("OUSHODH_API_KEY"));
  }

  private static void assertKeyNotIn(Throwable error) {
    StringWriter trace = new StringWriter();

    error.printStackTrace(new PrintWriter(trace));

    assertFalse(trace.toString().contains(KEY));
    assertInstanceOf(OushodhException.class, error);
  }
}
