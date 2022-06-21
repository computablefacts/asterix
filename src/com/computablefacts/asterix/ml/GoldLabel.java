package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.File;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@CheckReturnValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class GoldLabel {

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

  public GoldLabel(GoldLabel goldLabel) {
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
   * Split a set of gold labels, i.e. reference labels, into 2 subsets : train and test.
   *
   * @param goldLabels gold labels.
   * @return a {@link Map.Entry}. The key represents the train dataset (75% of the gold labels) and
   * the value represents the test dataset (25% of the gold labels). The proportion of TP, TN, FP
   * and FN will be roughly the same in the train and test datasets.
   */
  public static Map.Entry<List<GoldLabel>, List<GoldLabel>> split(List<GoldLabel> goldLabels) {
    return split(goldLabels, true, 0.75);
  }

  /**
   * Split a set of gold labels, i.e. reference labels, into 2 subsets : train and test.
   *
   * @param goldLabels gold labels.
   * @param keepProportions must be true iif the proportion of TP, TN, FP and FN must be roughly the
   * same in the train and test subsets.
   * @return a {@link Map.Entry}. The key is the train dataset and the value is the test dataset.
   */
  public static Map.Entry<List<GoldLabel>, List<GoldLabel>> split(List<GoldLabel> goldLabels,
      boolean keepProportions, double trainSizeInPercent) {

    Preconditions.checkNotNull(goldLabels, "goldLabels should not be null");
    Preconditions.checkArgument(0.0 <= trainSizeInPercent && trainSizeInPercent <= 1.0,
        "trainSizeInPercent must be such as 0.0 <= trainSizeInPercent <= 1.0");

    List<GoldLabel> gls = Lists.newArrayList(goldLabels);

    Collections.shuffle(gls);

    List<GoldLabel> train = new ArrayList<>();
    List<GoldLabel> test = new ArrayList<>();

    if (!keepProportions) {

      int trainSize = (int) (gls.size() * trainSizeInPercent);

      train.addAll(gls.subList(0, trainSize));
      test.addAll(gls.subList(trainSize, gls.size()));
    } else {

      List<GoldLabel> tps = goldLabels.stream().filter(GoldLabel::isTruePositive)
          .collect(Collectors.toList());
      List<GoldLabel> tns = goldLabels.stream().filter(GoldLabel::isTrueNegative)
          .collect(Collectors.toList());
      List<GoldLabel> fps = goldLabels.stream().filter(GoldLabel::isFalsePositive)
          .collect(Collectors.toList());
      List<GoldLabel> fns = goldLabels.stream().filter(GoldLabel::isFalseNegative)
          .collect(Collectors.toList());

      int trainTpSize = (int) (trainSizeInPercent * tps.size());
      int trainTnSize = (int) (trainSizeInPercent * tns.size());
      int trainFpSize = (int) (trainSizeInPercent * fps.size());
      int trainFnSize = (int) (trainSizeInPercent * fns.size());

      train.addAll(tps.subList(0, trainTpSize));
      train.addAll(tns.subList(0, trainTnSize));
      train.addAll(fps.subList(0, trainFpSize));
      train.addAll(fns.subList(0, trainFnSize));

      test.addAll(tps.subList(trainTpSize, tps.size()));
      test.addAll(tns.subList(trainTnSize, tns.size()));
      test.addAll(fps.subList(trainFpSize, fps.size()));
      test.addAll(fns.subList(trainFnSize, fns.size()));
    }

    Preconditions.checkState(train.size() + test.size() == gls.size(),
        "Inconsistent state reached for splits : %s found vs %s expected",
        train.size() + test.size(), gls.size());

    return new SimpleImmutableEntry<>(train, test);
  }

  /**
   * Build a confusion matrix from a set of gold labels.
   *
   * @param goldLabels gold labels.
   * @return a {@link ConfusionMatrix}.
   */
  public static ConfusionMatrix confusionMatrix(List<GoldLabel> goldLabels) {

    Preconditions.checkNotNull(goldLabels, "goldLabels should not be null");

    ConfusionMatrix matrix = new ConfusionMatrix();

    for (int i = 0; i < goldLabels.size(); i++) {

      GoldLabel gl = goldLabels.get(i);

      int tp = gl.isTruePositive() ? 1 : 0;
      int tn = gl.isTrueNegative() ? 1 : 0;
      int fp = gl.isFalsePositive() ? 1 : 0;
      int fn = gl.isFalseNegative() ? 1 : 0;

      Preconditions.checkState(tp + tn + fp + fn == 1,
          "Inconsistent state reached for gold label : (%s, %s)", gl.label(), gl.id());

      matrix.addTruePositives(tp);
      matrix.addTrueNegatives(tn);
      matrix.addFalsePositives(fp);
      matrix.addFalseNegatives(fn);
    }
    return matrix;
  }

  /**
   * Load gold labels from a gzipped JSONL file.
   *
   * @param file the input file.
   * @param label the specific gold labels to load. If {@code label} is set to {@code null}, all
   * gold labels will be loaded.
   * @return a set of gold labels.
   */
  public static View<GoldLabel> load(File file, String label) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file file does not exist : %s", file);

    return View.of(file, true).index()
        .filter(row -> !Strings.isNullOrEmpty(row.getValue()) /* remove empty rows */)
        .map(row -> new GoldLabel(JsonCodec.asObject(row.getValue())))
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
  public static boolean save(File file, View<GoldLabel> goldLabels) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkNotNull(goldLabels, "goldLabels should not be null");

    if (!file.exists()) {
      goldLabels.toFile(JsonCodec::asString, file, false, true);
      return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof GoldLabel)) {
      return false;
    }
    GoldLabel gl = (GoldLabel) obj;
    return Objects.equals(id(), gl.id()) && Objects.equals(label(), gl.label()) && Objects.equals(
        data(), gl.data()) && isTrueNegative() == gl.isTrueNegative()
        && isTruePositive() == gl.isTruePositive() && isFalseNegative() == gl.isFalseNegative()
        && isFalsePositive() == gl.isFalsePositive();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id(), label(), data(), isTrueNegative(), isTruePositive(),
        isFalseNegative(), isFalsePositive());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id()).add("label", label())
        .add("data", data()).add("is_true_negative", isTrueNegative())
        .add("is_true_positive", isTruePositive()).add("is_false_negative", isFalseNegative())
        .add("is_false_positive", isFalsePositive()).omitNullValues().toString();
  }

  /**
   * Get the gold label unique identifier.
   *
   * @return a unique identifier.
   */
  public @NotNull String id() {
    return id_;
  }

  /**
   * Get the gold label class.
   *
   * @return the label name.
   */
  public @NotNull String label() {
    return label_;
  }

  /**
   * Get the data point associated to this gold label.
   *
   * @return the data point.
   */
  public @NotNull String data() {
    return data_;
  }

  /**
   * Check if the gold label is a TP.
   *
   * @return true iif the current gold label is a TP, false otherwise.
   */
  public boolean isTruePositive() {
    return isTruePositive_;
  }

  /**
   * Check if the gold label is a FP.
   *
   * @return true iif the current gold label is a FP, false otherwise.
   */
  public boolean isFalsePositive() {
    return isFalsePositive_;
  }

  /**
   * Check if the gold label is a TN.
   *
   * @return true iif the current gold label is a TN, false otherwise.
   */
  public boolean isTrueNegative() {
    return isTrueNegative_;
  }

  /**
   * Check if the gold label is a FN.
   *
   * @return true iif the current gold label is a FN, false otherwise.
   */
  public boolean isFalseNegative() {
    return isFalseNegative_;
  }

  /**
   * Returns the current gold label as a {@link Map}.
   *
   * @return a {@link Map}.
   */
  @Deprecated
  public Map<String, Object> asMap() {

    Map<String, Object> map = new HashMap<>();
    map.put("id", id());
    map.put("label", label());
    map.put("data", data());
    map.put("is_true_positive", isTruePositive());
    map.put("is_false_positive", isFalsePositive());
    map.put("is_true_negative", isTrueNegative());
    map.put("is_false_negative", isFalseNegative());

    return map;
  }
}
