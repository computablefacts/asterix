package com.computablefacts.asterix.ml;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.ArrayList;
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

  private Set<Integer> indices_ = null;

  public VectorsReducer() {
  }

  @Override
  public List<FeatureVector> apply(List<FeatureVector> vectors) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");

    if (indices_ == null) {

      indices_ = new HashSet<>(vectors.get(0).zeroes());

      for (int i = 1; i < vectors.size(); i++) {
        indices_.retainAll(vectors.get(i).zeroes());
      }
    }
    if (indices_.isEmpty()) {
      return vectors;
    }

    List<FeatureVector> newVectors = new ArrayList<>();

    for (FeatureVector vector : vectors) {

      @Var int k = 0;
      FeatureVector newVector = new FeatureVector(vector.length() - indices_.size());

      for (int i = 0; i < vector.length(); i++) {
        if (!indices_.contains(i)) {
          newVector.set(k++, vector.get(i));
        }
      }
      newVectors.add(newVector);
    }
    return newVectors;
  }
}
