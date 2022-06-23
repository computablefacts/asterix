package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Generated;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import smile.util.SparseArray;

@CheckReturnValue
final public class FeatureVector {

  private final Map<Integer, Double> nonZeroEntries_;
  private final int length_;

  public FeatureVector(int length) {
    length_ = length;
    nonZeroEntries_ = new HashMap<>(length_ / 3);
  }

  public FeatureVector(double[] vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");

    length_ = vector.length;
    nonZeroEntries_ = new HashMap<>(length_ / 3);

    for (int i = 0; i < vector.length; i++) {
      set(i, vector[i]);
    }
  }

  public FeatureVector(List<Double> vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");

    length_ = vector.size();
    nonZeroEntries_ = new HashMap<>(length_ / 3);

    for (int i = 0; i < vector.size(); i++) {
      set(i, vector.get(i));
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FeatureVector fv = (FeatureVector) obj;
    return Objects.equals(nonZeroEntries_, fv.nonZeroEntries_) && length_ == fv.length_;
  }

  @Override
  public int hashCode() {
    return Objects.hash(nonZeroEntries_, length_);
  }

  @Generated
  public int length() {
    return length_;
  }

  @Generated
  public boolean isEmpty() {
    return nonZeroEntries_.isEmpty();
  }

  public SparseArray sparseArray() {
    SparseArray array = new SparseArray(length_ / 3);
    nonZeroEntries_.forEach(array::set);
    return array;
  }

  public double[] denseArray() {
    double[] array = new double[length_];
    for (int i = 0; i < length_; i++) {
      array[i] = get(i);
    }
    return array;
  }

  public double get(int pos) {

    Preconditions.checkArgument(0 <= pos && pos < length_, "pos must be such as 0 <= pos <= %s",
        length_);

    return nonZeroEntries_.getOrDefault(pos, 0.0);
  }

  public void set(int pos, double value) {

    Preconditions.checkArgument(0 <= pos && pos < length_, "pos must be such as 0 <= pos <= %s",
        length_);

    if (value == 0.0) {
      nonZeroEntries_.remove(pos);
    } else {
      nonZeroEntries_.put(pos, value);
    }
  }

  public void remove(int pos) {

    Preconditions.checkArgument(0 <= pos && pos < length_, "pos must be such as 0 <= pos <= %s",
        length_);

    nonZeroEntries_.remove(pos);
  }

  public void remove(Collection<Integer> pos) {

    Preconditions.checkNotNull(pos, "pos should not be null");

    pos.forEach(this::remove);
  }

  public List<Integer> zeroes() {

    List<Integer> zeroes = new ArrayList<>();

    for (int i = 0; i < length_; i++) {
      if (!nonZeroEntries_.containsKey(i)) {
        zeroes.add(i);
      }
    }
    return zeroes;
  }

  public void normalizeUsingEuclideanNorm() {

    double normalizer = Math.sqrt(nonZeroEntries_.values().stream().mapToDouble(x -> x * x).sum());

    for (Map.Entry<Integer, Double> entry : nonZeroEntries_.entrySet()) {
      entry.setValue(entry.getValue() / normalizer);
    }
  }

  public void normalizeUsingMinMax() {

    double min = nonZeroEntries_.values().stream().mapToDouble(x -> x).min().orElse(0.0);
    double max = nonZeroEntries_.values().stream().mapToDouble(x -> x).max().orElse(min);

    for (Map.Entry<Integer, Double> entry : nonZeroEntries_.entrySet()) {
      entry.setValue((entry.getValue() - min) / (max - min));
    }
  }
}
