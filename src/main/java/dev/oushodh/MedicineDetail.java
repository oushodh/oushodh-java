package dev.oushodh;

import org.jspecify.annotations.Nullable;

/** One medicine with its medical text, from {@code GET /v1/medicines/{ref}}. */
public final class MedicineDetail extends Medicine {
  private @Nullable Monograph monograph;

  MedicineDetail() {}

  /** The medical text. {@code null} when no source gives the medicine any text. */
  public @Nullable Monograph getMonograph() {
    return monograph;
  }
}
