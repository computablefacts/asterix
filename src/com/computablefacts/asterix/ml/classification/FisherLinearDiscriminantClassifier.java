package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import org.apache.commons.lang3.NotImplementedException;
import smile.classification.FLD;

@CheckReturnValue
final public class FisherLinearDiscriminantClassifier implements AbstractBinaryClassifier {

  private FLD classifier_;

  public FisherLinearDiscriminantClassifier() {
  }

  @Override
  public String type() {
    return "FisherLinearDiscriminant";
  }

  @Override
  public boolean isTrained() {
    return classifier_ != null;
  }

  @Override
  public int predict(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkState(classifier_ != null, "classifier should be trained first");

    return classifier_.predict(vector.denseArray());
  }

  @Override
  public void train(FeatureMatrix matrix, int[] actuals) {

    Preconditions.checkNotNull(matrix, "matrix should not be null");
    Preconditions.checkNotNull(actuals, "actuals should not be null");
    Preconditions.checkArgument(matrix.nbRows() == actuals.length,
        "mismatch between the number of rows and the number of actuals");
    Preconditions.checkState(classifier_ == null, "classifier has already been trained");

    classifier_ = FLD.fit(matrix.denseArray(), actuals);
  }

  @Override
  public void update(FeatureVector vector, int actual) {
    throw new NotImplementedException("FLD classifier cannot be incrementally trained");
  }

  @Override
  public boolean supportsIncrementalTraining() {
    return false;
  }
}
