package com.computablefacts.asterix.ml;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.function.Function;

@CheckReturnValue
final public class MinMaxNormalizer implements Function<FeatureVector, FeatureVector> {

  public MinMaxNormalizer() {
  }

  @Override
  public FeatureVector apply(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");

    vector.normalizeUsingMinMax();
    return vector;
  }
}
