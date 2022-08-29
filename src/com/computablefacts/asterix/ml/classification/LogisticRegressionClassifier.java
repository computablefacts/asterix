package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Properties;
import smile.classification.SparseLogisticRegression;
import smile.data.SparseDataset;

@CheckReturnValue
final public class LogisticRegressionClassifier implements AbstractBinaryClassifier {

  private SparseLogisticRegression classifier_;

  public LogisticRegressionClassifier() {
  }

  @Override
  public boolean isTrained() {
    return classifier_ != null;
  }

  @Override
  public int predict(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkState(classifier_ != null, "classifier should be trained first");

    return classifier_.predict(vector.sparseArray());
  }

  @Override
  public void train(FeatureMatrix matrix, int[] actuals) {

    Preconditions.checkNotNull(matrix, "matrix should not be null");
    Preconditions.checkNotNull(actuals, "actuals should not be null");
    Preconditions.checkArgument(matrix.nbRows() == actuals.length,
        "mismatch between the number of rows and the number of actuals");
    Preconditions.checkState(classifier_ == null, "classifier has already been trained");

    Properties properties = new Properties();
    properties.setProperty("smile.logit.max.iterations", "1000");

    classifier_ = SparseLogisticRegression.binomial(
        SparseDataset.of(matrix.rows().stream().map(FeatureVector::sparseArray)), actuals, properties);
  }

  @Override
  public void update(FeatureVector vector, int actual) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkArgument(actual == KO || actual == OK,
        "invalid class: should be either 1 (in class) or 0 (not in class)");
    Preconditions.checkState(classifier_ != null, "classifier should be trained first");

    classifier_.update(vector.sparseArray(), actual);
  }

  @Override
  public boolean supportsIncrementalTraining() {
    return true;
  }
}
