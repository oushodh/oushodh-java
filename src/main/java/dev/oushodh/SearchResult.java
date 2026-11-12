package dev.oushodh;

import java.util.List;

/** The best matches for a search, from {@code GET /v1/medicines/search}. */
public final class SearchResult extends Model {
  private List<Medicine> items = List.of();
  private int total;

  SearchResult() {}

  /** The best matches first, at most {@code limit}. */
  public List<Medicine> getItems() {
    return items;
  }

  /** How many medicines match, including those past {@code limit}. */
  public int getTotal() {
    return total;
  }
}
