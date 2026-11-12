package dev.oushodh;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** The options of {@link Oushodh#listMedicines(MedicineQuery)}. Leave out what you do not need. */
public final class MedicineQuery {
  private @Nullable Integer limit;
  private @Nullable String cursor;
  private @Nullable Instant updatedSince;
  private @Nullable String q;

  /** Medicines on one page, 1 to 500. The API's default is 100. */
  public MedicineQuery limit(int limit) {
    this.limit = limit;

    return this;
  }

  /** The {@code next} value from the page before. Leave it out for the first page. */
  public MedicineQuery cursor(@Nullable String cursor) {
    this.cursor = cursor;

    return this;
  }

  /**
   * Only medicines changed at or after this time, oldest change first. It is sent in UTC, in whole
   * seconds.
   */
  public MedicineQuery updatedSince(@Nullable Instant updatedSince) {
    this.updatedSince = updatedSince;

    return this;
  }

  /**
   * Only medicines whose brand, generic or manufacturer starts with these words, or whose ID
   * matches.
   */
  public MedicineQuery q(@Nullable String q) {
    this.q = q;

    return this;
  }

  @Nullable Integer getLimit() {
    return limit;
  }

  @Nullable String getCursor() {
    return cursor;
  }

  @Nullable Instant getUpdatedSince() {
    return updatedSince;
  }

  @Nullable String getQ() {
    return q;
  }
}
