package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
public interface AbstractScaler {

  default FeatureVector predict(FeatureVector vector) {
    return vector;
  }

  default FeatureMatrix train(FeatureMatrix matrix) {
    return matrix;
  }
}
