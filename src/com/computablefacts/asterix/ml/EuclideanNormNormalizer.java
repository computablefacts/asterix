package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.function.Function;

@CheckReturnValue
final public class EuclideanNormNormalizer implements Function<FeatureVector, FeatureVector> {

  public EuclideanNormNormalizer() {
  }

  @Override
  public FeatureVector apply(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");

    vector.normalizeUsingEuclideanNorm();
    return vector;
  }
}
