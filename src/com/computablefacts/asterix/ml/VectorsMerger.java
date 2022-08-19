package com.computablefacts.asterix.ml;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.List;
import java.util.function.Function;

/**
 * Merge vectors together using a simple concatenation.
 */
@CheckReturnValue
final public class VectorsMerger implements Function<List<FeatureVector>, FeatureVector> {

  public VectorsMerger() {
  }

  @Override
  public FeatureVector apply(List<FeatureVector> vectors) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");

    int length = vectors.stream().mapToInt(FeatureVector::length).sum();
    FeatureVector newVector = new FeatureVector(length);

    @Var int prevLength = 0;

    for (FeatureVector vector : vectors) {
      for (int i = 0; i < vector.length(); i++) {
        newVector.set(prevLength + i, vector.get(i));
      }
      prevLength += vector.length();
    }
    return newVector;
  }
}
