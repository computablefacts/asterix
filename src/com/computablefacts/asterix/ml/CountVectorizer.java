package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.SpanSequence;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import java.util.function.Function;

@CheckReturnValue
final public class CountVectorizer implements Function<List<String>, FeatureVector> {

  private final Vocabulary vocabulary_;

  public CountVectorizer(Vocabulary vocabulary) {
    this(vocabulary, 10000);
  }

  public CountVectorizer(Vocabulary vocabulary, int maxCacheSize) {

    Preconditions.checkNotNull(vocabulary, "vocabulary should not be null");
    Preconditions.checkArgument(maxCacheSize > 0, "maxCacheSize must be > 0");

    vocabulary_ = vocabulary;
  }

  @Override
  public FeatureVector apply(List<String> spans) {

    Preconditions.checkNotNull(spans, "spans should not be null");

    Multiset<String> counts = HashMultiset.create(spans);
    FeatureVector vector = new FeatureVector(vocabulary_.size() - 1 /* UNK */);

    for (int i = 0; i < vocabulary_.size() - 1 /* UNK */; i++) {
      int idx = i + 1;
      vector.set(i, counts.count(vocabulary_.term(idx)) / (double) counts.size());
    }
    return vector;
  }
}
