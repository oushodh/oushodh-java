# Oushodh for Java

The official Java and Kotlin SDK for the [Oushodh](https://oushodh.dev) medicine data API: medicine data for
Bangladesh, checked against the DGDA register. Look up a medicine, search by brand or generic, page through the whole
list, and keep a copy in sync.

- Java 17 or newer. It works from Kotlin, and on Android with your own HTTP client (see [Android](#android)).
- One runtime library: [Jackson](https://github.com/FasterXML/jackson) 3 for JSON, plus the
  [JSpecify](https://jspecify.dev/) annotations, so Kotlin knows which values can be `null`.
- Requests go through the JDK's own `java.net.http.HttpClient`.
- A builder-style client with a blocking API. It is safe to share between threads.

The full API reference is at [oushodh.dev/docs](https://oushodh.dev/docs).

## Install

Install from GitHub for now, through [JitPack](https://jitpack.io). Use a release tag, such as `v0.1.0` once it
exists, or `main-SNAPSHOT` for the newest commit.

Gradle (Kotlin DSL):

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.oushodh:oushodh-java:main-SNAPSHOT")
}
```

Maven:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.oushodh</groupId>
  <artifactId>oushodh-java</artifactId>
  <version>main-SNAPSHOT</version>
</dependency>
```

Or build it yourself and install it in your local Maven repository:

```sh
git clone https://github.com/oushodh/oushodh-java.git
cd oushodh-java
./gradlew publishToMavenLocal
```

Then add `mavenLocal()` to your repositories and depend on `dev.oushodh:oushodh:0.1.0`.

Install from Maven Central is coming soon:

```kotlin
implementation("dev.oushodh:oushodh:0.1.0")
```

## Quick start

Make an API key on the Keys page of the member portal at [app.oushodh.dev](https://app.oushodh.dev). A key starts
with `oush_` and is shown once. Keep it on your server: a key inside an app can be copied out of it.

```java
import dev.oushodh.Oushodh;
import dev.oushodh.models.Medicine;
import dev.oushodh.models.SearchResult;

Oushodh client = Oushodh.builder().apiKey("oush_...").build(); // or set OUSHODH_API_KEY and leave out apiKey

SearchResult result = client.search("napa 500", 5);

for (Medicine medicine : result.getItems()) {
    System.out.println(medicine.getId() + " " + medicine.getProductName() + " " + medicine.getPrices().getUnit());
}
```

From Kotlin, the getters read as properties:

```kotlin
val client = Oushodh.builder().build()

for (medicine in client.medicines(MedicineQuery().q("napa"))) {
    println("${medicine.id} ${medicine.productName}")
}
```

## Packages

| Package              | What it holds                                                                             |
| -------------------- | ----------------------------------------------------------------------------------------- |
| `dev.oushodh`        | `Oushodh`, the client, and `MedicineQuery`, the options of a medicine list.               |
| `dev.oushodh.models` | `Medicine`, `MedicineDetail`, `Monograph`, `Prices`, `Page`, `SearchResult` and `Meta`.   |
| `dev.oushodh.errors` | `OushodhException` and one subclass per error family.                                     |
| `dev.oushodh.http`   | `Transport`, the interface that sends a request, and `JdkTransport`, the default.         |

## Configuration

The builder takes these options:

| Option       | Default                   | What it does                                                                         |
| ------------ | ------------------------- | ------------------------------------------------------------------------------------ |
| `apiKey`     | `OUSHODH_API_KEY`         | Your API key. With no key, `build()` throws `OushodhException`.                      |
| `baseUrl`    | `https://api.oushodh.dev` | Where the API is.                                                                    |
| `timeout`    | 30 seconds                | How long to wait for each request, as a `Duration`.                                  |
| `maxRetries` | `2`                       | Retries after a `429 rate_limited`, a `5xx` or a network error. `0` turns them off.  |
| `transport`  | a `JdkTransport`          | How requests are sent. Pass `new JdkTransport(yourHttpClient)` to use your own `HttpClient`. |

```java
Oushodh client = Oushodh.builder()
    .apiKey(System.getenv("OUSHODH_API_KEY"))
    .timeout(Duration.ofSeconds(10))
    .maxRetries(3)
    .build();
```

The client is safe to share between threads. Build one and keep it.

## Methods

| Method                          | Route                          | Returns              |
| ------------------------------- | ------------------------------ | -------------------- |
| `meta()`                        | `GET /v1/meta`                 | `Meta`               |
| `listMedicines(query)`          | `GET /v1/medicines`            | `Page`               |
| `medicines(query)`              | `GET /v1/medicines`, each page | `Iterable<Medicine>` |
| `search(q)`, `search(q, limit)` | `GET /v1/medicines/search`     | `SearchResult`       |
| `getMedicine(ref)`              | `GET /v1/medicines/{ref}`      | `MedicineDetail`     |
| `imageUrl(file)`                | none                           | the picture's address |
| `getImage(file)`                | `GET /v1/images/{file}`        | the picture's bytes  |

Each model has a getter for every field the API documents. Money is a `BigDecimal` in taka (BDT), and times are
`Instant`s. A value no source gives is `null`. Fields the API adds later are kept in `getExtra()`, by their JSON name.

### meta()

The account the key belongs to. Call it to check that a key works.

```java
Meta meta = client.meta();

System.out.println(meta.getClient() + " reads " + meta.getMedicines() + " medicines");
```

### listMedicines(query)

One page of medicines, in the order of their permanent number. `MedicineQuery` takes `limit` (1 to 500, default
100), `cursor`, `updatedSince` and `q`. Leave out what you do not need, or call `listMedicines()` with no query.

```java
Page page = client.listMedicines(new MedicineQuery().limit(50).q("napa"));

System.out.println(page.getTotal() + " match, next cursor " + page.getNext());
```

### medicines(query)

Every medicine that matches the query, page after page to the end. See [Paging](#paging).

```java
for (Medicine medicine : client.medicines(new MedicineQuery().q("paracetamol"))) {
    System.out.println(medicine.getProductName());
}
```

### search(q, limit)

The best matches for what a person typed, best match first. `limit` is 1 to 50.

```java
SearchResult result = client.search("napa extra", 10);

System.out.println(result.getTotal() + " match");
```

### getMedicine(ref)

One medicine with its medical text. `ref` is a medicine's ID in any profile, an old ID, or its UUID. The answer
always carries the medicine's current ID.

```java
MedicineDetail detail = client.getMedicine("MED-012933");
Monograph monograph = detail.getMonograph();

if (monograph != null) {
    System.out.println(monograph.getIndications());
}
```

### imageUrl(file) and getImage(file)

`imageUrl` builds the address of a picture. `getImage` downloads its bytes. Both take the file name, and neither
sends your key. See [Images](#images).

```java
String url = client.imageUrl("abc.jpg");
byte[] jpeg = client.getImage("abc.jpg");
```

## Paging

`listMedicines()` gives one page: `getItems()`, `getNext()` and `getTotal()`. Pass `getNext()` back as `cursor` for
the next page. It is `null` on the last page. A page holds 1 to 500 medicines (`limit`, default 100).

`medicines()` does this for you, page after page to the end. It fetches each page as the loop reaches it:

```java
for (Medicine medicine : client.medicines(new MedicineQuery().limit(500))) {
    save(medicine);
}
```

To get a `Stream`, use `StreamSupport.stream(client.medicines(query).spliterator(), false)`.

Search has no cursor. To page every match, use `medicines(new MedicineQuery().q(...))`.

## Sync

Copy the whole database once, then fetch only what changed.

1. The first sync pages through `medicines()` to the end. Keep the `getLastUpdated()` of the newest medicine.
2. Each later sync passes that time as `updatedSince` and pages to the end. Medicines come oldest change first.

```java
Instant since = loadLastSync(); // null the first time

for (Medicine medicine : client.medicines(new MedicineQuery().limit(500).updatedSince(since))) {
    upsert(medicine.getUuid(), medicine);

    if (since == null || medicine.getLastUpdated().isAfter(since)) {
        since = medicine.getLastUpdated();
    }
}

storeLastSync(since);
```

`updatedSince` takes an `Instant`. The SDK sends it in UTC, in whole seconds, like `2026-11-08T09:30:00Z`. It means
"at or after", so the medicine that set the time comes back once more. Store each medicine by its `uuid`: it never
changes. Medicines are never deleted and IDs are never reused.

## Errors

The exceptions are in `dev.oushodh.errors`. Every one is an `OushodhException`, which is a `RuntimeException`. It
has `getStatus()` (the HTTP status), `getCode()` (the API's error code) and `getMessage()`. Match on the code or on
the class, never on the message: the message is for people and may change. When the answer is not JSON, the code is `null` and the message is
`HTTP <status>`.

| Class                       | Status | Code                               |
| --------------------------- | ------ | ---------------------------------- |
| `BadRequestException`       | 400    | `bad_request`                      |
| `AuthenticationException`   | 401    | `unauthorized`                     |
| `PermissionDeniedException` | 403    | `plan_expired` or `plan_limit`     |
| `NotFoundException`         | 404    | `not_found`                        |
| `RateLimitException`        | 429    | `rate_limited` or `quota_exceeded` |
| `InternalServerException`   | 5xx    | `internal`                         |
| `ApiConnectionException`    | none   | none: the network failed           |
| `ApiTimeoutException`       | none   | none: the request timed out        |

```java
try {
    medicine = client.getMedicine("MED-012933");
} catch (NotFoundException error) {
    medicine = null;
} catch (PermissionDeniedException error) {
    if ("plan_limit".equals(error.getCode())) {
        System.out.println("Upgrade your plan to read this medicine.");
    }

    throw error;
}
```

No error message holds your key.

## Retries and rate limits

The SDK retries a `429 rate_limited`, a `5xx` and a network error up to `maxRetries` times. After a `5xx` or a
network error it waits a little longer each time, with jitter. It never retries any other `4xx`: they give the same
answer every time. Every route is a `GET`, so a retry is always safe.

Your plan sets how many requests each key may make in a minute (60 a minute on Developer). It also sets how many
your account may make in a month.

- Over the minute's limit, the API answers `429 rate_limited`. The `Retry-After` header says how many seconds to wait.
  The SDK waits that long and tries again.
- When the month's requests are used up, the API answers `429 quota_exceeded` until the next month. The SDK throws
  `RateLimitException` at once and does not wait. `getRetryAfter()` holds the seconds to the next month.

## Plan limits

Some plans read only the first medicines, in the order of their permanent number. Lists and search leave out the rest.
A lookup by ID of such a medicine throws `PermissionDeniedException` with the code `plan_limit`.
`meta().getMedicines()` says how many medicines your key reads.

## Images

`productImage` is the address of a medicine's picture. When it is under `/v1/images/`, Oushodh keeps the copy and
serves it with no key, so an `<img>` tag can show it. The file never changes, so it can be cached for a year.

```java
String url = medicine.getProductImage();

if (url != null && url.startsWith(client.imageUrl(""))) {
    byte[] jpeg = client.getImage(url.substring(url.lastIndexOf('/') + 1));
}
```

`getImage` takes the file name, not the whole address. Any other address belongs to the source, and it can move or
be removed.

## Android

Android has no `java.net.http`, so give the client a `Transport` built on the HTTP client your app uses. This one uses
OkHttp. Call the client off the main thread.

```kotlin
val http = OkHttpClient()

val client = Oushodh.builder()
    .apiKey(key)
    .transport { uri, headers, timeout ->
        val request = Request.Builder().url(uri.toString())
        headers.forEach { (name, value) -> request.header(name, value) }

        http.newBuilder().callTimeout(timeout).build().newCall(request.build()).execute().use { response ->
            Transport.Response(response.code, response.headers.toMultimap(), response.body.bytes())
        }
    }
    .build()
```

A transport throws an `InterruptedIOException`, such as a `SocketTimeoutException`, when a request times out, and any
other `IOException` when the network fails. OkHttp already does this.

## Develop

You need a JDK 17 or newer to run Gradle. The build uses a JDK 25 toolchain, and Gradle downloads it when you have
none. It compiles for Java 17.

```sh
./gradlew build          # format check, compile, tests
./gradlew spotlessApply  # format the code with google-java-format
./gradlew javadoc
```

The tests use a local HTTP server from the JDK. They never call the API.

## Release

For maintainers. Pushing a `v*` tag runs the release workflow, which publishes to Maven Central. The tag, `version`
in `gradle.properties` and `Oushodh.VERSION` must match. The workflow's comments list the repository secrets it needs.

## Links

- API documentation: [oushodh.dev/docs](https://oushodh.dev/docs)
- API keys: [app.oushodh.dev](https://app.oushodh.dev)
- Changes: [CHANGELOG.md](CHANGELOG.md)

## License

MIT. See [LICENSE](LICENSE).
