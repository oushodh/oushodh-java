package dev.oushodh;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One medicine: one brand in one strength and dosage form. A value no source gives is {@code null},
 * never a blank, a zero or an estimate.
 */
public class Medicine extends Model {
  private String id = "";
  private String uuid = "";
  private String brand = "";
  private @Nullable String category;
  private String productName = "";
  private @Nullable String productImage;
  private List<String> images = List.of();
  private @Nullable String unit;
  private @Nullable String size;
  private @Nullable BigDecimal price;
  private @Nullable String pricePer;
  private @Nullable String priceSource;
  private List<UnitPrice> unitPrices = List.of();
  private Prices prices = new Prices();
  private @Nullable String descriptionEn;
  private @Nullable String descriptionBn;
  private @Nullable String dosageForm;
  private @Nullable String strength;
  private @Nullable String genericName;
  private @Nullable String manufacturer;
  private @Nullable String sourceUrl;
  private Instant lastUpdated = Instant.EPOCH;
  private boolean dgdaListed;
  private @Nullable String medicineType;

  Medicine() {}

  /** The medicine's ID in your account's ID profile, such as {@code MED-012933}. */
  public String getId() {
    return id;
  }

  /** The key that never changes. Store it to match a medicine across syncs. */
  public String getUuid() {
    return uuid;
  }

  public String getBrand() {
    return brand;
  }

  /** Reserved for a symptom category. {@code null} for every medicine today. */
  public @Nullable String getCategory() {
    return category;
  }

  /** Brand, strength and dosage form together, such as {@code Napa 500 mg Tablet}. */
  public String getProductName() {
    return productName;
  }

  /** The address of the medicine's picture. {@code null} when there is none. */
  public @Nullable String getProductImage() {
    return productImage;
  }

  /** Every picture of the medicine, {@code productImage} first. */
  public List<String> getImages() {
    return images;
  }

  /** The pack's unit, such as {@code Strip} or {@code Bottle}. */
  public @Nullable String getUnit() {
    return unit;
  }

  /** The pack's size, such as {@code 10 Tablets} or {@code 200 ml}. */
  public @Nullable String getSize() {
    return size;
  }

  /** MRP in BDT of what {@link #getPricePer()} names. Kept for older clients: read prices. */
  public @Nullable BigDecimal getPrice() {
    return price;
  }

  /** What {@link #getPrice()} buys. Kept for older clients: read prices. */
  public @Nullable String getPricePer() {
    return pricePer;
  }

  /** Where the price comes from. */
  public @Nullable String getPriceSource() {
    return priceSource;
  }

  /** The unit price in each pack size, cheapest first. Kept for older clients: read prices. */
  public List<UnitPrice> getUnitPrices() {
    return unitPrices;
  }

  /** Every MRP the sources state for the medicine. This is the home of all price data. */
  public Prices getPrices() {
    return prices;
  }

  public @Nullable String getDescriptionEn() {
    return descriptionEn;
  }

  public @Nullable String getDescriptionBn() {
    return descriptionBn;
  }

  public @Nullable String getDosageForm() {
    return dosageForm;
  }

  public @Nullable String getStrength() {
    return strength;
  }

  public @Nullable String getGenericName() {
    return genericName;
  }

  public @Nullable String getManufacturer() {
    return manufacturer;
  }

  /** The page the record was read from. */
  public @Nullable String getSourceUrl() {
    return sourceUrl;
  }

  /** When any field of this record last changed. Sync uses it. */
  public Instant getLastUpdated() {
    return lastUpdated;
  }

  /** {@code true} when a DGDA registration (DAR number) links to the medicine. */
  public boolean isDgdaListed() {
    return dgdaListed;
  }

  /**
   * The DGDA register that lists it: {@code allopathic}, {@code homoeopathic}, {@code herbal},
   * {@code ayurvedic} or {@code unani}. {@code null} when no register lists it.
   */
  public @Nullable String getMedicineType() {
    return medicineType;
  }

  /** The unit price in one pack size. */
  public static final class UnitPrice extends Model {
    private @Nullable String size;
    private BigDecimal price = BigDecimal.ZERO;

    UnitPrice() {}

    /** The pack size as the source names it. {@code null} when it names none. */
    public @Nullable String getSize() {
      return size;
    }

    /** MRP in BDT of one unit in that size. */
    public BigDecimal getPrice() {
      return price;
    }
  }
}
