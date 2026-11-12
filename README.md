# Oushodh for Java

The Java SDK for the Oushodh medicine data API: medicine data for Bangladesh, checked against the DGDA register.

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

## Quickstart

Make an API key on the Keys page of the member portal at [app.oushodh.dev](https://app.oushodh.dev). A key starts
with `oush_` and is shown once. Keep it on your server: a key inside an app can be copied out of it.

```java
import dev.oushodh.MedicineDetail;
import dev.oushodh.Oushodh;
import dev.oushodh.SearchResult;

Oushodh client = Oushodh.builder().apiKey("oush_...").build(); // or set OUSHODH_API_KEY and leave out apiKey

System.out.println(client.meta().getClient()); // check that the key works

SearchResult result = client.search("napa 500", 5);

for (var medicine : result.getItems()) {
    System.out.println(medicine.getId() + " " + medicine.getProductName() + " " + medicine.getPrices().getUnit());
}

MedicineDetail detail = client.getMedicine("MED-012933");
System.out.println(detail.getMonograph());
```

From Kotlin, the getters read as properties:

```kotlin
val client = Oushodh.builder().build()

for (medicine in client.medicines(MedicineQuery().q("napa"))) {
    println("${medicine.id} ${medicine.productName}")
}
```

The builder takes these options:

| Option          | Default                   | What it does                                                    |
| --------------- | ------------------------- | --------------------------------------------------------------- |
| `apiKey`        | `OUSHODH_API_KEY`         | Your API key. With no key, `build()` throws.                    |
| `baseUrl`       | `https://api.oushodh.dev` | Where the API is.                                               |
| `timeout`       | 30 seconds                | How long to wait for each request, as a `Duration`.             |
| `maxRetries`    | `2`                       | Retries after a `429 rate_limited`, a `5xx` or a network error. |
| `transport`     | a `JdkTransport`          | How requests are sent. Pass `new JdkTransport(yourHttpClient)` to use your own `HttpClient`. |

## Methods

| Method                          | Route                          | Returns                    |
| ------------------------------- | ------------------------------ | -------------------------- |
| `meta()`                        | `GET /v1/meta`                 | `Meta`                     |
| `listMedicines(query)`          | `GET /v1/medicines`            | `Page`                     |
| `medicines(query)`              | `GET /v1/medicines`, each page | `Iterable<Medicine>`       |
| `search(q)`, `search(q, limit)` | `GET /v1/medicines/search`     | `SearchResult`             |
| `getMedicine(ref)`              | `GET /v1/medicines/{ref}`      | `MedicineDetail`           |
| `imageUrl(file)`                | none                           | the picture's address      |
| `getImage(file)`                | `GET /v1/images/{file}`        | the picture's bytes        |

`query` is a `MedicineQuery`, with `limit`, `cursor`, `updatedSince` and `q`. Leave out what you do not need, or call
`listMedicines()` and `medicines()` with no query.

`ref` is a medicine's ID in any profile, an old ID, or its UUID. The answer always carries the medicine's current ID.

Each model has a getter for every field the API documents. Money is a `BigDecimal` in taka (BDT), and times are
`Instant`s. A value no source gives is `null`. Fields the API adds later are kept in `getExtra()`, by their JSON name.

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

Every exception is an `OushodhException`, which is a `RuntimeException`. It has `getStatus()` (the HTTP status),
`getCode()` (the API's error code) and `getMessage()`. Match on the code or on the class, never on the message: the
message is for people and may change. When the answer is not JSON, the code is `null` and the message is
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

The SDK retries a `429 rate_limited`, a `5xx` and a network error up to `maxRetries` times. After a `5xx` or a
network error it waits a little longer each time. It never retries any other `4xx`: they give the same answer every
time. No error message holds your key.

## Rate limits

Your plan sets how many requests each key may make in a minute (60 a minute on Developer), and how many your account
may make in a month.

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

The release workflow publishes to Maven Central through the Central Portal when a `v*` tag is pushed. Maven Central
has no trusted publishing, so the workflow reads these repository secrets. They are not set yet.

| Secret                   | What it holds                                 |
| ------------------------ | --------------------------------------------- |
| `MAVEN_CENTRAL_USERNAME` | The user name of a Central Portal user token. |
| `MAVEN_CENTRAL_PASSWORD` | The password of that token.                   |
| `SIGNING_KEY`            | An ASCII-armored PGP private key.             |
| `SIGNING_KEY_PASSWORD`   | The password of that key.                     |

The tag must match `version` in `gradle.properties`, and `Oushodh.VERSION` must match it too.

## License

MIT. See [LICENSE](LICENSE).
