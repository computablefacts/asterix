package com.computablefacts.asterix.ml.stack;

import com.computablefacts.asterix.ml.ConfusionMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
public interface AbstractStack {

  FeatureVector actuals();

  FeatureVector predictions();

  ConfusionMatrix confusionMatrix(); // actuals vs. predictions

  int predict(FeatureVector vector);
}
