package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import smile.classification.Classifier;
import smile.classification.SVM;

@CheckReturnValue
final public class SvmClassifier implements AbstractBinaryClassifier {

  private Classifier<double[]> classifier_;

  public SvmClassifier() {
  }

  @Override
  public boolean isTrained() {
    return classifier_ != null;
  }

  @Override
  public int predict(FeatureVector vector) {

    Preconditions.checkState(classifier_ != null, "classifier should be trained first");

    return classifier_.predict(vector.denseArray()) == -1 ? KO : OK;
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

    int[] newActuals = new int[actuals.length];

    for (int i = 0; i < actuals.length; i++) {
      newActuals[i] = actuals[i] == KO ? -1 : +1;
    }

    classifier_ = SVM.fit(newVectors, newActuals, 1.0, 0.01);
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
