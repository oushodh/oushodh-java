package dev.oushodh.models;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** One page of medicines, from {@code GET /v1/medicines}. */
public final class Page extends Model {
  private List<Medicine> items = List.of();
  private @Nullable String next;
  private int total;

  Page() {}

  /** The medicines on this page. */
  public List<Medicine> getItems() {
    return items;
  }

  /** Pass it back as {@code cursor} for the next page. {@code null} on the last page. */
  public @Nullable String getNext() {
    return next;
  }

  /** How many medicines match, across all pages. */
  public int getTotal() {
    return total;
  }
}
