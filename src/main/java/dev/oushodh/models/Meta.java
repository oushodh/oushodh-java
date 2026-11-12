package dev.oushodh.models;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** The account a key belongs to, from {@code GET /v1/meta}. */
public final class Meta extends Model {
  private String client = "";
  private String idProfile = "";
  private int medicines;
  private @Nullable Instant lastImport;

  Meta() {}

  /** The account the key belongs to. */
  public String getClient() {
    return client;
  }

  /** The ID profile your medicine IDs come from. */
  public String getIdProfile() {
    return idProfile;
  }

  /** How many medicines your key reads: all the API holds, or fewer on a plan with a limit. */
  public int getMedicines() {
    return medicines;
  }

  /** When the data was last imported. */
  public @Nullable Instant getLastImport() {
    return lastImport;
  }
}
