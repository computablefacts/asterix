package com.computablefacts.asterix.ml.stacking;

import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.Result;
import com.computablefacts.asterix.ml.ConfusionMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;

@CheckReturnValue
public abstract class AbstractStack {

  protected ConfusionMatrix confusionMatrix_ = new ConfusionMatrix();
  protected FeatureVector actuals_;
  protected FeatureVector predictions_;
  protected boolean isFrozen_ = false;

  public FeatureVector actuals() {
    return actuals_;
  }

  public FeatureVector predictions() {
    return predictions_;
  }

  public ConfusionMatrix confusionMatrix() {
    return confusionMatrix_;
  }

  public void compactify() {
    isFrozen_ = true;
    actuals_ = null;
    predictions_ = null;
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

  public int predict(String text) {
    throw new NotImplementedException("predict(String) is not implemented");
  }

  @Beta
  public int predictOnNormalizedText(String text) {
    throw new NotImplementedException("predictOnNormalizedText(String) is not implemented");
  }

  public abstract int predict(FeatureVector vector);

  @Beta
  public Result<String> focus(String text) {
    throw new NotImplementedException("focus(String) is not implemented");
  }
  
  @Beta
  public Result<String> focusOnNormalizedText(String txt) {
    throw new NotImplementedException("focusOnNormalizedText(String) is not implemented");
  }
}
