package dev.oushodh;

import org.jspecify.annotations.Nullable;

/**
 * The medical text of a medicine, as its source wrote it, in English. It is information for
 * professionals, not medical advice. Some text holds HTML tags.
 */
public final class Monograph extends Model {
  private String source = "";
  private @Nullable String indications;
  private @Nullable String composition;
  private @Nullable String pharmacology;
  private @Nullable String dosage;
  private @Nullable String interactions;
  private @Nullable String contraindications;
  private @Nullable String sideEffects;
  private @Nullable String pregnancyLactation;
  private @Nullable String precautions;
  private @Nullable String overdose;
  private @Nullable String storage;

  Monograph() {}

  /** The source of the text. */
  public String getSource() {
    return source;
  }

  /** What the medicine is used for. */
  public @Nullable String getIndications() {
    return indications;
  }

  /** What the medicine contains. */
  public @Nullable String getComposition() {
    return composition;
  }

  /** How the medicine works. */
  public @Nullable String getPharmacology() {
    return pharmacology;
  }

  /** Dosage and how to give it. */
  public @Nullable String getDosage() {
    return dosage;
  }

  /** Interactions with other medicines. */
  public @Nullable String getInteractions() {
    return interactions;
  }

  /** When the medicine must not be used. */
  public @Nullable String getContraindications() {
    return contraindications;
  }

  /** Side effects. */
  public @Nullable String getSideEffects() {
    return sideEffects;
  }

  /** Use in pregnancy and while breastfeeding. */
  public @Nullable String getPregnancyLactation() {
    return pregnancyLactation;
  }

  /** Precautions and warnings. */
  public @Nullable String getPrecautions() {
    return precautions;
  }

  /** What an overdose does, and its treatment. */
  public @Nullable String getOverdose() {
    return overdose;
  }

  /** How to store the medicine. */
  public @Nullable String getStorage() {
    return storage;
  }
}
