package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import smile.classification.DiscreteNaiveBayes;
import smile.classification.DiscreteNaiveBayes.Model;
import smile.util.SparseArray;

@CheckReturnValue
final public class DiscreteNaiveBayesClassifier implements AbstractBinaryClassifier {

  private final DiscreteNaiveBayes.Model model_;
  private DiscreteNaiveBayes classifier_;

  public DiscreteNaiveBayesClassifier() {
    this(Model.MULTINOMIAL);
  }

  public DiscreteNaiveBayesClassifier(DiscreteNaiveBayes.Model model) {

    Preconditions.checkNotNull(model, "model should not be null");

    model_ = model;
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
    Preconditions.checkState(model_ != null, "model should be defined first");
    Preconditions.checkState(classifier_ == null, "classifier has already been trained");

    SparseArray[] newMatrix = new SparseArray[matrix.nbRows()];

    for (int rowIdx = 0; rowIdx < matrix.nbRows(); rowIdx++) {
      newMatrix[rowIdx] = matrix.row(rowIdx).sparseArray();
    }

    classifier_ = new DiscreteNaiveBayes(model_, 2, matrix.nbColumns());
    classifier_.update(newMatrix, actuals);
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