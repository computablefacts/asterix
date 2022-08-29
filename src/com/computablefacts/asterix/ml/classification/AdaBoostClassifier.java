package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import org.apache.commons.lang3.NotImplementedException;
import smile.classification.AdaBoost;
import smile.data.DataFrame;
import smile.data.formula.Formula;

@CheckReturnValue
final public class AdaBoostClassifier implements AbstractBinaryClassifier {

  private AdaBoost boost_ = null;

  public AdaBoostClassifier() {
  }

  @Override
  public boolean isTrained() {
    return boost_ != null;
  }

  @Override
  public int predict(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkState(boost_ != null, "classifier should be trained first");

    int[][] vectors = new int[1][vector.length() + 1];
    vectors[0] = new int[vector.length() + 1];

    for (int i : vector.nonZeroEntries()) {
      vectors[0][i + 1] = (int) vector.get(i);
    }

    DataFrame df = DataFrame.of(vectors);
    return boost_.predict(df)[0];
  }

  @Override
  public void train(FeatureMatrix matrix, int[] actuals) {

    Preconditions.checkNotNull(matrix, "matrix should not be null");
    Preconditions.checkNotNull(actuals, "actuals should not be null");
    Preconditions.checkArgument(matrix.nbRows() == actuals.length,
        "mismatch between the number of rows and the number of actuals");
    Preconditions.checkState(boost_ == null, "classifier has already been trained");

    int[][] newMatrix = new int[matrix.nbRows()][matrix.nbColumns() + 1];

    for (int rowIdx = 0; rowIdx < matrix.nbRows(); rowIdx++) {

      FeatureVector row = matrix.row(rowIdx);
      newMatrix[rowIdx] = new int[row.length() + 1];
      newMatrix[rowIdx][0] = actuals[rowIdx]; // V1

      for (int j : row.nonZeroEntries()) {
        newMatrix[rowIdx][j + 1] = (int) row.get(j);
      }
    }

    DataFrame df = DataFrame.of(newMatrix);
    boost_ = AdaBoost.fit(Formula.lhs("V1"), df);
  }

  @Override
  public void update(FeatureVector vector, int actual) {
    throw new NotImplementedException("Random Forest classifier cannot be incrementally trained");
  }

  @Override
  public boolean supportsIncrementalTraining() {
    return false;
  }
}
