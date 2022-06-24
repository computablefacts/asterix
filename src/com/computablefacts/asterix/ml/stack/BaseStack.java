package com.computablefacts.asterix.ml.stack;

import static com.computablefacts.asterix.ml.classifiers.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classifiers.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.ml.ConfusionMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;

@CheckReturnValue
public abstract class BaseStack implements AbstractStack {

  private final String label_;
  private final ConfusionMatrix confusionMatrix_ = new ConfusionMatrix();
  private FeatureVector actuals_;
  private FeatureVector predictions_;

  public BaseStack(String label) {

    Preconditions.checkNotNull(label, "label should not be null");

    label_ = label;
  }

  @Override
  public String toString() {
    return label_;
  }

  @Override
  public FeatureVector actuals() {
    return actuals_;
  }

  @Override
  public FeatureVector predictions() {
    return predictions_;
  }

  @Override
  public ConfusionMatrix confusionMatrix() {
    return confusionMatrix_;
  }

  public void init(List<FeatureVector> dataset, int[] actuals) {

    Preconditions.checkNotNull(dataset, "predictions should not be null");
    Preconditions.checkNotNull(actuals, "actuals should not be null");
    Preconditions.checkArgument(dataset.size() == actuals.length,
        "mismatch between the number of data points and the number of actuals");

    actuals_ = new FeatureVector(actuals.length);
    predictions_ = new FeatureVector(actuals.length);

    for (int i = 0; i < actuals.length; i++) {

      int actual = actuals[i];
      int prediction = predict(dataset.get(i));

      Preconditions.checkState(actual == KO || actual == OK,
          "invalid actual: should be either 1 (in class) or 0 (not in class)");
      Preconditions.checkState(prediction == KO || prediction == OK,
          "invalid prediction: should be either 1 (in class) or 0 (not in class)");

      actuals_.set(i, actual);
      predictions_.set(i, prediction);
      confusionMatrix_.add(actual, prediction);
    }
  }
}
