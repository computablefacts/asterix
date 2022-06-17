package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.SpanSequence;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.function.Function;

@CheckReturnValue
final public class NGramsToTfIdfVector implements Function<SpanSequence, double[]> {

  private final Vocabulary vocabulary_;

  public NGramsToTfIdfVector(Vocabulary vocabulary) {

    Preconditions.checkNotNull(vocabulary, "vocabulary should not be null");

    vocabulary_ = vocabulary;
  }

  @Override
  public double[] apply(SpanSequence spans) {

    Preconditions.checkNotNull(spans, "spans should not be null");

    Multiset<String> counts = HashMultiset.create();
    spans.forEach(span -> counts.add(Vocabulary.normalize(span.text())));

    double[] vector = new double[vocabulary_.size() - 1 /* skip UNK */];

    for (int i = 0; i < vector.length; i++) {
      int idx = i + 1;
      vector[i] = vocabulary_.tfIdf(idx, counts.count(vocabulary_.term(idx)));
    }
    return vector;
  }
}
