package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.View;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import smile.stat.hypothesis.CorTest;

@CheckReturnValue
final public class VectorsReducer implements Function<List<FeatureVector>, List<FeatureVector>> {

  private final eCorrelation correlation_;
  private List<Integer> nonZeroEntries_ = null;

  /**
   * Reduce a list of vectors by removing entries that are zeroes across all vectors. The reduction is computed on the
   * first batch of vectors seen and then applied on all the subsequents batches.
   */
  public VectorsReducer() {
    correlation_ = null;
  }

  /**
   * Reduce a list of vectors by removing features that are highly correlated. The reduction is computed on the first
   * batch of vectors seen and then applied on all the subsequents batches.
   */
  public VectorsReducer(eCorrelation correlation) {
    correlation_ = correlation;
  }

  public static Table<Integer, Integer, CorTest> correlations(List<FeatureVector> vectors, eCorrelation correlation) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");
    Preconditions.checkNotNull(correlation, "correlation should not be null");

    // Transpose
    int length = vectors.get(0).length();
    List<double[]> matrix = new ArrayList<>(length);

    for (int i = 0; i < length; i++) {

      double[] vector = new double[vectors.size()];

      for (int j = 0; j < vectors.size(); j++) {
        vector[j] = vectors.get(j).get(i);
      }

      matrix.add(vector);
    }

    // Compute correlation coefficient between each feature
    Table<Integer, Integer, CorTest> correlations = HashBasedTable.create();

    for (int i = 0; i < matrix.size(); i++) {
      for (int j = 0; j < i + 1; j++) {

        double[] v1 = matrix.get(i);
        double[] v2 = matrix.get(j);

        if (eCorrelation.KENDALL.equals(correlation)) {
          correlations.put(i, j, CorTest.kendall(v1, v2));
        } else if (eCorrelation.SPEARMAN.equals(correlation)) {
          correlations.put(i, j, CorTest.spearman(v1, v2));
        } else { // PEARSON
          correlations.put(i, j, CorTest.pearson(v1, v2));
        }
      }
    }
    return correlations;
  }

  @Override
  public List<FeatureVector> apply(List<FeatureVector> vectors) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");

    if (nonZeroEntries_ == null) {
      if (correlation_ != null) {

        Set<Integer> correlatedFeatures = highlyCorrelatedFeatures(vectors, correlation_);
        Set<Integer> nonCorrelatedEntries = Sets.difference(View.range(0, vectors.get(0).length()).toSet(),
            correlatedFeatures);

        nonZeroEntries_ = new ArrayList<>(nonCorrelatedEntries);
        Collections.sort(nonZeroEntries_);
      } else {

        Set<Integer> nonZeroEntries = new HashSet<>(vectors.get(0).nonZeroEntries());

        for (int i = 1; i < vectors.size(); i++) {
          nonZeroEntries.addAll(vectors.get(i).nonZeroEntries());
        }

        nonZeroEntries_ = new ArrayList<>(nonZeroEntries);
        Collections.sort(nonZeroEntries_);
      }
    }
    if (nonZeroEntries_.isEmpty()) {
      return vectors;
    }

    List<FeatureVector> newVectors = new ArrayList<>();

    for (FeatureVector vector : vectors) {

      @Var int k = 0;
      FeatureVector newVector = new FeatureVector(nonZeroEntries_.size());

      for (int i : nonZeroEntries_) {
        newVector.set(k++, vector.get(i));
      }
      newVectors.add(newVector);
    }
    return newVectors;
  }

  @Beta
  public List<Integer> nonZeroEntries() {
    return nonZeroEntries_ == null ? ImmutableList.of() : ImmutableList.copyOf(nonZeroEntries_);
  }

  private Set<Integer> highlyCorrelatedFeatures(List<FeatureVector> vectors, eCorrelation correlation) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");
    Preconditions.checkNotNull(correlation, "correlation should not be null");

    // Transpose
    int length = vectors.get(0).length();
    List<double[]> matrix = new ArrayList<>(length);

    for (int i = 0; i < length; i++) {

      double[] vector = new double[vectors.size()];

      for (int j = 0; j < vectors.size(); j++) {
        vector[j] = vectors.get(j).get(i);
      }

      matrix.add(vector);
    }

    // Compute correlation coefficient between each feature
    Set<Integer> correlatedFeatures = new HashSet<>();

    for (int i = 0; i < matrix.size(); i++) {

      double[] v1 = matrix.get(i);

      for (int j = 0; j < i; j++) {
        if (correlatedFeatures.contains(j)) {
          continue;
        }

        double[] v2 = matrix.get(j);
        CorTest test;

        if (eCorrelation.KENDALL.equals(correlation)) {
          test = CorTest.kendall(v1, v2);
        } else if (eCorrelation.SPEARMAN.equals(correlation)) {
          test = CorTest.spearman(v1, v2);
        } else { // PEARSON
          test = CorTest.pearson(v1, v2);
        }
        if (test.cor >= 0.8 /* strong correlation */) {
          correlatedFeatures.add(j);
        }
      }
    }
    return correlatedFeatures;
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
