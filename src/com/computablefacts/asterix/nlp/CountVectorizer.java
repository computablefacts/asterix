package com.computablefacts.asterix.nlp;

import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@CheckReturnValue
final public class CountVectorizer implements Function<List<String>, FeatureVector> {

  private final Vocabulary vocabulary_;
  private final Set<String> whitelist_;

  public CountVectorizer(Vocabulary vocabulary) {
    this(vocabulary, null);
  }

  public CountVectorizer(Vocabulary vocabulary, Set<String> whitelist) {
    this(vocabulary, whitelist, 10000);
  }

  public CountVectorizer(Vocabulary vocabulary, Set<String> whitelist, int maxCacheSize) {

    Preconditions.checkNotNull(vocabulary, "vocabulary should not be null");
    Preconditions.checkArgument(maxCacheSize > 0, "maxCacheSize must be > 0");

    vocabulary_ = vocabulary;
    whitelist_ = whitelist == null ? null : ImmutableSet.copyOf(whitelist);
  }

  @Override
  public FeatureVector apply(List<String> spans) {

    Preconditions.checkNotNull(spans, "spans should not be null");

    Multiset<String> counts = HashMultiset.create(spans);
    FeatureVector vector = new FeatureVector(whitelist_ == null ? vocabulary_.size() - 1 /* UNK */ : whitelist_.size());
    @Var int pos = 0;

    for (int i = 0; i < vocabulary_.size() - 1 /* UNK */; i++) {
      int idx = i + 1;
      String term = vocabulary_.term(idx);
      if (whitelist_ == null || whitelist_.contains(term)) {
        vector.set(pos++, counts.count(term) / (double) counts.size());
      }
    }
    return vector;
  }
}
