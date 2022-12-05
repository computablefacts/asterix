package com.computablefacts.asterix.nlp;

import com.computablefacts.asterix.View;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Beta
@CheckReturnValue
final public class TfIdfVectorizer2 implements Function<List<String>, FeatureVector> {

  private final Vocabulary vocabulary_;
  private final List<String> patterns_;

  public TfIdfVectorizer2(Vocabulary vocabulary, Set<String> ngrams) {

    Preconditions.checkNotNull(vocabulary, "vocabulary should not be null");
    Preconditions.checkNotNull(ngrams, "patterns should not be null");

    vocabulary_ = vocabulary;
    patterns_ = View.of(ngrams).toSortedList(Comparator.naturalOrder());
  }

  @Override
  public FeatureVector apply(List<String> spans) {

    Preconditions.checkNotNull(spans, "spans should not be null");

    Set<String> patterns = new HashSet<>(spans);
    Multiset<String> counts = HashMultiset.create(
        View.of(spans).map(pattern -> Splitter.on('_').splitToList(pattern)).flatten(View::of).toList());

    FeatureVector vector = new FeatureVector(patterns_.size());

    for (int i = 0; i < patterns_.size(); i++) {

      String pattern = patterns_.get(i);

      if (patterns.contains(pattern)) {

        List<String> tkns = Splitter.on('_').splitToList(pattern);
        double weight = View.of(tkns).map(tkn -> vocabulary_.tfIdf(tkn, counts.count(tkn)))
            .reduce(1.0, (c, v) -> c * v);

        vector.set(i, weight);
      }
    }
    return vector;
  }
}
