package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
public interface AbstractBinaryClassifier {

  int OK = 1;
  int KO = 0;

  String type();

  boolean isTrained();

  int predict(FeatureVector vector);

  // actual values must be either 0 (not in class) or 1 (in class)
  void train(FeatureMatrix matrix, int[] actuals);

  void update(FeatureVector vector, int actual);

  boolean supportsIncrementalTraining();
}
