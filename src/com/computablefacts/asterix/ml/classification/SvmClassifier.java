package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import org.apache.commons.lang3.NotImplementedException;
import smile.classification.Classifier;
import smile.classification.SVM;

@CheckReturnValue
final public class SvmClassifier implements AbstractBinaryClassifier {

  private final AbstractScaler scaler_;
  private Classifier<double[]> classifier_;

  public SvmClassifier(AbstractScaler scaler) {

    Preconditions.checkNotNull(scaler, "scaler should not be null");

    scaler_ = scaler;
  }

  @Override
  public boolean isTrained() {
    return classifier_ != null;
  }

  @Override
  public int predict(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkState(classifier_ != null, "classifier should be trained first");

    return classifier_.predict(scaler_.predict(vector).denseArray()) == -1 ? KO : OK;
  }

  @Override
  public void train(FeatureMatrix matrix, int[] actuals) {

    Preconditions.checkNotNull(matrix, "matrix should not be null");
    Preconditions.checkNotNull(actuals, "actuals should not be null");
    Preconditions.checkArgument(matrix.nbRows() == actuals.length,
        "mismatch between the number of rows and the number of actuals");
    Preconditions.checkState(classifier_ == null, "classifier has already been trained");

    int[] newActuals = new int[actuals.length];

    for (int i = 0; i < actuals.length; i++) {
      newActuals[i] = actuals[i] == KO ? -1 : +1;
    }

    classifier_ = SVM.fit(scaler_.train(matrix).denseArray(), newActuals, 1.0, 0.01);
  }

  @Override
  public void update(FeatureVector vector, int actual) {
    throw new NotImplementedException("SVM classifier cannot be incrementally trained");
  }

  @Override
  public boolean supportsIncrementalTraining() {
    return false;
  }
}
