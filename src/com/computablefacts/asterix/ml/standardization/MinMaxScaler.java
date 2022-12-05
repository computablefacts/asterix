package com.computablefacts.asterix.ml.standardization;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;

@CheckReturnValue
public final class MinMaxScaler implements AbstractScaler {

  private final List<Double> mins_ = new ArrayList<>();
  private final List<Double> maxs_ = new ArrayList<>();

  public MinMaxScaler() {
  }

  @Override
  public FeatureVector transform(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkState(mins_.size() == maxs_.size(), "mismatch between the number of min and max values");
    Preconditions.checkState(vector.length() == mins_.size(),
        "mismatch between the vector length and the number of min/max values");

    FeatureVector newVector = new FeatureVector(vector);

    for (int colIdx = 0; colIdx < vector.length(); colIdx++) {

      double min = mins_.get(colIdx);
      double max = maxs_.get(colIdx);

      if (min != 0 || max != 0) {
        newVector.set(colIdx, (vector.get(colIdx) - min) / (max - min));
      }
    }
    return newVector;
  }

  @Override
  public FeatureMatrix fitAndTransform(FeatureMatrix matrix) {

    Preconditions.checkNotNull(matrix, "matrix should not be null");
    Preconditions.checkState(mins_.size() == 0 && maxs_.size() == 0,
        "mismatch between the number of min and max values");

    FeatureMatrix newMatrix = new FeatureMatrix(matrix);
    List<FeatureVector> columns = matrix.columns();

    for (int colIdx = 0; colIdx < columns.size(); colIdx++) {

      DoubleSummaryStatistics stats = Arrays.stream(columns.get(colIdx).denseArray()).summaryStatistics();
      double min = stats.getMin();
      double max = stats.getMax();

      mins_.add(min);
      maxs_.add(max);

      if (min != 0 || max != 0) {
        newMatrix.mapColumnValues(colIdx, x -> (x - min) / (max - min));
      }
    }
    return newMatrix;
  }
}
