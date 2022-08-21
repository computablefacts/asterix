package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureVector;
import com.computablefacts.asterix.ml.VectorsReducer;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import smile.classification.GradientTreeBoost;
import smile.data.DataFrame;
import smile.data.formula.Formula;

@CheckReturnValue
final public class GradientBoostedTreesClassifier implements AbstractBinaryClassifier {

  private final VectorsReducer reducer_ = new VectorsReducer();
  private GradientTreeBoost tree_ = null;

  public GradientBoostedTreesClassifier() {
  }

  @Override
  public boolean isTrained() {
    return tree_ != null;
  }

  @Override
  public int predict(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkState(tree_ != null, "classifier should be trained first");

    FeatureVector newVector = reducer_.apply(Lists.newArrayList(vector)).get(0);
    int[][] vectors = new int[1][newVector.length() + 1];
    vectors[0] = new int[newVector.length() + 1];

    for (int i : newVector.nonZeroEntries()) {
      vectors[0][i + 1] = (int) vector.get(i);
    }

    DataFrame df = DataFrame.of(vectors);
    return tree_.predict(df)[0];
  }

  @Override
  public void train(List<FeatureVector> vectors, int[] actuals) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");
    Preconditions.checkNotNull(actuals, "actuals should not be null");
    Preconditions.checkArgument(vectors.size() == actuals.length,
        "mismatch between the number of vectors and the number of actuals");
    Preconditions.checkState(tree_ == null, "classifier has already been trained");

    List<FeatureVector> newVectors = reducer_.apply(vectors);
    int[][] vects = new int[newVectors.size()][newVectors.get(0).length() + 1];

    for (int i = 0; i < newVectors.size(); i++) {

      FeatureVector vector = newVectors.get(i);
      vects[i] = new int[vector.length() + 1];
      vects[i][0] = actuals[i]; // V1

      for (int j : vector.nonZeroEntries()) {
        vects[i][j + 1] = (int) vector.get(j);
      }
    }

    DataFrame df = DataFrame.of(vects);
    tree_ = GradientTreeBoost.fit(Formula.lhs("V1"), df);
  }

  @Override
  public void update(FeatureVector vector, int actual) {
    throw new NotImplementedException(
        "Gradient Boosted Trees classifier cannot be incrementally trained");
  }

  @Override
  public boolean supportsIncrementalTraining() {
    return false;
  }
}
