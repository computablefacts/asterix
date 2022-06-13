package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Generated;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CheckReturnValue
public interface IGoldLabel<D> {

  /**
   * Split a set of gold labels, i.e. reference labels, into 2 subsets : train and test.
   *
   * @param goldLabels gold labels.
   * @return a {@link Map.Entry}. The key represents the train dataset (75% of the gold labels) and
   * the value represents the test dataset (25% of the gold labels). The proportion of TP, TN, FP
   * and FN will be roughly the same in the train and test datasets.
   */
  static <T> Map.Entry<List<IGoldLabel<T>>, List<IGoldLabel<T>>> split(
      List<? extends IGoldLabel<T>> goldLabels) {
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
  static <T> Map.Entry<List<IGoldLabel<T>>, List<IGoldLabel<T>>> split(
      List<? extends IGoldLabel<T>> goldLabels, boolean keepProportions,
      double trainSizeInPercent) {

    Preconditions.checkNotNull(goldLabels, "goldLabels should not be null");
    Preconditions.checkArgument(0.0 <= trainSizeInPercent && trainSizeInPercent <= 1.0,
        "trainSizeInPercent must be such as 0.0 <= trainSizeInPercent <= 1.0");

    List<IGoldLabel<T>> gls = Lists.newArrayList(goldLabels);

    Collections.shuffle(gls);

    List<IGoldLabel<T>> train = new ArrayList<>();
    List<IGoldLabel<T>> test = new ArrayList<>();

    if (!keepProportions) {

      int trainSize = (int) (gls.size() * trainSizeInPercent);

      train.addAll(gls.subList(0, trainSize));
      test.addAll(gls.subList(trainSize, gls.size()));
    } else {

      List<IGoldLabel<T>> tps = goldLabels.stream().filter(IGoldLabel::isTruePositive)
          .collect(Collectors.toList());
      List<IGoldLabel<T>> tns = goldLabels.stream().filter(IGoldLabel::isTrueNegative)
          .collect(Collectors.toList());
      List<IGoldLabel<T>> fps = goldLabels.stream().filter(IGoldLabel::isFalsePositive)
          .collect(Collectors.toList());
      List<IGoldLabel<T>> fns = goldLabels.stream().filter(IGoldLabel::isFalseNegative)
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
  static <T> ConfusionMatrix confusionMatrix(List<? extends IGoldLabel<T>> goldLabels) {

    Preconditions.checkNotNull(goldLabels, "goldLabels should not be null");

    ConfusionMatrix matrix = new ConfusionMatrix();

    for (int i = 0; i < goldLabels.size(); i++) {

      IGoldLabel<?> gl = goldLabels.get(i);

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
   * Get the gold label unique identifier.
   *
   * @return a unique identifier.
   */
  String id();

  /**
   * Get the gold label class.
   *
   * @return the label name.
   */
  String label();

  /**
   * Get the data point associated to this gold label.
   *
   * @return the data point.
   */
  D data();

  /**
   * Check if the gold label is a TP.
   *
   * @return true iif the current gold label is a TP, false otherwise.
   */
  boolean isTruePositive();

  /**
   * Check if the gold label is a FP.
   *
   * @return true iif the current gold label is a FP, false otherwise.
   */
  boolean isFalsePositive();

  /**
   * Check if the gold label is a TN.
   *
   * @return true iif the current gold label is a TN, false otherwise.
   */
  boolean isTrueNegative();

  /**
   * Check if the gold label is a FN.
   *
   * @return true iif the current gold label is a FN, false otherwise.
   */
  boolean isFalseNegative();

  /**
   * Returns the current gold label as a {@link Map}.
   *
   * @return a {@link Map}.
   */
  @Generated
  @Deprecated
  default Map<String, Object> asMap() {

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
