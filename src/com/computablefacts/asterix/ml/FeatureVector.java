package com.computablefacts.asterix.ml;

import com.computablefacts.Generated;
import com.computablefacts.asterix.View;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import smile.stat.hypothesis.CorTest;
import smile.util.SparseArray;

@CheckReturnValue
final public class FeatureVector {

  private final Map<Integer, Double> nonZeroEntries_;
  private int length_;

  public FeatureVector(int length) {
    length_ = length;
    nonZeroEntries_ = new HashMap<>(length_ / 3);
  }

  public FeatureVector(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");

    length_ = vector.length();
    nonZeroEntries_ = new HashMap<>(vector.nonZeroEntries_);
  }

  public FeatureVector(double[] vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");

    length_ = vector.length;
    nonZeroEntries_ = new HashMap<>(length_ / 3);

    for (int i = 0; i < vector.length; i++) {
      set(i, vector[i]);
    }
  }

  public FeatureVector(List<Double> vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");

    length_ = vector.size();
    nonZeroEntries_ = new HashMap<>(length_ / 3);

    for (int i = 0; i < vector.size(); i++) {
      set(i, vector.get(i));
    }
  }

  public static FeatureVector concat(List<FeatureVector> vectors) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");

    if (vectors.size() == 1) {
      return vectors.get(0);
    }

    int length = vectors.stream().mapToInt(FeatureVector::length).sum();
    FeatureVector vector = new FeatureVector(length);
    @Var int prevLength = 0;

    for (FeatureVector vect : vectors) {
      int disp = prevLength;
      vect.nonZeroEntries().forEach(idx -> vector.set(disp + idx, vect.get(idx)));
      prevLength += vect.length();
    }
    return vector;
  }

  public static FeatureVector dropEntries(FeatureVector vector, Set<Integer> idxToDrop) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkNotNull(idxToDrop, "idxToDrop should not be null");

    if (idxToDrop.isEmpty()) {
      return vector;
    }

    @Var int k = 0;
    FeatureVector newVector = new FeatureVector(vector.length() - idxToDrop.size());

    for (int i = 0; i < vector.length(); i++) {
      if (!idxToDrop.contains(i)) {
        newVector.set(k++, vector.get(i));
      }
    }
    return newVector;
  }

  public static Set<Integer> findZeroedEntries(List<FeatureVector> vectors) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");
    Preconditions.checkArgument(vectors.size() > 0, "the number of vectors must be > 0");

    FeatureVector sums = new FeatureVector(vectors.get(0).length());

    vectors.forEach(vector -> {
      for (int i = 0; i < vector.length(); i++) {
        sums.set(i, sums.get(i) + vector.get(i));
      }
    });
    return Sets.newHashSet(sums.zeroEntries());
  }

  public static Set<Integer> findNonZeroedEntries(List<FeatureVector> vectors) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");
    Preconditions.checkArgument(vectors.size() > 0, "the number of vectors must be > 0");

    FeatureVector sums = new FeatureVector(vectors.get(0).length());

    vectors.forEach(vector -> {
      for (int i = 0; i < vector.length(); i++) {
        sums.set(i, sums.get(i) + vector.get(i));
      }
    });
    return Sets.newHashSet(sums.nonZeroEntries());
  }

  public static Set<Map.Entry<Integer, Integer>> findCorrelatedEntries(List<FeatureVector> vectors,
      eCorrelation correlation, double threshold, int nbIterations) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");
    Preconditions.checkNotNull(correlation, "correlation should not be null");
    Preconditions.checkArgument(threshold > 0.0, "threshold must be > 0");
    Preconditions.checkArgument(nbIterations > 0, "nbIterations must be > 0");

    // Transpose
    int length = vectors.get(0).length();
    FeatureVector sums = new FeatureVector(length);
    List<double[]> matrix = new ArrayList<>(length);

    for (int i = 0; i < length; i++) {

      double[] vector = new double[vectors.size()];

      for (int j = 0; j < vectors.size(); j++) {
        vector[j] = vectors.get(j).get(i);
        sums.set(i, sums.get(i) + vectors.get(j).get(i));
      }

      matrix.add(vector);
    }

    // Compute correlation coefficient between each feature
    BigDecimal newThreshold = new BigDecimal(threshold).setScale(2, RoundingMode.HALF_UP);
    Set<Map.Entry<Integer, Integer>> correlated = ConcurrentHashMap.newKeySet();
    List<Integer> nonZeroed = View.of(sums.nonZeroEntries()).toList();
    Collections.shuffle(nonZeroed);

    View.of(nonZeroed).take(nbIterations).forEachRemainingInParallel(m -> {

      double[] v1 = matrix.get(m);

      for (int k = 0; k < nonZeroed.size(); k++) {

        int n = nonZeroed.get(k);

        if (!m.equals(n)) {

          double[] v2 = matrix.get(n);
          CorTest test;

          if (eCorrelation.KENDALL.equals(correlation)) {
            test = CorTest.kendall(v1, v2);
          } else if (eCorrelation.SPEARMAN.equals(correlation)) {
            test = CorTest.spearman(v1, v2);
          } else { // PEARSON
            test = CorTest.pearson(v1, v2);
          }
          if (newThreshold.compareTo(new BigDecimal(test.cor).setScale(2, RoundingMode.HALF_UP)) <= 0) {
            correlated.add(n < m ? new SimpleImmutableEntry<>(n, m) : new SimpleImmutableEntry<>(m, n));
          }
        }
      }
    });
    return correlated;
  }

  @Override
  public String toString() {
    List<String> array = new ArrayList<>();
    for (int i = 0; i < length_; i++) {
      array.add(Double.toString(get(i)));
    }
    return "[" + Joiner.on(", ").join(array) + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FeatureVector fv = (FeatureVector) obj;
    return length_ == fv.length_ && Objects.equals(nonZeroEntries_, fv.nonZeroEntries_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(length_, nonZeroEntries_);
  }

  @Generated
  public int length() {
    return length_;
  }

  @Generated
  public boolean isEmpty() {
    return nonZeroEntries_.isEmpty();
  }

  public SparseArray sparseArray() {
    SparseArray array = new SparseArray(length_ / 3);
    nonZeroEntries_.forEach(array::set);
    return array;
  }

  public double[] denseArray() {
    double[] array = new double[length_];
    nonZeroEntries().forEach(i -> array[i] = get(i));
    return array;
  }

  public double get(int pos) {

    Preconditions.checkArgument(0 <= pos && pos < length_, "pos must be such as 0 <= pos <= %s", length_);

    return nonZeroEntries_.getOrDefault(pos, 0.0);
  }

  public void set(int pos, double value) {

    Preconditions.checkArgument(0 <= pos && pos < length_, "pos must be such as 0 <= pos <= %s", length_);

    if (value == 0.0) {
      nonZeroEntries_.remove(pos);
    } else {
      nonZeroEntries_.put(pos, value);
    }
  }

  public void append(double value) {
    length_++;
    set(length_ - 1, value);
  }

  public void prepend(double value) {
    Map<Integer, Double> nonZeroEntries = new HashMap<>(nonZeroEntries_);
    nonZeroEntries_.clear();
    length_++;
    set(0, value);
    for (Map.Entry<Integer, Double> entry : nonZeroEntries.entrySet()) {
      set(entry.getKey() + 1, entry.getValue());
    }
  }

  public Set<Integer> zeroEntries() {
    return Sets.difference(View.range(0, length_).toSet(), nonZeroEntries_.keySet());
  }

  public Set<Integer> nonZeroEntries() {
    return ImmutableSet.copyOf(nonZeroEntries_.keySet());
  }

  public void mapValues(Function<Double, Double> function) {

    Preconditions.checkNotNull(function, "function should not be null");

    for (int idx = 0; idx < length_; idx++) {
      set(idx, function.apply(get(idx)));
    }
  }

  /**
   * It ranges from -1 to 1, -1 being a perfect negative correlation and +1 being a perfect positive correlation.
   * <p>
   * The Pearson product-moment correlation is one of the most commonly used correlations in statistics. It’s a measure
   * of the strength and the direction of a linear relationship between two variables.
   * <p>
   * The nice thing about the Spearman correlation is that relies on nearly all the same assumptions as the pearson
   * correlation, but it doesn't rely on normality, and your data can be ordinal as well. Thus, it’s a non-parametric
   * test. More on the spearman correlation here, http://www.statstutor.ac.uk/resources/uploaded/spearmans.pdf, and on
   * parametric vs. non-parametric here, http://www.oxfordmathcenter.com/drupal7/node/246.
   * <p>
   * The Kendall correlation is similar to the spearman correlation in that it is non-parametric. It can be used with
   * ordinal or continuous data. It is a statistic of dependence between two variables.
   * <p>
   * Note that Spearman is computed on ranks and so depicts monotonic relationships while Pearson is on true values and
   * depicts linear relationships. If Spearman > Pearson the correlation is monotonic but not linear.
   */
  public enum eCorrelation {
    PEARSON, SPEARMAN, KENDALL
  }
}
