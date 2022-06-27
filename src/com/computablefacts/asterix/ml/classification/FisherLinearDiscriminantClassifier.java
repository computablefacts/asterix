package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import smile.classification.FLD;

@CheckReturnValue
final public class FisherLinearDiscriminantClassifier implements AbstractBinaryClassifier {

  private FLD classifier_;

  public FisherLinearDiscriminantClassifier() {
  }

  @Override
  public int predict(FeatureVector vector) {

    Preconditions.checkState(classifier_ != null, "classifier should be trained first");

    return classifier_.predict(vector.denseArray());
  }

  @Override
  public void train(List<FeatureVector> vectors, int[] actuals) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");
    Preconditions.checkNotNull(actuals, "actuals should not be null");
    Preconditions.checkArgument(vectors.size() == actuals.length,
        "mismatch between the number of vectors and the number of actuals");
    Preconditions.checkState(classifier_ == null, "classifier has already been trained");

    double[][] newVectors = new double[vectors.size()][vectors.get(0).length()];

    for (int i = 0; i < vectors.size(); i++) {
      newVectors[i] = vectors.get(i).denseArray();
    }

    classifier_ = FLD.fit(newVectors, actuals);
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
