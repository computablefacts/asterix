package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Generated;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import smile.util.SparseArray;

@CheckReturnValue
final public class FeatureVector {

  private final Map<Integer, Double> nonZeroEntries_;
  private int length_;

  public FeatureVector(int length) {
    length_ = length;
    nonZeroEntries_ = new HashMap<>(length_ / 3);
  }

  public FeatureVector(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");

    length_ = vector.length();
    nonZeroEntries_ = new HashMap<>(vector.nonZeroEntries_);
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
  public String toString() {
    List<String> array = new ArrayList<>();
    for (int i = 0; i < length_; i++) {
      array.add(Double.toString(get(i)));
    }
    return "[" + Joiner.on(", ").join(array) + "]";
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
    return length_ == fv.length_ && Objects.equals(nonZeroEntries_, fv.nonZeroEntries_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(length_, nonZeroEntries_);
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
    nonZeroEntries().forEach(i -> array[i] = get(i));
    return array;
  }

  public double get(int pos) {

    Preconditions.checkArgument(0 <= pos && pos < length_, "pos must be such as 0 <= pos <= %s", length_);

    return nonZeroEntries_.getOrDefault(pos, 0.0);
  }

  public void set(int pos, double value) {

    Preconditions.checkArgument(0 <= pos && pos < length_, "pos must be such as 0 <= pos <= %s", length_);

    if (value == 0.0) {
      nonZeroEntries_.remove(pos);
    } else {
      nonZeroEntries_.put(pos, value);
    }
  }

  public void append(double value) {
    length_++;
    set(length_ - 1, value);
  }

  public void prepend(double value) {
    Map<Integer, Double> nonZeroEntries = new HashMap<>(nonZeroEntries_);
    nonZeroEntries_.clear();
    length_++;
    set(0, value);
    for (Map.Entry<Integer, Double> entry : nonZeroEntries.entrySet()) {
      set(entry.getKey() + 1, entry.getValue());
    }
  }

  public Set<Integer> nonZeroEntries() {
    return ImmutableSet.copyOf(nonZeroEntries_.keySet());
  }

  public void mapValues(Function<Double, Double> function) {

    Preconditions.checkNotNull(function, "function should not be null");

    for (int idx = 0; idx < length_; idx++) {
      set(idx, function.apply(get(idx)));
    }
  }
}
