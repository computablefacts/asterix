package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.ArrayList;
import java.util.List;

@CheckReturnValue
public final class ZScoreScaler implements AbstractScaler {

  private final List<Double> mus_ = new ArrayList<>();
  private final List<Double> sigmas_ = new ArrayList<>();

  public ZScoreScaler() {
  }

  @Override
  public FeatureVector predict(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkState(mus_.size() == sigmas_.size(), "mismatch between the number of mu and sigma values");
    Preconditions.checkState(vector.length() == mus_.size(),
        "mismatch between the vector length and the number of mu/sigma values");

    FeatureVector newVector = new FeatureVector(vector);

    for (int colIdx = 0; colIdx < vector.length(); colIdx++) {

      double mu = mus_.get(colIdx);
      double sigma = sigmas_.get(colIdx);

      newVector.set(colIdx, (vector.get(colIdx) - mu) / sigma);
    }
    return newVector;
  }

  @Override
  public FeatureMatrix train(FeatureMatrix matrix) {

    Preconditions.checkNotNull(matrix, "matrix should not be null");
    Preconditions.checkState(mus_.size() == 0 && sigmas_.size() == 0,
        "mismatch between the number of mu and sigma values");

    FeatureMatrix newMatrix = new FeatureMatrix(matrix);
    List<FeatureVector> columns = matrix.columns();

    for (int colIdx = 0; colIdx < columns.size(); colIdx++) {

      @Var double mu = 0;
      @Var double temp = 0;
      FeatureVector column = columns.get(colIdx);

      for (int idx = 0; idx < column.length(); idx++) {

        double x = column.get(idx);
        double delta = x - mu;
        mu += delta / (idx + 1);

        if (idx > 0) {
          temp += delta * (x - mu);
        }
      }

      @Var double sigma = Math.sqrt(temp / (column.length() - 1));
      if (sigma == 0) {
        sigma = 1;
      }

      mus_.add(mu);
      sigmas_.add(sigma);

      newMatrix.mapColumnValues(colIdx, x -> (x - mus_.get(mus_.size() - 1)) / sigmas_.get(sigmas_.size() - 1));
    }
    return newMatrix;
  }
}
