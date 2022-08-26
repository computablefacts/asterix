package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.SpanSequence;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.function.Function;

@CheckReturnValue
final public class TfIdfVectorizer implements Function<SpanSequence, FeatureVector> {

  private final LoadingCache<String, String> cache_;
  private final Vocabulary vocabulary_;

  public TfIdfVectorizer(Vocabulary vocabulary) {
    this(vocabulary, 10000);
  }

  public TfIdfVectorizer(Vocabulary vocabulary, int maxCacheSize) {

    Preconditions.checkNotNull(vocabulary, "vocabulary should not be null");
    Preconditions.checkArgument(maxCacheSize > 0, "maxCacheSize must be > 0");

    vocabulary_ = vocabulary;
    cache_ = CacheBuilder.newBuilder().maximumSize(maxCacheSize)
        .build(new CacheLoader<String, String>() {
          @Override
          public String load(String term) {
            return Vocabulary.normalize(term);
          }
        });
  }

  @Override
  public FeatureVector apply(SpanSequence spans) {

    Preconditions.checkNotNull(spans, "spans should not be null");

    Multiset<String> counts = HashMultiset.create();
    spans.forEach(span -> counts.add(cache_.getUnchecked(span.text())));

    FeatureVector vector = new FeatureVector(vocabulary_.size() - 1 /* ignore UNK */);

    for (int i = 0; i < vocabulary_.size() - 1 /* ignore UNK */; i++) {
      int idx = i + 1;
      vector.set(i, vocabulary_.tfIdf(idx, counts.count(vocabulary_.term(idx))));
    }
    return vector;
  }
}
