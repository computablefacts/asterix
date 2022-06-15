package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.SpanSequence;
import com.computablefacts.asterix.View;
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
    View.of(spans).map(Span::text).forEachRemaining(term -> counts.add(term));
    double[] vector = new double[vocabulary_.size()];

    for (int i = 0; i < vector.length; i++) {
      String term = vocabulary_.term(i);
      double tfIdf = vocabulary_.tfIdf(i, counts.count(term));
      vector[i] = tfIdf;
    }
    return vector;
  }
}