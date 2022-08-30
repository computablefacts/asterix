package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import org.apache.commons.lang3.NotImplementedException;
import smile.classification.KNN;

@CheckReturnValue
final public class KNearestNeighborClassifier implements AbstractBinaryClassifier {

  private final AbstractScaler scaler_;
  private KNN<double[]> classifier_;

  public KNearestNeighborClassifier(AbstractScaler scaler) {

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

    return classifier_.predict(scaler_.predict(vector).denseArray());
  }

  @Override
  public void train(FeatureMatrix matrix, int[] actuals) {

    Preconditions.checkNotNull(matrix, "matrix should not be null");
    Preconditions.checkNotNull(actuals, "actuals should not be null");
    Preconditions.checkArgument(matrix.nbRows() == actuals.length,
        "mismatch between the number of rows and the number of actuals");
    Preconditions.checkState(classifier_ == null, "classifier has already been trained");

    classifier_ = KNN.fit(scaler_.train(matrix).denseArray(), actuals);
  }

  @Override
  public void update(FeatureVector vector, int actual) {
    throw new NotImplementedException("KNN classifier cannot be incrementally trained");
  }

  @Override
  public boolean supportsIncrementalTraining() {
    return false;
  }
}
