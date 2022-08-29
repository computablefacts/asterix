package com.computablefacts.asterix.ml;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@CheckReturnValue
final public class FeatureMatrix {

  private final List<FeatureVector> rows_ = new ArrayList<>();

  public FeatureMatrix() {
  }

  public FeatureMatrix(List<FeatureVector> vectors) {
    addRows(vectors);
  }

  public FeatureMatrix(double[][] array) {
    addRows(array);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FeatureMatrix m = (FeatureMatrix) obj;
    return nbRows() == m.nbRows() && nbColumns() == m.nbColumns() && Objects.equals(rows_, m.rows_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rows_);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    rows_.forEach(row -> builder.append(row.toString()).append('\n'));
    return builder.toString().trim();
  }

  public List<FeatureVector> rows() {
    return ImmutableList.copyOf(rows_);
  }

  public List<FeatureVector> columns() {

    List<FeatureVector> columns = new ArrayList<>(nbColumns());

    for (int colIdx = 0; colIdx < nbColumns(); colIdx++) {
      columns.add(new FeatureVector(nbRows()));
    }
    for (int rowIdx = 0; rowIdx < nbRows(); rowIdx++) {
      for (int colIdx = 0; colIdx < nbColumns(); colIdx++) {
        columns.get(colIdx).set(rowIdx, row(rowIdx).get(colIdx));
      }
    }
    return columns;
  }

  public FeatureVector row(int rowIdx) {

    Preconditions.checkArgument(0 <= rowIdx && rowIdx < nbRows(), "index must be such as 0 <= rowIdx < " + nbRows());

    return rows_.get(rowIdx);
  }

  public FeatureVector column(int colIdx) {

    Preconditions.checkArgument(0 <= colIdx && colIdx < nbColumns(),
        "index must be such as 0 <= colIdx < " + nbColumns());

    FeatureVector column = new FeatureVector(nbRows());

    for (int rowIdx = 0; rowIdx < nbRows(); rowIdx++) {
      column.set(rowIdx, row(rowIdx).get(colIdx));
    }
    return column;
  }

  public int nbRows() {
    return rows_.size();
  }

  public int nbColumns() {
    return nbRows() == 0 ? 0 : rows_.get(0).length();
  }

  public void addRows(Collection<FeatureVector> rows) {

    Preconditions.checkNotNull(rows, "rows should not be null");

    rows.forEach(this::addRow);
  }

  public void addRow(FeatureVector row) {

    Preconditions.checkNotNull(row, "row should not be null");
    Preconditions.checkArgument(nbRows() == 0 || nbColumns() == row.length(),
        "mismatch between the row length and the number of columns : expected " + nbColumns());

    rows_.add(row);
  }

  public void addRows(double[][] rows) {

    Preconditions.checkNotNull(rows, "rows should not be null");

    for (int rowIdx = 0; rowIdx < rows.length; rowIdx++) {
      addRow(rows[rowIdx]);
    }
  }

  public void addRow(double[] row) {

    Preconditions.checkNotNull(row, "row should not be null");
    Preconditions.checkArgument(nbRows() == 0 || nbColumns() == row.length,
        "mismatch between the row length and the number of columns : expected " + nbColumns());

    rows_.add(new FeatureVector(row));
  }

  public void appendColumn(FeatureVector vector) {
    addColumn(vector, false);
  }

  public void prependColumn(FeatureVector vector) {
    addColumn(vector, true);
  }

  public void appendColumn(double[] vector) {
    addColumn(vector, false);
  }

  public void prependColumn(double[] vector) {
    addColumn(vector, true);
  }

  public void replaceRow(int rowIdx, FeatureVector row) {

    Preconditions.checkArgument(0 <= rowIdx && rowIdx < nbRows(), "rowIdx must be such as 0 <= rowIdx < " + nbRows());
    Preconditions.checkNotNull(row, "row should not be null");
    Preconditions.checkArgument(nbColumns() == row.length(),
        "mismatch between the row length and the number of columns : expected " + nbColumns());

    rows_.set(rowIdx, row);
  }

  public void replaceColumn(int colIdx, FeatureVector column) {

    Preconditions.checkArgument(0 <= colIdx && colIdx < nbColumns(),
        "colIdx must be such as 0 <= colIdx < " + nbColumns());
    Preconditions.checkNotNull(column, "column should not be null");
    Preconditions.checkArgument(nbRows() == column.length(),
        "mismatch between the column length and the number of rows : expected " + nbRows());

    for (int rowIdx = 0; rowIdx < nbRows(); rowIdx++) {
      rows_.get(rowIdx).set(colIdx, column.get(rowIdx));
    }
  }

  public void replaceRow(int rowIdx, double[] row) {

    Preconditions.checkArgument(0 <= rowIdx && rowIdx < nbRows(), "rowIdx must be such as 0 <= rowIdx < " + nbRows());
    Preconditions.checkNotNull(row, "row should not be null");
    Preconditions.checkArgument(nbColumns() == row.length,
        "mismatch between the row length and the number of columns : expected " + nbColumns());

    rows_.set(rowIdx, new FeatureVector(row));
  }

  public void replaceColumn(int colIdx, double[] column) {

    Preconditions.checkArgument(0 <= colIdx && colIdx < nbColumns(),
        "colIdx must be such as 0 <= colIdx < " + nbColumns());
    Preconditions.checkNotNull(column, "column should not be null");
    Preconditions.checkArgument(nbRows() == column.length,
        "mismatch between the column length and the number of rows : expected " + nbRows());

    for (int rowIdx = 0; rowIdx < nbRows(); rowIdx++) {
      rows_.get(rowIdx).set(colIdx, column[rowIdx]);
    }
  }

  public double[][] denseArray() {
    double[][] array = new double[nbRows()][nbColumns()];
    for (int rowIdx = 0; rowIdx < nbRows(); rowIdx++) {
      array[rowIdx] = rows_.get(rowIdx).denseArray();
    }
    return array;
  }

  public void normalizeColumnUsingEuclideanNorm(int colIdx) {

    Preconditions.checkArgument(0 <= colIdx && colIdx < nbColumns(),
        "index must be such as 0 <= colIdx < " + nbColumns());

    FeatureVector column = column(colIdx);
    double normalizer = Math.sqrt(
        column.nonZeroEntries().stream().mapToDouble(idx -> column.get(idx) * column.get(idx)).sum());
    mapColumnValues(colIdx, value -> value / normalizer);
  }

  public void normalizeColumnUsingMinMax(int colIdx) {

    Preconditions.checkArgument(0 <= colIdx && colIdx < nbColumns(),
        "index must be such as 0 <= colIdx < " + nbColumns());

    FeatureVector column = column(colIdx);
    double min = column.nonZeroEntries().stream().mapToDouble(column::get).min().orElse(0.0);
    double max = column.nonZeroEntries().stream().mapToDouble(column::get).max().orElse(min);
    mapColumnValues(colIdx, value -> (value - min) / (max - min));
  }

  public void mapColumnValues(int colIdx, Function<Double, Double> function) {

    Preconditions.checkArgument(0 <= colIdx && colIdx < nbColumns(),
        "index must be such as 0 <= colIdx < " + nbColumns());
    Preconditions.checkNotNull(function, "function should not be null");

    for (int rowIdx = 0; rowIdx < nbRows(); rowIdx++) {
      rows_.get(rowIdx).set(colIdx, function.apply(rows_.get(rowIdx).get(colIdx)));
    }
  }

  private void addColumn(FeatureVector column, boolean prepend) {

    Preconditions.checkNotNull(column, "column should not be null");
    Preconditions.checkArgument(nbRows() == column.length(),
        "mismatch between the column length and the number of rows : expected " + nbRows());

    for (int rowIdx = 0; rowIdx < rows_.size(); rowIdx++) {

      FeatureVector row = rows_.get(rowIdx);

      if (!prepend) {
        row.append(column.get(rowIdx));
      } else {
        FeatureVector newRow = new FeatureVector(row.length() + 1);
        row.nonZeroEntries().forEach(colIdx -> newRow.set(colIdx + 1, row.get(colIdx)));
        newRow.set(0, column.get(rowIdx));
        rows_.set(rowIdx, newRow);
      }
    }
  }

  private void addColumn(double[] column, boolean prepend) {

    Preconditions.checkNotNull(column, "column should not be null");
    Preconditions.checkArgument(nbRows() == column.length,
        "mismatch between the column length and the number of rows : expected " + nbRows());

    for (int rowIdx = 0; rowIdx < rows_.size(); rowIdx++) {

      FeatureVector row = rows_.get(rowIdx);

      if (!prepend) {
        row.append(column[rowIdx]);
      } else {
        FeatureVector newRow = new FeatureVector(row.length() + 1);
        row.nonZeroEntries().forEach(colIdx -> newRow.set(colIdx + 1, row.get(colIdx)));
        newRow.set(0, column[rowIdx]);
        rows_.set(rowIdx, newRow);
      }
    }
  }
}
