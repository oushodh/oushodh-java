package dev.oushodh.models;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Every MRP the sources state for a medicine: per unit, per strip and per pack. No amount is
 * computed from another one.
 */
public final class Prices extends Model {
  private String currency = "BDT";
  private @Nullable UnitAmount unit;
  private List<SizeAmount> sizes = List.of();
  private @Nullable StripAmount strip;
  private List<PackAmount> packs = List.of();

  Prices() {}

  /** Always {@code BDT}, Bangladeshi taka. */
  public String getCurrency() {
    return currency;
  }

  /** The MRP of one unit. {@code null} when no source states it or it differs by pack size. */
  public @Nullable UnitAmount getUnit() {
    return unit;
  }

  /** The unit price in each pack size, cheapest first. Empty unless one source gives several. */
  public List<SizeAmount> getSizes() {
    return sizes;
  }

  /** The MRP of one strip. {@code null} when no source states it. */
  public @Nullable StripAmount getStrip() {
    return strip;
  }

  /** The MRP of each pack or box, cheapest first. */
  public List<PackAmount> getPacks() {
    return packs;
  }

  /** The MRP of one unit, such as a tablet or a bottle. */
  public static final class UnitAmount extends Model {
    private BigDecimal amount = BigDecimal.ZERO;
    private String source = "";

    UnitAmount() {}

    /** MRP in BDT. */
    public BigDecimal getAmount() {
      return amount;
    }

    /** The source that states this price. */
    public String getSource() {
      return source;
    }
  }

  /** The unit price in one pack size. */
  public static final class SizeAmount extends Model {
    private @Nullable String size;
    private BigDecimal amount = BigDecimal.ZERO;
    private String source = "";

    SizeAmount() {}

    /** The pack size as the source names it. {@code null} when it names none. */
    public @Nullable String getSize() {
      return size;
    }

    /** MRP in BDT. */
    public BigDecimal getAmount() {
      return amount;
    }

    /** The source that states this price. */
    public String getSource() {
      return source;
    }
  }

  /** The MRP of one strip. */
  public static final class StripAmount extends Model {
    private BigDecimal amount = BigDecimal.ZERO;
    private @Nullable BigDecimal units;
    private String source = "";

    StripAmount() {}

    /** MRP in BDT. */
    public BigDecimal getAmount() {
      return amount;
    }

    /** How many units the strip holds. {@code null} when the source does not say. */
    public @Nullable BigDecimal getUnits() {
      return units;
    }

    /** The source that states this price. */
    public String getSource() {
      return source;
    }
  }

  /** The MRP of one pack or box. */
  public static final class PackAmount extends Model {
    private BigDecimal amount = BigDecimal.ZERO;
    private @Nullable BigDecimal units;
    private String label = "";
    private String source = "";

    PackAmount() {}

    /** MRP in BDT. */
    public BigDecimal getAmount() {
      return amount;
    }

    /** How many units the pack holds. {@code null} when the source does not say. */
    public @Nullable BigDecimal getUnits() {
      return units;
    }

    /** The pack as the source names it, such as {@code 30's pack}. */
    public String getLabel() {
      return label;
    }

    /** The source that states this price. */
    public String getSource() {
      return source;
    }
  }
}
