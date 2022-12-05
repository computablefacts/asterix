package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import org.apache.commons.lang3.NotImplementedException;
import smile.classification.DecisionTree;
import smile.data.DataFrame;
import smile.data.formula.Formula;

@CheckReturnValue
final public class DecisionTreeClassifier implements AbstractBinaryClassifier {

  private DecisionTree tree_ = null;

  public DecisionTreeClassifier() {
  }

  @Override
  public String type(){
    return "DecisionTree";
  }

  @Override
  public boolean isTrained() {
    return tree_ != null;
  }

  @Override
  public int predict(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkState(tree_ != null, "classifier should be trained first");

    int[][] vectors = new int[1][vector.length() + 1];
    vectors[0] = new int[vector.length() + 1];

    for (int i : vector.nonZeroEntries()) {
      vectors[0][i + 1] = (int) vector.get(i);
    }

    DataFrame df = DataFrame.of(vectors);
    return tree_.predict(df)[0];
  }

  @Override
  public void train(FeatureMatrix matrix, int[] actuals) {

    Preconditions.checkNotNull(matrix, "matrix should not be null");
    Preconditions.checkNotNull(actuals, "actuals should not be null");
    Preconditions.checkArgument(matrix.nbRows() == actuals.length,
        "mismatch between the number of rows and the number of actuals");
    Preconditions.checkState(tree_ == null, "classifier has already been trained");

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
    tree_ = DecisionTree.fit(Formula.lhs("V1"), df);
  }

  @Override
  public void update(FeatureVector vector, int actual) {
    throw new NotImplementedException("Decision Tree classifier cannot be incrementally trained");
  }

  @Override
  public boolean supportsIncrementalTraining() {
    return false;
  }
}
