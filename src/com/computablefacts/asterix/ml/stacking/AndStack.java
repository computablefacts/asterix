package com.computablefacts.asterix.ml.stacking;

import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.ml.ConfusionMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
final public class AndStack implements AbstractStack {

  private final FeatureVector predictions_;
  private final ConfusionMatrix confusionMatrix_ = new ConfusionMatrix();
  private final AbstractStack leftStack_;
  private final AbstractStack rightStack_;

  public AndStack(AbstractStack leftStack, AbstractStack rightStack) {

    Preconditions.checkNotNull(leftStack, "leftStack should not be null");
    Preconditions.checkNotNull(rightStack, "rightStack should not be null");

    leftStack_ = leftStack;
    rightStack_ = rightStack;

    Preconditions.checkState(leftStack.actuals().length() == rightStack.actuals().length());
    Preconditions.checkState(leftStack.predictions().length() == rightStack.predictions().length());

    predictions_ = new FeatureVector(leftStack.actuals().length());

    for (int i = 0; i < leftStack.actuals().length(); i++) {

      int actual1 = (int) leftStack.actuals().get(i);
      int actual2 = (int) rightStack.actuals().get(i);

      Preconditions.checkState(actual1 == actual2);

      int prediction1 = (int) leftStack.predictions().get(i);
      int prediction2 = (int) rightStack.predictions().get(i);

      predictions_.set(i, reduce(prediction1, prediction2));
      confusionMatrix_.add(actual1, (int) predictions_.get(i));
    }
  }

  @Override
  public String toString() {
    return String.format("(%s AND %s)", leftStack_.toString(), rightStack_.toString());
  }

  @Override
  public FeatureVector actuals() {
    return leftStack_.actuals();
  }

  @Override
  public FeatureVector predictions() {
    return predictions_;
  }

  @Override
  public ConfusionMatrix confusionMatrix() {
    return confusionMatrix_;
  }

  @Override
  public int predict(FeatureVector vector) {
    if (leftStack_.predict(vector) == KO) {
      return KO;
    }
    return rightStack_.predict(vector);
  }

  private int reduce(int prediction1, int prediction2) {
    return prediction1 == OK && prediction2 == OK ? OK : KO;
  }
}
