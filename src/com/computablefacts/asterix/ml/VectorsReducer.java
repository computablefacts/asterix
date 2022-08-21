package com.computablefacts.asterix.ml;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Reduce a list of vectors by removing entries that are zeroes across all vectors. The reduction is
 * computed on the first batch of vectors seen and then applied on all the subsequents batches.
 */
@CheckReturnValue
final public class VectorsReducer implements Function<List<FeatureVector>, List<FeatureVector>> {

  private List<Integer> commonNonZeroEntries_ = null;

  public VectorsReducer() {
  }

  @Override
  public List<FeatureVector> apply(List<FeatureVector> vectors) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");

    if (commonNonZeroEntries_ == null) {

      Set<Integer> commonNonZeroEntries = new HashSet<>(vectors.get(0).nonZeroEntries());

      for (int i = 1; i < vectors.size(); i++) {
        commonNonZeroEntries.addAll(vectors.get(i).nonZeroEntries());
      }

      commonNonZeroEntries_ = new ArrayList<>(commonNonZeroEntries);
      Collections.sort(commonNonZeroEntries_);
    }
    if (commonNonZeroEntries_.isEmpty()) {
      return vectors;
    }

    List<FeatureVector> newVectors = new ArrayList<>();

    for (FeatureVector vector : vectors) {

      @Var int k = 0;
      FeatureVector newVector = new FeatureVector(commonNonZeroEntries_.size());

      for (int i : commonNonZeroEntries_) {
        newVector.set(k++, vector.get(i));
      }
      newVectors.add(newVector);
    }
    return newVectors;
  }
}
