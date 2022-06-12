package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Generated;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;

@CheckReturnValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class GoldLabel implements IGoldLabel<String> {

  @JsonProperty(value = "id", required = true)
  private final String id_;
  @JsonProperty(value = "label", required = true)
  private final String label_;
  @JsonProperty(value = "data", required = true)
  private final String data_;
  @JsonProperty(value = "is_true_negative", required = true)
  private final boolean isTrueNegative_;
  @JsonProperty(value = "is_true_positive", required = true)
  private final boolean isTruePositive_;
  @JsonProperty(value = "is_false_negative", required = true)
  private final boolean isFalseNegative_;
  @JsonProperty(value = "is_false_positive", required = true)
  private final boolean isFalsePositive_;

  public GoldLabel(IGoldLabel<String> goldLabel) {
    this(goldLabel.id(), goldLabel.label(), goldLabel.data(), goldLabel.isTrueNegative(),
        goldLabel.isTruePositive(), goldLabel.isFalseNegative(), goldLabel.isFalsePositive());
  }

  public GoldLabel(Map<String, Object> goldLabel) {
    this((String) goldLabel.get("id"), (String) goldLabel.get("label"),
        (String) goldLabel.get("data"), (Boolean) goldLabel.get("is_true_negative"),
        (Boolean) goldLabel.get("is_true_positive"), (Boolean) goldLabel.get("is_false_negative"),
        (Boolean) goldLabel.get("is_false_positive"));
  }

  @JsonCreator
  public GoldLabel(@JsonProperty(value = "id") String id,
      @JsonProperty(value = "label") String label, @JsonProperty(value = "data") String data,
      @JsonProperty(value = "is_true_negative") boolean isTrueNegative,
      @JsonProperty(value = "is_true_positive") boolean isTruePositive,
      @JsonProperty(value = "is_false_negative") boolean isFalseNegative,
      @JsonProperty(value = "is_false_positive") boolean isFalsePositive) {

    Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "id should neither be null nor empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(label),
        "label should neither be null nor empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(data),
        "data should neither be null nor empty");

    id_ = id;
    label_ = label;
    data_ = data;
    isTrueNegative_ = isTrueNegative;
    isTruePositive_ = isTruePositive;
    isFalseNegative_ = isFalseNegative;
    isFalsePositive_ = isFalsePositive;

    int tp = isTruePositive ? 1 : 0;
    int tn = isTrueNegative ? 1 : 0;
    int fp = isFalsePositive ? 1 : 0;
    int fn = isFalseNegative ? 1 : 0;

    Preconditions.checkState(tp + tn + fp + fn == 1,
        "inconsistent state reached for gold label : (%s, %s)", id(), label());
  }

  /**
   * Load gold labels from a gzipped JSONL file.
   *
   * @param file the input file.
   * @param label the specific gold labels to load. If {@code label} is set to {@code null}, all
   * gold labels will be loaded.
   * @return a set of gold labels.
   */
  public static View<IGoldLabel<String>> load(File file, String label) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file file does not exist : %s", file);

    return View.of(file, true).index()
        .filter(row -> !Strings.isNullOrEmpty(row.getValue()) /* remove empty rows */)
        .map(row -> (IGoldLabel<String>) new GoldLabel(JsonCodec.asObject(row.getValue())))
        .filter(goldLabel -> label == null || label.equals(goldLabel.label()));
  }

  /**
   * Save gold labels to a gzipped JSONL file.
   *
   * @param file the output file.
   * @param goldLabels the gold labels to save.
   * @return true iif the gold labels have been written to the file, false otherwise.
   */
  @CanIgnoreReturnValue
  public static boolean save(File file, View<? extends IGoldLabel<String>> goldLabels) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkNotNull(goldLabels, "goldLabels should not be null");

    if (!file.exists()) {
      goldLabels.toFile(JsonCodec::asString, file, false, true);
      return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof IGoldLabel)) {
      return false;
    }
    IGoldLabel<?> gl = (IGoldLabel<?>) o;
    return Objects.equals(id(), gl.id()) && Objects.equals(label(), gl.label())
        && Objects.equals(data(), gl.data())
        && isTrueNegative() == gl.isTrueNegative()
        && isTruePositive() == gl.isTruePositive()
        && isFalseNegative() == gl.isFalseNegative()
        && isFalsePositive() == gl.isFalsePositive();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id(), label(), data(), isTrueNegative(), isTruePositive(),
        isFalseNegative(), isFalsePositive());
  }

  @Generated
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id()).add("label", label())
        .add("data", data()).add("is_true_negative", isTrueNegative())
        .add("is_true_positive", isTruePositive()).add("is_false_negative", isFalseNegative())
        .add("is_false_positive", isFalsePositive()).omitNullValues().toString();
  }

  @Override
  public @NotNull String id() {
    return id_;
  }

  @Override
  public @NotNull String label() {
    return label_;
  }

  @Override
  public @NotNull String data() {
    return data_;
  }

  @Override
  public boolean isTruePositive() {
    return isTruePositive_;
  }

  @Override
  public boolean isFalsePositive() {
    return isFalsePositive_;
  }

  @Override
  public boolean isTrueNegative() {
    return isTrueNegative_;
  }

  @Override
  public boolean isFalseNegative() {
    return isFalseNegative_;
  }
}
