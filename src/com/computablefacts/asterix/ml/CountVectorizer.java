package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.SpanSequence;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.List;
import java.util.function.Function;

@CheckReturnValue
final public class CountVectorizer implements Function<SpanSequence, FeatureVector> {

  private final LoadingCache<String, String> cache_;
  private final Vocabulary vocabulary_;
  private final boolean normalize_;
  private List<Integer> indices_;

  public CountVectorizer(Vocabulary vocabulary, boolean normalize) {
    this(vocabulary, normalize, 10000);
  }

  public CountVectorizer(Vocabulary vocabulary, boolean normalize, int maxCacheSize) {

    Preconditions.checkNotNull(vocabulary, "vocabulary should not be null");
    Preconditions.checkArgument(maxCacheSize > 0, "maxCacheSize must be > 0");

    vocabulary_ = vocabulary;
    normalize_ = normalize;
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

    FeatureVector vector;

    if (indices_ == null) {

      vector = new FeatureVector(vocabulary_.size() - 1 /* ignore UNK */);

      for (int i = 0; i < vocabulary_.size() - 1 /* ignore UNK */; i++) {
        int idx = i + 1;
        vector.set(i, counts.count(vocabulary_.term(idx)) / (double) counts.size());
      }
    } else {

      vector = new FeatureVector(indices_.size());
      @Var int i = 0;

      for (int idx : indices_) {
        vector.set(i++, counts.count(vocabulary_.term(idx)) / (double) counts.size());
      }
    }
    if (normalize_) {
      vector.normalizeUsingEuclideanNorm();
    }
    return vector;
  }

  public void subsetOfVocabularyConsidered(List<Integer> indices) {
    indices_ = indices;
  }
}
