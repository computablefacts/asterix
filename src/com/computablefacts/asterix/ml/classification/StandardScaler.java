package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.View;
import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;

@CheckReturnValue
public final class StandardScaler implements AbstractScaler {

  private final List<Double> means_ = new ArrayList<>();
  private final List<Double> stdDevs_ = new ArrayList<>();

  public StandardScaler() {
  }

  @Override
  public FeatureVector transform(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkState(means_.size() == stdDevs_.size(), "mismatch between the number of mu and sigma values");
    Preconditions.checkState(vector.length() == means_.size(),
        "mismatch between the vector length and the number of mu/sigma values");

    FeatureVector newVector = new FeatureVector(vector);

    for (int colIdx = 0; colIdx < vector.length(); colIdx++) {

      double mean = means_.get(colIdx);
      double stdDev = stdDevs_.get(colIdx);

      newVector.set(colIdx, (vector.get(colIdx) - mean) / stdDev);
    }
    return newVector;
  }

  @Override
  public FeatureMatrix fitAndTransform(FeatureMatrix matrix) {

    Preconditions.checkNotNull(matrix, "matrix should not be null");
    Preconditions.checkState(means_.size() == 0 && stdDevs_.size() == 0,
        "mismatch between the number of mu and sigma values");

    FeatureMatrix newMatrix = new FeatureMatrix(matrix);
    List<FeatureVector> columns = matrix.columns();

    for (int colIdx = 0; colIdx < columns.size(); colIdx++) {

      FeatureVector column = columns.get(colIdx);
      double mean = View.of(column.nonZeroEntries()).map(column::get).reduce(0.0, (carry, x) -> carry + x)
          / (double) column.length();
      double sumOfSquares = View.of(column.nonZeroEntries()).map(column::get).map(x -> (x - mean) * (x - mean))
          .reduce(0.0, (carry, x) -> carry + x);
      double variance = sumOfSquares / ((double) column.length() /* - 1.0 */);
      double stdDev = Math.sqrt(variance);

      means_.add(mean);
      stdDevs_.add(stdDev);

      newMatrix.mapColumnValues(colIdx, x -> (x - mean) / stdDev);
    }
    return newMatrix;
  }
}
