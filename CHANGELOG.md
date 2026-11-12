# Changelog

## 0.1.0 (unreleased)

- First version of the Java SDK for the Oushodh API `/v1`. It runs on Java 17 or later and from Kotlin.
- `Oushodh` client, made with `Oushodh.builder()`: `meta()`, `listMedicines()`, `medicines()` (pages to the end),
  `search()`, `getMedicine()`, `imageUrl()` and `getImage()`.
- Models for `Medicine`, `MedicineDetail`, `Monograph`, `Page`, `SearchResult`, `Meta` and `Prices`. Unknown fields
  are kept in `getExtra()`.
- Exceptions by family, each with `getStatus()`, `getCode()` and `getMessage()`.
- Retries on `429 rate_limited` (honours `Retry-After`), `5xx` and network errors.
- A `Transport` interface, so Android apps can send requests with their own HTTP client.
