package dev.oushodh.models;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** The base of every API object. It keeps the fields this SDK does not know yet. */
public abstract class Model {
  private final transient Map<String, @Nullable Object> extra = new LinkedHashMap<>();

  Model() {}

  /**
   * Fields the API added after this SDK was released, by their JSON name. Objects are maps and
   * arrays are lists.
   */
  public Map<String, @Nullable Object> getExtra() {
    return Collections.unmodifiableMap(extra);
  }

  @JsonAnySetter
  void putExtra(String name, @Nullable Object value) {
    extra.put(name, value);
  }
}
